package com.fruvio.GestionConge.demande_conge.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fruvio.GestionConge.demande_conge.entity.DemandeConge;
import com.fruvio.GestionConge.demande_conge.repository.DemandeCongeRepository;
import com.fruvio.GestionConge.historique_mouvement.service.Historique_mouvementService;
import com.fruvio.GestionConge.jour_ferie.service.JourFerieService;
import com.fruvio.GestionConge.notifications_conge.service.Notification_congeService;
import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@Service
public class DemandeCongeService {

    private static final Logger log = LoggerFactory.getLogger(DemandeCongeService.class);

    private final DemandeCongeRepository demandeRepo;
    private final Solde_congeRepository soldeRepo;
    private final Type_congeRepository typeCongeRepo;
    private final Historique_mouvementService historiqueService;
    private final Notification_congeService notificationService;
    private final UtilisateurRepository utilisateurRepo;
    private final JourFerieService jourFerieService;

    public DemandeCongeService(DemandeCongeRepository demandeRepo, Solde_congeRepository soldeRepo,
            Type_congeRepository typeCongeRepo, Historique_mouvementService historiqueService,
            Notification_congeService notificationService, UtilisateurRepository utilisateurRepo,
            JourFerieService jourFerieService) {
        this.demandeRepo = demandeRepo;
        this.soldeRepo = soldeRepo;
        this.typeCongeRepo = typeCongeRepo;
        this.historiqueService = historiqueService;
        this.notificationService = notificationService;
        this.utilisateurRepo = utilisateurRepo;
        this.jourFerieService = jourFerieService;
    }

    // Créer une demande avec vérification d'identité
    @Transactional
    public DemandeConge creerDemande(Long utilisateurId, DemandeConge demande, Utilisateur currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise.");
        }

        if (currentUser.getRole() != Role.ADMIN && !Objects.equals(currentUser.getId(), utilisateurId)) {
            throw new AccessDeniedException("Vous ne pouvez soumettre une demande que pour votre propre compte.");
        }

        if (demande.getDateDebut() == null || demande.getDateFin() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }

        demande.setUtilisateurId(utilisateurId);
        demande.setStatut("EN_ATTENTE");
        demande.setDateCreation(LocalDate.now());

