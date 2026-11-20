package com.fruvio.GestionConge.confirmation_reliquat.service;

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

    public ConfirmationReliquatService(ConfirmationReliquatRepository confirmations,
            CircuitDemandeCongeRepository circuits, EmployeRepository employes,
            Notification_congeService notifications, Solde_congeRepository soldes,
            Solde_congeService soldeService) {
        this.confirmations = confirmations;
        this.circuits = circuits;
        this.employes = employes;
        this.notifications = notifications;
        this.soldes = soldes;
        this.soldeService = soldeService;
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
        if (anneeReference >= anneeDebut)
            throw new IllegalArgumentException("La période de référence du reliquat doit précéder l'exercice du congé demandé.");

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
        int disponibleReferenceNonReserve = Math.max(0, disponibleReference - dejaReserve);
        if (jours > disponibleReferenceNonReserve)
            throw new IllegalArgumentException("Le reliquat confirmé (" + jours + " j) dépasse le reliquat disponible de l'exercice "
                    + anneeReference + " (" + disponibleReferenceNonReserve + " j après réservations).");

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

    /**
     * Confirme automatiquement le reliquat annuel à partir des soldes calculés
     * depuis la date d'embauche et de l'historique des congés validés.
     */
    @Transactional
    public ConfirmationReliquat confirmerAutomatiquement(Long dossierId, String observation, Utilisateur acteur) {
        exigerAdministration(acteur);
        CircuitDemandeConge dossier = circuits.findByIdForUpdate(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable."));
        if (dossier.getEmployeId() == null)
            throw new IllegalStateException("Ce dossier historique doit suivre son circuit habituel.");
        Employe employe = employes.findById(dossier.getEmployeId()).filter(Employe::isActif)
                .orElseThrow(() -> new IllegalStateException("Employé actif introuvable."));

        int anneeDebut = dossier.getDateDebut().getYear();
        int anneeFin = dossier.getDateFin().getYear();
        int anneeReference = anneeDebut - 1;

        soldeService.calculerSoldeAnnuelEmploye(employe.getId(), dossier.getTypeCongeId(), anneeReference, acteur);
        for (int annee = anneeDebut; annee <= anneeFin; annee++) {
            soldeService.calculerSoldeAnnuelEmploye(employe.getId(), dossier.getTypeCongeId(), annee, acteur);
        }

        Solde_conge soldeReference = soldes.findByEmployeIdAndTypeCongeIdAndPeriode(
                employe.getId(), dossier.getTypeCongeId(), anneeReference)
                .orElseThrow(() -> new IllegalStateException("Le solde de référence n'a pas pu être calculé."));
        int dejaReserve = confirmations.findByEmployeIdOrderByDateConfirmationDesc(employe.getId()).stream()
                .filter(c -> !c.getCircuitDemandeCongeId().equals(dossier.getId()))
                .filter(c -> extraireAnneeReference(c.getPeriodeReference()) == anneeReference)
                .filter(c -> circuits.findById(c.getCircuitDemandeCongeId())
                        .map(x -> List.of("RELIQUAT_RETOURNE", "CHEF_EN_ATTENTE", "DIRECTEUR_EN_ATTENTE").contains(x.getStatut()))
                        .orElse(false))
                .mapToInt(ConfirmationReliquat::getNombreJoursConfirme).sum();
        int jours = Math.max(0, disponible(soldeReference) - dejaReserve);
        String periode = "Exercice " + anneeReference;
        String reference = "AUTO-RH-" + dossierId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return confirmer(dossierId, jours, periode, reference, null, observation, acteur);
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
