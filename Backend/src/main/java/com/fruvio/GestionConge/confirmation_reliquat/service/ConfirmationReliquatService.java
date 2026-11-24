package com.fruvio.GestionConge.confirmation_reliquat.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fruvio.GestionConge.circuit_conge.entity.CircuitDemandeConge;
import com.fruvio.GestionConge.circuit_conge.repository.CircuitDemandeCongeRepository;
import com.fruvio.GestionConge.confirmation_reliquat.entity.ConfirmationReliquat;
import com.fruvio.GestionConge.confirmation_reliquat.repository.ConfirmationReliquatRepository;
import com.fruvio.GestionConge.employe.entity.Employe;
import com.fruvio.GestionConge.employe.repository.EmployeRepository;
import com.fruvio.GestionConge.notifications_conge.service.Notification_congeService;
import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.solde_conge.service.Solde_congeService;
import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

@Service
public class ConfirmationReliquatService {
    private final ConfirmationReliquatRepository confirmations;
    private final CircuitDemandeCongeRepository circuits;
    private final EmployeRepository employes;
    private final Notification_congeService notifications;
    private final Solde_congeRepository soldes;
    private final Solde_congeService soldeService;
    private final Type_congeRepository typesConge;

    @org.springframework.beans.factory.annotation.Autowired
    public ConfirmationReliquatService(ConfirmationReliquatRepository confirmations,
            CircuitDemandeCongeRepository circuits, EmployeRepository employes,
            Notification_congeService notifications, Solde_congeRepository soldes,
            Solde_congeService soldeService, Type_congeRepository typesConge) {
        this.confirmations = confirmations;
        this.circuits = circuits;
        this.employes = employes;
        this.notifications = notifications;
        this.soldes = soldes;
        this.soldeService = soldeService;
        this.typesConge = typesConge;
    }

    public ConfirmationReliquatService(ConfirmationReliquatRepository confirmations,
            CircuitDemandeCongeRepository circuits, EmployeRepository employes,
            Notification_congeService notifications, Solde_congeRepository soldes,
            Solde_congeService soldeService) {
        this(confirmations, circuits, employes, notifications, soldes, soldeService, null);
    }

    public ConfirmationReliquatService(ConfirmationReliquatRepository confirmations,
            CircuitDemandeCongeRepository circuits, EmployeRepository employes,
            Notification_congeService notifications, Solde_congeRepository soldes) {
        this(confirmations, circuits, employes, notifications, soldes, null, null);
    }