        if (demande.getDateDebut() != null && demande.getDateFin() != null) {
            if (demande.getDateFin().isBefore(demande.getDateDebut())) {
                throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de début.");
            }

            Map<Integer, Integer> joursParAnnee = calculerJoursOuvresParAnnee(
                    demande.getDateDebut(), demande.getDateFin());
            int joursOuvres = joursParAnnee.values().stream().mapToInt(Integer::intValue).sum();
            if (joursOuvres <= 0) {
                throw new IllegalArgumentException(
                        "Cette période ne contient aucune journée ouvrée. La demande de congé doit inclure au moins un jour ouvré.");
            }
            demande.setNombreJours(joursOuvres);

            if (demande.getTypeCongeId() == null) {
                throw new IllegalArgumentException("Veuillez sélectionner un type de congé.");
            }

            Type_conge typeConge = typeCongeRepo.findById(demande.getTypeCongeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Le type de congé sélectionné n'existe pas."));
            if (Boolean.FALSE.equals(typeConge.getActif())) {
                throw new IllegalArgumentException("Ce type de congé n'est plus disponible pour les nouvelles demandes.");
            }

            Integer limiteJours = typeConge.getNombreJoursParAn();
            if (limiteJours != null && limiteJours > 0) {
                for (Map.Entry<Integer, Integer> entry : joursParAnnee.entrySet()) {
                    if (entry.getValue() > limiteJours) {
                        throw new IllegalArgumentException(
                                "Demande non autorisée pour l'année " + entry.getKey() + " : le type « "
                                        + typeConge.getLibelle() + " » autorise au maximum " + limiteJours
                                        + " jour(s) ouvré(s) par an, mais vous en demandez " + entry.getValue() + ".");
                    }
                }
            }

            for (DemandeConge existing : demandeRepo.findByUtilisateurId(utilisateurId)) {
                if (existing == null || Objects.equals(existing.getId(), demande.getId())) {
                    continue;
                }
                if (existing.getDateDebut() == null || existing.getDateFin() == null) {
                    continue;
                }
                if ("ANNULEE".equalsIgnoreCase(existing.getStatut())
                        || "REFUSEE".equalsIgnoreCase(existing.getStatut())) {
                    continue;
                }
                boolean overlaps = !(demande.getDateFin().isBefore(existing.getDateDebut())
                        || demande.getDateDebut().isAfter(existing.getDateFin()));
                if (overlaps) {
                    throw new IllegalStateException(
                            "Cette période chevauche une autre demande de congé déjà enregistrée.");
                }
            }
        } else {
            demande.setNombreJours(0);
        }

        DemandeConge saved = demandeRepo.save(demande);

        if (saved != null) {
            try {
                notificationService.creerNotification(
                        utilisateurId, "Votre demande de congé du " + demande.getDateDebut() + " au "
                                + demande.getDateFin() + " a été soumise avec succès.",
                        "INFO", "/dashboard/demande");

                Utilisateur demandeur = utilisateurRepo.findById(utilisateurId).orElse(null);
                Utilisateur manager = demandeur == null || demandeur.getManager_id() == null
                        ? null : utilisateurRepo.findById(demandeur.getManager_id()).orElse(null);

                if (manager != null && manager.isActif() && manager.getRole() == Role.MANAGER) {
                    notificationService.creerNotification(manager.getId(),
                            "Nouvelle demande de congé de " + safe(demandeur.getPrenom()) + " "
                                    + safe(demandeur.getNom()) + " du " + demande.getDateDebut()
                                    + " au " + demande.getDateFin() + ".",
                            "NOUVELLE_DEMANDE", "/dashboard/demande");
                } else {
                    for (Utilisateur admin : utilisateurRepo.findByRoleAndActifTrue(Role.ADMIN)) {
                        notificationService.creerNotification(admin.getId(),
                                "Demande de congé sans manager actif : " + safe(demandeur == null ? null : demandeur.getPrenom())
                                        + " " + safe(demandeur == null ? null : demandeur.getNom())
                                        + " (demande #" + saved.getId() + ").",
                                "NOUVELLE_DEMANDE", "/dashboard/demande");
                    }
                }
            } catch (Exception e) {
                log.warn("Demande #{} enregistrée, mais une notification a échoué.", saved.getId(), e);
            }
        }

        return saved;
    }

    // Traiter une demande avec vérification stricte du périmètre managérial
    @Transactional
    public DemandeConge traiterDemande(Long demandeId, Long requestedManagerId, String statut, String motifRefus,
            Utilisateur currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise.");
        }

        if (currentUser.getRole() == Role.EMPLOYE) {
            throw new AccessDeniedException("Les employés ne sont pas habilités à traiter les demandes de congé.");
        }

        DemandeConge demande = demandeRepo.findByIdForUpdate(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande non trouvée avec l'ID : " + demandeId));

        Long demandeurId = demande.getUtilisateurId();
        Utilisateur demandeur = utilisateurRepo.findById(demandeurId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborateur demandeur introuvable."));

        if (Objects.equals(demandeur.getId(), currentUser.getId())) {
            throw new AccessDeniedException("Vous ne pouvez pas approuver ou refuser votre propre demande.");
        }

        // Un MANAGER ne peut traiter que les demandes des membres de son équipe.
        if (currentUser.getRole() == Role.MANAGER) {
            if (!Objects.equals(demandeur.getManager_id(), currentUser.getId())) {
                throw new AccessDeniedException(
                        "Accès refusé : vous n'êtes pas le manager responsable de ce collaborateur.");
            }
        }

        String statutNormalise = statut == null ? "" : statut.trim().toUpperCase();
        if (!Set.of("VALIDEE", "REFUSEE").contains(statutNormalise)) {
            throw new IllegalArgumentException("Le statut de traitement doit être VALIDEE ou REFUSEE.");
        }

        String motifRefusNormalise = motifRefus == null ? "" : motifRefus.trim();
        if ("REFUSEE".equals(statutNormalise) && motifRefusNormalise.isEmpty()) {
            throw new IllegalArgumentException(
                    "Le motif de refus est obligatoire. Veuillez justifier votre décision avant de continuer.");
        }

        if (!"EN_ATTENTE".equalsIgnoreCase(demande.getStatut())) {
            throw new IllegalStateException("Seule une demande en attente peut être traitée.");
        }
        if (demande.getDateDebut() == null || demande.getDateFin() == null
                || demande.getTypeCongeId() == null || demande.getUtilisateurId() == null) {
            throw new IllegalStateException("Cette demande est incomplète et ne peut pas être traitée.");
        }
        if (demande.getDateFin().isBefore(demande.getDateDebut())) {
            throw new IllegalStateException("La date de fin ne peut pas être antérieure à la date de début.");
        }

        Long actualValidatorId = currentUser.getId();
        demande.setValideePar(actualValidatorId);
        demande.setStatut(statutNormalise);
        demande.setDateValidation(LocalDate.now());

        if ("REFUSEE".equalsIgnoreCase(statutNormalise)) {
            demande.setMotifRefus(motifRefusNormalise);
        }

        boolean estValidee = "VALIDEE".equalsIgnoreCase(statutNormalise) || "VALIDE".equalsIgnoreCase(statutNormalise)
                || "APPROVED".equalsIgnoreCase(statutNormalise);

        if (estValidee && demande.getDateDebut() != null && demande.getDateFin() != null) {
            Map<Integer, Integer> joursParAnnee = calculerJoursOuvresParAnnee(
                    demande.getDateDebut(), demande.getDateFin());
            int jours = joursParAnnee.values().stream().mapToInt(Integer::intValue).sum();
            if (jours <= 0) {
                throw new IllegalStateException(
                        "La période demandée ne contient aucune journée ouvrée. Aucune validation ne peut être effectuée.");
            }
            demande.setNombreJours(jours);

            if (demande.getTypeCongeId() != null && demande.getUtilisateurId() != null) {
                Map<Integer, Solde_conge> soldesParAnnee = new LinkedHashMap<>();

                // Vérifier tous les exercices avant de débiter le moindre solde.
                for (Map.Entry<Integer, Integer> entry : joursParAnnee.entrySet()) {
                    Solde_conge solde = soldeRepo.findByUtilisateurIdAndTypeCongeIdAndPeriode(
                            demande.getUtilisateurId(), demande.getTypeCongeId(), entry.getKey())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Aucun solde trouvé pour le collaborateur #" + demande.getUtilisateurId()
                                            + ", le type #" + demande.getTypeCongeId()
                                            + " pour l'année " + entry.getKey()));

                    int acquis = solde.getSoldeAquis() != null ? solde.getSoldeAquis() : 0;
                    int pris = solde.getSoldePris() != null ? solde.getSoldePris() : 0;
                    int disponibles = Math.max(0, acquis - pris);
                    if (disponibles < entry.getValue()) {
                        throw new IllegalStateException("Solde insuffisant pour l'année " + entry.getKey()
                                + " : " + entry.getValue() + " jour(s) demandés pour "
                                + disponibles + " jour(s) disponibles.");
                    }
                    soldesParAnnee.put(entry.getKey(), solde);
                }

                for (Map.Entry<Integer, Integer> entry : joursParAnnee.entrySet()) {
                    Solde_conge solde = soldesParAnnee.get(entry.getKey());
                    int pris = solde.getSoldePris() != null ? solde.getSoldePris() : 0;
                    int acquis = solde.getSoldeAquis() != null ? solde.getSoldeAquis() : 0;
                    int nouveauPris = pris + entry.getValue();

                    solde.setSoldePris(nouveauPris);
                    solde.setSoldeRestant(Math.max(0, acquis - nouveauPris));
                    solde.setDate_maj(new Timestamp(System.currentTimeMillis()));
                    soldeRepo.save(solde);

                    historiqueService.enregistrerMouvement(
                            demande.getUtilisateurId(), "DEBIT_CONGE", entry.getValue(),
                            demande.getTypeCongeId(), demande.getId(),
                            "Congé validé par le responsable #" + actualValidatorId
                                    + " (exercice " + entry.getKey() + ")", actualValidatorId);
                }
            }

            try {
                notificationService.creerNotification(
                        demande.getUtilisateurId(), "Votre demande de congé du " + demande.getDateDebut()
                                + " au " + demande.getDateFin() + " a été VALIDÉE.",
                        "VALIDATION", "/dashboard/demande");
            } catch (Exception e) {
                log.warn("Échec d'envoi notification validation : {}", e.getMessage());
            }

        } else if ("REFUSEE".equalsIgnoreCase(statutNormalise)) {
            try {
                notificationService.creerNotification(demande.getUtilisateurId(),
                        "Votre demande de congé du " + demande.getDateDebut() + " a été REFUSÉE"
                                + (motifRefusNormalise.isEmpty() ? "." : " (Motif : " + motifRefusNormalise + ")"),
                        "REFUS", "/dashboard/demande");
            } catch (Exception e) {
                log.warn("Échec d'envoi notification refus : {}", e.getMessage());
            }
        }