    @Transactional
    public ConfirmationReliquat confirmer(Long dossierId, Integer jours, String periode,
            String reference, String justificatif, String observation, Utilisateur acteur) {
        exigerAdministration(acteur);
        if (jours == null || jours < 0) throw new IllegalArgumentException("Le nombre de jours confirmé doit être positif ou nul.");
        if (periode == null || periode.isBlank()) throw new IllegalArgumentException("La période de référence est obligatoire.");
        if (reference == null || reference.isBlank()) throw new IllegalArgumentException("La référence de confirmation est obligatoire.");

        CircuitDemandeConge dossier = circuits.findByIdForUpdate(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable."));
        if (!"RELIQUAT_EN_ATTENTE".equals(dossier.getStatut()))
            throw new IllegalStateException("Ce dossier n'attend pas une confirmation de reliquat.");
        if (dossier.getEmployeId() == null)
            throw new IllegalStateException("Ce dossier historique est lié à un compte utilisateur ; utilisez son circuit historique.");
        Employe employe = employes.findById(dossier.getEmployeId()).filter(Employe::isActif)
                .orElseThrow(() -> new IllegalStateException("Employé actif introuvable."));
        if (confirmations.existsByReferenceConfirmation(reference.trim()))
            throw new IllegalArgumentException("Cette référence de confirmation existe déjà.");

        int anneeDebut = dossier.getDateDebut().getYear();
        int anneeFin = dossier.getDateFin().getYear();
        int anneeReference = extraireAnneeReference(periode);
        if (estCongeAnnuel(dossier.getTypeCongeId()) && anneeReference >= anneeDebut)
            throw new IllegalArgumentException("La période de référence du reliquat annuel doit précéder l'exercice du congé demandé.");

        if (estCongeAnnuel(dossier.getTypeCongeId())) {
            boolean referenceEligible = employe.getDateEmbauche() != null
                    && !employe.getDateEmbauche().isAfter(LocalDate.of(anneeReference, 12, 31));
            int disponibleReferenceNonReserve = 0;
            if (referenceEligible) {
                Solde_conge soldeReference = soldes.findByEmployeIdAndTypeCongeIdAndPeriode(
                        employe.getId(), dossier.getTypeCongeId(), anneeReference)
                        .orElseThrow(() -> new IllegalStateException("Aucun solde RH enregistré pour l'exercice de référence " + anneeReference + "."));
                int disponibleReference = disponible(soldeReference);
                int dejaReserve = confirmations.findByEmployeIdOrderByDateConfirmationDesc(employe.getId()).stream()
                        .filter(c -> !c.getCircuitDemandeCongeId().equals(dossier.getId()))
                        .filter(c -> extraireAnneeReference(c.getPeriodeReference()) == anneeReference)
                        .filter(c -> circuits.findById(c.getCircuitDemandeCongeId())
                                .map(x -> List.of("RELIQUAT_RETOURNE", "CHEF_EN_ATTENTE", "DIRECTEUR_EN_ATTENTE").contains(x.getStatut()))
                                .orElse(false))
                        .mapToInt(ConfirmationReliquat::getNombreJoursConfirme).sum();
                disponibleReferenceNonReserve = Math.max(0, disponibleReference - dejaReserve);
            }
            if (!referenceEligible && jours > 0)
                throw new IllegalArgumentException("Aucun reliquat antérieur ne peut être attribué : l'employé a été recruté après l'exercice " + anneeReference + ".");
            if (jours > disponibleReferenceNonReserve)
                throw new IllegalArgumentException("Le reliquat confirmé (" + jours + " j) dépasse le reliquat disponible de l'exercice "
                        + anneeReference + " (" + disponibleReferenceNonReserve + " j après réservations).");
            if (jours != disponibleReferenceNonReserve && (observation == null || observation.isBlank()))
                throw new IllegalArgumentException("Une observation est obligatoire lorsque le reliquat confirmé diffère du calcul automatique.");

            List<Solde_conge> soldesConges = soldes.findByEmployeIdAndTypeCongeId(
                    employe.getId(), dossier.getTypeCongeId()).stream()
                    .filter(s -> s.getPeriode() >= anneeDebut && s.getPeriode() <= anneeFin)
                    .toList();
            if (soldesConges.size() != anneeFin - anneeDebut + 1)
                throw new IllegalStateException("Les soldes RH de tous les exercices du congé doivent être enregistrés.");
            int besoinCourant = Math.max(0, dossier.getNombreJours() - jours);
            int disponibleCourant = soldesConges.stream().mapToInt(this::disponible).sum();
            if (besoinCourant > disponibleCourant)
                throw new IllegalArgumentException("Couverture insuffisante : " + jours + " j de reliquat + "
                        + disponibleCourant + " j disponibles sur les exercices du congé, pour " + dossier.getNombreJours() + " j demandés.");
        } else {
            CalculReliquat calcul = calculerReliquat(dossierId, acteur);
            if (jours != calcul.reliquatDisponible())
                throw new IllegalArgumentException("Le nombre confirmé doit correspondre au solde calculé en base : "
                        + calcul.reliquatDisponible() + " jour(s).");
        }

        ConfirmationReliquat confirmation = confirmations.save(ConfirmationReliquat.builder()
                .circuitDemandeCongeId(dossier.getId())
                .employeId(employe.getId())
                .nombreJoursConfirme(jours)
                .periodeReference(periode.trim())
                .confirmePar(acteur.getId())
                .dateConfirmation(LocalDateTime.now())
                .referenceConfirmation(reference.trim())
                .justificatif(justificatif == null || justificatif.isBlank() ? null : justificatif.trim())
                .observation(observation == null || observation.isBlank() ? null : observation.trim())
                .build());

        dossier.setReliquatJours(jours);
        dossier.setReliquatCommentaire(observation);
        dossier.setAdministrationPersonnelId(acteur.getId());
        dossier.setDateReliquat(LocalDateTime.now());
        dossier.setStatut("RELIQUAT_RETOURNE");
        circuits.save(dossier);

        notifications.creerNotification(dossier.getAssistantId(),
                "Le reliquat du dossier #" + dossier.getId() + " a été confirmé (" + jours + " jour(s)).",
                "CIRCUIT_CONGE", "/dashboard/circuit-conge");
        return confirmation;
    }

    public record CalculReliquat(Integer droitsAcquis, Integer joursConsommes, Integer reliquatBrut,
            Integer joursDejaReserves, Integer reliquatDisponible, String periodeReference) {}

    /** Calcule le reliquat annuel disponible, après déduction des reliquats déjà réservés. */
    @Transactional
    public CalculReliquat calculerReliquat(Long dossierId, Utilisateur acteur) {
        exigerAdministration(acteur);
        CircuitDemandeConge dossier = circuits.findByIdForUpdate(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable."));
        if (dossier.getEmployeId() == null)
            throw new IllegalStateException("Ce dossier historique doit suivre son circuit habituel.");
        if (!"RELIQUAT_EN_ATTENTE".equals(dossier.getStatut()))
            throw new IllegalStateException("Ce dossier n'attend pas une confirmation de reliquat.");
        Employe employe = employes.findById(dossier.getEmployeId()).filter(Employe::isActif)
                .orElseThrow(() -> new IllegalStateException("Employé actif introuvable."));
        if (!estCongeAnnuel(dossier.getTypeCongeId()))
            return calculerReliquatDepuisSoldesEnregistres(dossier, employe);
        if (soldeService == null)
            throw new IllegalStateException("Le calcul automatique des soldes n'est pas configuré.");

        int anneeDebut = dossier.getDateDebut().getYear();
        int anneeFin = dossier.getDateFin().getYear();
        int anneeReference = anneeDebut - 1;
        boolean referenceEligible = employe.getDateEmbauche() != null
                && !employe.getDateEmbauche().isAfter(LocalDate.of(anneeReference, 12, 31));
        if (referenceEligible) {
            soldeService.calculerSoldeAnnuelEmploye(employe.getId(), dossier.getTypeCongeId(), anneeReference, acteur);
        }
        for (int annee = anneeDebut; annee <= anneeFin; annee++) {
            soldeService.calculerSoldeAnnuelEmploye(employe.getId(), dossier.getTypeCongeId(), annee, acteur);
        }

        int reliquatBrut = 0;
        int dejaReserve = 0;
        int droitsAcquis = 0;
        int joursConsommes = 0;
        if (referenceEligible) {
            Solde_conge soldeReference = soldes.findByEmployeIdAndTypeCongeIdAndPeriode(
                    employe.getId(), dossier.getTypeCongeId(), anneeReference)
                    .orElseThrow(() -> new IllegalStateException("Le solde de référence n'a pas pu être calculé."));
            reliquatBrut = disponible(soldeReference);
            droitsAcquis = soldeReference.getSoldeAquisEnDemiJournees() / 2;
            joursConsommes = soldeReference.getSoldePrisEnDemiJournees() / 2;
            dejaReserve = confirmations.findByEmployeIdOrderByDateConfirmationDesc(employe.getId()).stream()
                    .filter(c -> !c.getCircuitDemandeCongeId().equals(dossier.getId()))
                    .filter(c -> extraireAnneeReference(c.getPeriodeReference()) == anneeReference)
                    .filter(c -> circuits.findById(c.getCircuitDemandeCongeId())
                            .map(x -> List.of("RELIQUAT_RETOURNE", "CHEF_EN_ATTENTE", "DIRECTEUR_EN_ATTENTE").contains(x.getStatut()))
                            .orElse(false))
                    .mapToInt(ConfirmationReliquat::getNombreJoursConfirme).sum();
        }
        int reliquatDisponible = Math.max(0, reliquatBrut - dejaReserve);
        String periodeReference = referenceEligible
                ? "Exercice " + anneeReference
                : "Exercice " + anneeReference + " — embauche postérieure, aucun reliquat antérieur";
        return new CalculReliquat(droitsAcquis, joursConsommes, reliquatBrut, dejaReserve,
                reliquatDisponible, periodeReference);
    }


    /**
     * Congés non annuels : lecture exclusive des soldes RH persistés.
     * Aucune durée légale n'est déduite d'un libellé ni inventée.
     */
    private CalculReliquat calculerReliquatDepuisSoldesEnregistres(
            CircuitDemandeConge dossier, Employe employe) {
        int debut = dossier.getDateDebut().getYear();
        int fin = dossier.getDateFin().getYear();
        Type_conge type = typesConge.findById(dossier.getTypeCongeId())
                .orElseThrow(() -> new IllegalStateException("Type de congé introuvable."));
        String code = type.getCode() == null ? "" : type.getCode().trim().toUpperCase();
        boolean paternite = "NAISS_PAT".equals(code);
        boolean permissionFamiliale = List.of("MAR_SAL", "MAR_ENF", "DEC_CONJ", "CIRC_ENF", "URGENCE", "AUT_FAM").contains(code);
        if (paternite || permissionFamiliale) {
            if (soldeService == null)
                throw new IllegalStateException("Le calcul légal des permissions n'est pas configuré.");
            if (debut != fin)
                throw new IllegalStateException("La confirmation d'une permission familiale doit rester dans une seule année civile.");
            int plafond = paternite ? 3 : 10;
            int restantApresDemandesValidees = soldeService.calculerPermissionFamilialeDisponible(
                    employe.getId(), dossier.getTypeCongeId(), debut);
            int dejaReserve = confirmations.findByEmployeIdOrderByDateConfirmationDesc(employe.getId()).stream()
                    .filter(c -> !c.getCircuitDemandeCongeId().equals(dossier.getId()))
                    .filter(c -> extraireAnneeReference(c.getPeriodeReference()) == debut)
                    .filter(c -> circuits.findById(c.getCircuitDemandeCongeId())
                            .map(x -> List.of("RELIQUAT_RETOURNE", "CHEF_EN_ATTENTE", "DIRECTEUR_EN_ATTENTE").contains(x.getStatut()))
                            .orElse(false))
                    .mapToInt(ConfirmationReliquat::getNombreJoursConfirme).sum();
            int disponible = Math.max(0, restantApresDemandesValidees - dejaReserve);
            int consommes = plafond - restantApresDemandesValidees;
            return new CalculReliquat(plafond, consommes, restantApresDemandesValidees,
                    dejaReserve, disponible, "Année civile " + debut);
        }
        int acquis = 0;
        int consommes = 0;
        int restant = 0;
        for (int annee = debut; annee <= fin; annee++) {
            Solde_conge soldeEnregistre = soldes.findByEmployeIdAndTypeCongeIdAndPeriode(
                    employe.getId(), dossier.getTypeCongeId(), annee).orElse(null);
            if (soldeEnregistre != null) {
                acquis += soldeEnregistre.getSoldeAquisEnDemiJournees() / 2;
                consommes += soldeEnregistre.getSoldePrisEnDemiJournees() / 2;
                restant += soldeEnregistre.getSoldeRestantEnDemiJournees() / 2;
                continue;
            }
            if (soldeService == null)
                throw new IllegalStateException("Le calcul des droits configurés n'est pas disponible pour l'exercice " + annee + ".");
            Solde_congeService.DroitsTypeConfigure droits = soldeService.calculerDroitsAnnuelsConfigures(
                    employe.getId(), dossier.getTypeCongeId(), annee);
            acquis += droits.plafond();
            consommes += droits.consommes();
            restant += droits.disponible();
        }
        int reserves = confirmations.findByEmployeIdOrderByDateConfirmationDesc(employe.getId()).stream()
                .filter(c -> !c.getCircuitDemandeCongeId().equals(dossier.getId()))
                .filter(c -> {
                    try {
                        int annee = extraireAnneeReference(c.getPeriodeReference());
                        return annee >= debut && annee <= fin;
                    } catch (RuntimeException ignored) {
                        return false;
                    }
                })
                .filter(c -> circuits.findById(c.getCircuitDemandeCongeId())
                        .map(x -> List.of("RELIQUAT_RETOURNE", "CHEF_EN_ATTENTE", "DIRECTEUR_EN_ATTENTE").contains(x.getStatut()))
                        .orElse(false))
                .mapToInt(ConfirmationReliquat::getNombreJoursConfirme).sum();
        String periode = debut == fin ? "Exercice " + debut : "Exercices " + debut + "-" + fin;
        return new CalculReliquat(acquis, consommes, restant, reserves,
                Math.max(0, restant - reserves), periode);
    }

    /**
     * Confirme automatiquement le reliquat annuel à partir des soldes calculés
     * depuis la date d'embauche et de l'historique des congés validés.
     */
    @Transactional
    public ConfirmationReliquat confirmerAutomatiquement(Long dossierId, String observation, Utilisateur acteur) {
        CalculReliquat calcul = calculerReliquat(dossierId, acteur);
        String reference = "AUTO-RH-" + dossierId;
        return confirmer(dossierId, calcul.reliquatDisponible(), calcul.periodeReference(), reference,
                null, observation, acteur);
    }

    private boolean estCongeAnnuel(Long typeCongeId) {
        if (typesConge == null || typeCongeId == null)
            throw new IllegalStateException("Le référentiel des types de congé n'est pas configuré.");
        Type_conge type = typesConge.findById(typeCongeId)
                .orElseThrow(() -> new IllegalStateException("Type de congé introuvable."));
        String code = type.getCode() == null ? "" : type.getCode().toUpperCase();
        String libelle = type.getLibelle() == null ? "" : type.getLibelle().toUpperCase();
        return code.contains("ANNUEL") || libelle.contains("ANNUEL");
    }

    private int disponible(Solde_conge solde) {
        return Math.max(0, solde.getSoldeAquisEnDemiJournees()
                - solde.getSoldePrisEnDemiJournees()) / 2;
    }

    private int extraireAnneeReference(String periode) {
        Matcher matcher = Pattern.compile("(?:19|20)\\d{2}").matcher(periode == null ? "" : periode);
        if (!matcher.find())
            throw new IllegalArgumentException("La période de référence doit contenir une année explicite (ex. 2025).");
        return Integer.parseInt(matcher.group());
    }

    @Transactional(readOnly = true)
    public List<ConfirmationReliquat> historiqueDossier(Long dossierId, Utilisateur acteur) {
        exigerAdministration(acteur);
        return confirmations.findByCircuitDemandeCongeIdOrderByDateConfirmationDesc(dossierId);
    }

    private void exigerAdministration(Utilisateur acteur) {
        if (acteur == null || (acteur.getRole() != Role.ADMIN
                && acteur.getRole() != Role.ADMINISTRATION_PERSONNEL))
            throw new AccessDeniedException("Action réservée à l'administration du personnel.");
    }
}