        return demandeRepo.save(demande);
    }

    // Annuler une demande
    @Transactional
    public DemandeConge annulerDemande(Long demandeId, Long requestedUserId, Utilisateur currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise.");
        }

        DemandeConge demande = demandeRepo.findByIdForUpdate(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande non trouvée avec l'ID : " + demandeId));

        if (currentUser.getRole() != Role.ADMIN && !Objects.equals(currentUser.getId(), demande.getUtilisateurId())) {
            throw new AccessDeniedException("Vous ne pouvez annuler que vos propres demandes.");
        }

        if (!"EN_ATTENTE".equalsIgnoreCase(demande.getStatut()) && !"VALIDEE".equalsIgnoreCase(demande.getStatut())) {
            throw new IllegalStateException("Seule une demande en attente ou validée peut être annulée.");
        }

        if ("VALIDEE".equalsIgnoreCase(demande.getStatut())) {
            if (demande.getDateDebut() == null || demande.getDateFin() == null
                    || demande.getTypeCongeId() == null || demande.getUtilisateurId() == null) {
                throw new IllegalStateException("Annulation impossible : les informations de la demande sont incomplètes.");
            }

            Map<Integer, Integer> joursParAnnee = calculerJoursOuvresParAnnee(
                    demande.getDateDebut(), demande.getDateFin());
            Map<Integer, Solde_conge> soldesParAnnee = new LinkedHashMap<>();

            // Vérifier la présence de tous les soldes avant de réintégrer les jours.
            for (Map.Entry<Integer, Integer> entry : joursParAnnee.entrySet()) {
                Solde_conge solde = soldeRepo.findByUtilisateurIdAndTypeCongeIdAndPeriode(
                        demande.getUtilisateurId(), demande.getTypeCongeId(), entry.getKey())
                        .orElseThrow(() -> new IllegalStateException(
                                "Annulation impossible : solde de congé introuvable pour l'année " + entry.getKey() + "."));
                soldesParAnnee.put(entry.getKey(), solde);
            }

            for (Map.Entry<Integer, Integer> entry : joursParAnnee.entrySet()) {
                Solde_conge solde = soldesParAnnee.get(entry.getKey());
                int prisActuel = solde.getSoldePris() != null ? solde.getSoldePris() : 0;
                int acquis = solde.getSoldeAquis() != null ? solde.getSoldeAquis() : 0;
                int joursRestitues = Math.min(prisActuel, entry.getValue());
                int nouveauPris = prisActuel - joursRestitues;

                solde.setSoldePris(nouveauPris);
                solde.setSoldeRestant(Math.max(0, acquis - nouveauPris));
                solde.setDate_maj(new Timestamp(System.currentTimeMillis()));
                soldeRepo.save(solde);

                if (joursRestitues > 0) {
                    historiqueService.enregistrerMouvement(
                            demande.getUtilisateurId(), "CREDIT_CONGE", joursRestitues,
                            demande.getTypeCongeId(), demande.getId(),
                            "Restitution des jours suite à l'annulation du congé (exercice " + entry.getKey() + ")",
                            currentUser.getId());
                }
            }
        }

        demande.setStatut("ANNULEE");
        DemandeConge saved = demandeRepo.save(demande);
        try {
            notificationService.creerNotification(demande.getUtilisateurId(),
                    "Votre demande de congé du " + demande.getDateDebut() + " au " + demande.getDateFin()
                            + " a été annulée.",
                    "ANNULATION", "/dashboard/demande");
            Utilisateur demandeur = utilisateurRepo.findById(demande.getUtilisateurId()).orElse(null);
            if (demandeur != null && demandeur.getManager_id() != null) {
                notificationService.creerNotification(demandeur.getManager_id(),
                        "La demande de congé #" + demande.getId() + " de " + safe(demandeur.getPrenom())
                                + " " + safe(demandeur.getNom()) + " a été annulée.",
                        "ANNULATION", "/dashboard/demande");
            }
        } catch (Exception e) {
            log.warn("Demande #{} annulée, mais une notification a échoué.", demandeId, e);
        }
        return saved;
    }

    /**
     * Vérifie l'éligibilité d'un dossier préparé par l'assistant en réutilisant
     * les mêmes règles de jours ouvrés, plafonds annuels, chevauchements et soldes
     * que le circuit historique. Aucun solde n'est débité à cette étape.
     */
    @Transactional(readOnly = true)
    public int verifierEligibiliteWorkflow(Long utilisateurId, Long typeCongeId,
            LocalDate dateDebut, LocalDate dateFin) {
        if (utilisateurId == null || typeCongeId == null || dateDebut == null || dateFin == null
                || dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("Collaborateur, type et période valides obligatoires.");
        }
        Utilisateur utilisateur = utilisateurRepo.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborateur introuvable."));
        if (!utilisateur.isEnabled()) throw new IllegalStateException("Le collaborateur n'est pas actif.");

        Type_conge type = typeCongeRepo.findById(typeCongeId)
                .orElseThrow(() -> new IllegalArgumentException("Type de congé introuvable."));
        if (Boolean.FALSE.equals(type.getActif())) throw new IllegalStateException("Ce type de congé est désactivé.");

        Map<Integer, Integer> joursParAnnee = calculerJoursOuvresParAnnee(dateDebut, dateFin);
        int total = joursParAnnee.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) throw new IllegalArgumentException("La période ne contient aucun jour ouvré.");

        Integer plafond = type.getNombreJoursParAn();
        for (Map.Entry<Integer, Integer> e : joursParAnnee.entrySet()) {
            if (plafond != null && plafond > 0 && e.getValue() > plafond) {
                throw new IllegalStateException("Le plafond annuel de " + plafond + " jour(s) est dépassé pour " + e.getKey() + ".");
            }
            Solde_conge solde = soldeRepo.findByUtilisateurIdAndTypeCongeIdAndPeriode(
                    utilisateurId, typeCongeId, e.getKey())
                    .orElseThrow(() -> new IllegalStateException("Aucun solde enregistré pour l'année " + e.getKey() + "."));
            int acquis = solde.getSoldeAquis() == null ? 0 : solde.getSoldeAquis();
            int pris = solde.getSoldePris() == null ? 0 : solde.getSoldePris();
            if (acquis - pris < e.getValue()) {
                throw new IllegalStateException("Solde insuffisant pour " + e.getKey() + " : "
                        + e.getValue() + " jour(s) demandés, " + Math.max(0, acquis - pris) + " disponible(s).");
            }
        }

        for (DemandeConge existing : demandeRepo.findByUtilisateurId(utilisateurId)) {
            if (existing.getDateDebut() == null || existing.getDateFin() == null
                    || "ANNULEE".equalsIgnoreCase(existing.getStatut())
                    || "REFUSEE".equalsIgnoreCase(existing.getStatut())) continue;
            if (!(dateFin.isBefore(existing.getDateDebut()) || dateDebut.isAfter(existing.getDateFin()))) {
                throw new IllegalStateException("La période chevauche une autre demande déjà enregistrée.");
            }
        }
        return total;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private int calculerJoursOuvres(LocalDate dateDebut, LocalDate dateFin) {
        return jourFerieService.compterJoursOuvres(dateDebut, dateFin);
    }

    private Map<Integer, Integer> calculerJoursOuvresParAnnee(LocalDate dateDebut, LocalDate dateFin) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int annee = dateDebut.getYear(); annee <= dateFin.getYear(); annee++) {
            LocalDate debutAnnee = LocalDate.of(annee, 1, 1);
            LocalDate finAnnee = LocalDate.of(annee, 12, 31);
            LocalDate debutPeriode = dateDebut.isAfter(debutAnnee) ? dateDebut : debutAnnee;
            LocalDate finPeriode = dateFin.isBefore(finAnnee) ? dateFin : finAnnee;
            int jours = calculerJoursOuvres(debutPeriode, finPeriode);
            if (jours > 0) {
                result.put(annee, jours);
            }
        }
        return result;
    }

    // Récupérer les demandes d'un utilisateur avec contrôle d'accès
    public List<DemandeConge> getDemandesParUtilisateur(Long utilisateurId, Utilisateur currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise.");
        }

        if (currentUser.getRole() == Role.ADMIN) {
            return demandeRepo.findByUtilisateurId(utilisateurId);
        }

        if (currentUser.getRole() == Role.EMPLOYE) {
            if (!Objects.equals(currentUser.getId(), utilisateurId)) {
                throw new AccessDeniedException(
                        "Accès non autorisé : vous ne pouvez consulter que vos propres demandes.");
            }
            return demandeRepo.findByUtilisateurId(utilisateurId);
        }

        if (currentUser.getRole() == Role.MANAGER) {
            if (Objects.equals(currentUser.getId(), utilisateurId)) {
                return demandeRepo.findByUtilisateurId(utilisateurId);
            }
            Utilisateur target = utilisateurRepo.findById(utilisateurId).orElse(null);
            if (target == null || !Objects.equals(target.getManager_id(), currentUser.getId())) {
                throw new AccessDeniedException("Accès non autorisé : cet employé n'appartient pas à votre équipe.");
            }
            return demandeRepo.findByUtilisateurId(utilisateurId);
        }

        return Collections.emptyList();
    }

    // Récupérer les demandes en attente pour un manager
    public List<DemandeConge> getDemandesEnAttentePourManager(Long managerId, Utilisateur currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise.");
        }

        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Accès réservé aux managers et administrateurs.");
        }

        if (currentUser.getRole() == Role.MANAGER && !Objects.equals(currentUser.getId(), managerId)) {
            throw new AccessDeniedException("Vous ne pouvez consulter que les demandes de votre équipe.");
        }

        List<Utilisateur> team = utilisateurRepo.findByManagerId(managerId);
        if (team.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> memberIds = team.stream().map(Utilisateur::getId).toList();
        return demandeRepo.findByUtilisateurIdInAndStatut(memberIds, "EN_ATTENTE");
    }

    // Récupérer toutes les demandes en attente (adapté au rôle)
    public List<DemandeConge> getToutesLesDemandesEnAttente(Utilisateur currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise.");
        }

        if (currentUser.getRole() == Role.ADMIN) {
            return demandeRepo.findByStatut("EN_ATTENTE");
        }

        if (currentUser.getRole() == Role.MANAGER) {
            List<Utilisateur> team = utilisateurRepo.findByManagerId(currentUser.getId());
            if (team.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> memberIds = team.stream().map(Utilisateur::getId).toList();
            return demandeRepo.findByUtilisateurIdInAndStatut(memberIds, "EN_ATTENTE");
        }

        throw new AccessDeniedException("Accès réservé aux managers et administrateurs.");
    }

    // Récupérer toutes les demandes (adapté au rôle)
    public List<DemandeConge> getToutesLesDemandes(Utilisateur currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise.");
        }

        if (currentUser.getRole() == Role.ADMIN) {
            return demandeRepo.findAll();
        }

        if (currentUser.getRole() == Role.MANAGER) {
            List<Utilisateur> team = utilisateurRepo.findByManagerId(currentUser.getId());
            List<Long> ids = new ArrayList<>(team.stream().map(Utilisateur::getId).toList());
            if (!ids.contains(currentUser.getId())) {
                ids.add(currentUser.getId());
            }
            return demandeRepo.findByUtilisateurIdIn(ids);
        }

        throw new AccessDeniedException("Accès réservé aux managers et administrateurs.");
    }
}