package com.fruvio.GestionConge.demande_conge.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@Service
public class DemandeCongeService {

    private static final Logger log = LoggerFactory.getLogger(DemandeCongeService.class);

    private final DemandeCongeRepository demandeRepo;
    private final Solde_congeRepository soldeRepo;
    private final Historique_mouvementService historiqueService;
    private final Notification_congeService notificationService;
    private final UtilisateurRepository utilisateurRepo;
    private final JourFerieService jourFerieService;

    public DemandeCongeService(DemandeCongeRepository demandeRepo, Solde_congeRepository soldeRepo,
            Historique_mouvementService historiqueService, Notification_congeService notificationService,
            UtilisateurRepository utilisateurRepo, JourFerieService jourFerieService) {
        this.demandeRepo = demandeRepo;
        this.soldeRepo = soldeRepo;
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

        demande.setUtilisateurId(utilisateurId);
        demande.setStatut("EN_ATTENTE");
        demande.setDateCreation(LocalDate.now());

        if (demande.getDateDebut() != null && demande.getDateFin() != null) {
            if (demande.getDateFin().isBefore(demande.getDateDebut())) {
                throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de début.");
            }

            int joursOuvres = calculerJoursOuvres(demande.getDateDebut(), demande.getDateFin());
            if (joursOuvres <= 0) {
                throw new IllegalArgumentException(
                        "Cette période ne contient aucune journée ouvrée. La demande de congé doit inclure au moins un jour ouvré.");
            }
            demande.setNombreJours(joursOuvres);

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
                notificationService
                        .creerNotification(
                                utilisateurId, "Votre demande de congé du " + demande.getDateDebut() + " au "
                                        + demande.getDateFin() + " a été soumise avec succès.",
                                "INFO", "/dashboard/demande");
            } catch (Exception e) {
                log.warn("Impossible de créer la notification pour la demande #{} : {}", saved.getId(), e.getMessage());
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

        DemandeConge demande = demandeRepo.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande non trouvée avec l'ID : " + demandeId));

        Long demandeurId = demande.getUtilisateurId();
        Utilisateur demandeur = utilisateurRepo.findById(demandeurId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborateur demandeur introuvable."));

        // Si le validateur est un MANAGER, il ne peut traiter que les demandes des
        // membres de son équipe
        if (currentUser.getRole() == Role.MANAGER) {
            if (!Objects.equals(demandeur.getManager_id(), currentUser.getId())) {
                throw new AccessDeniedException(
                        "Accès refusé : vous n'êtes pas le manager responsable de ce collaborateur.");
            }
        }

        String statutNormalise = statut == null ? "" : statut.trim().toUpperCase();
        if (!Set.of("VALIDEE", "REFUSEE", "ANNULEE").contains(statutNormalise)) {
            throw new IllegalArgumentException("Le statut de traitement est invalide.");
        }

        if (!"EN_ATTENTE".equalsIgnoreCase(demande.getStatut())) {
            throw new IllegalStateException("Seule une demande en attente peut être traitée.");
        }

        Long actualValidatorId = currentUser.getId();
        demande.setValideePar(actualValidatorId);
        demande.setStatut(statutNormalise);
        demande.setDateValidation(LocalDate.now());

        if ("REFUSEE".equalsIgnoreCase(statutNormalise)) {
            demande.setMotifRefus(motifRefus);
        }

        boolean estValidee = "VALIDEE".equalsIgnoreCase(statutNormalise) || "VALIDE".equalsIgnoreCase(statutNormalise)
                || "APPROVED".equalsIgnoreCase(statutNormalise);

        if (estValidee && demande.getDateDebut() != null && demande.getDateFin() != null) {
            int jours = calculerJoursOuvres(demande.getDateDebut(), demande.getDateFin());
            if (jours <= 0) {
                throw new IllegalStateException(
                        "La période demandée ne contient aucune journée ouvrée. Aucune validation ne peut être effectuée.");
            }
            demande.setNombreJours(jours);

            int annee = demande.getDateDebut().getYear();

            if (demande.getTypeCongeId() != null && demande.getUtilisateurId() != null) {
                Optional<Solde_conge> soldeOpt = soldeRepo.findByUtilisateurIdAndTypeCongeIdAndPeriode(
                        demande.getUtilisateurId(), demande.getTypeCongeId(), annee);

                if (soldeOpt.isEmpty()) {
                    throw new IllegalStateException(
                            "Aucun solde trouvé pour le collaborateur #" + demande.getUtilisateurId() + ", le type #"
                                    + demande.getTypeCongeId() + " pour l'année " + annee);
                }

                Solde_conge solde = soldeOpt.get();
                int prisActuel = (solde.getSoldePris() != null) ? solde.getSoldePris() : 0;
                int acquisActuel = (solde.getSoldeAquis() != null) ? solde.getSoldeAquis() : 0;
                int joursDisponibles = Math.max(0, acquisActuel - prisActuel);

                if (joursDisponibles < jours) {
                    throw new IllegalStateException("Solde insuffisant : " + jours + " jour(s) demandés pour "
                            + joursDisponibles + " jour(s) disponibles.");
                }

                int nouveauSoldePris = prisActuel + jours;
                solde.setSoldePris(nouveauSoldePris);
                solde.setSoldeRestant(Math.max(0, acquisActuel - nouveauSoldePris));
                solde.setDate_maj(new Timestamp(System.currentTimeMillis()));

                soldeRepo.save(solde);

                try {
                    historiqueService.enregistrerMouvement(demande.getUtilisateurId(), "DEBIT_CONGE", jours,
                            demande.getTypeCongeId(), demande.getId(),
                            "Congé validé par le responsable #" + actualValidatorId, actualValidatorId);
                } catch (Exception e) {
                    log.warn("Impossible d'enregistrer le mouvement d'historique : {}", e.getMessage());
                }
            }

            try {
                notificationService
                        .creerNotification(
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
                                + (motifRefus != null && !motifRefus.isEmpty() ? " (Motif : " + motifRefus + ")" : "."),
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

        DemandeConge demande = demandeRepo.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande non trouvée avec l'ID : " + demandeId));

        if (currentUser.getRole() != Role.ADMIN && !Objects.equals(currentUser.getId(), demande.getUtilisateurId())) {
            throw new AccessDeniedException("Vous ne pouvez annuler que vos propres demandes.");
        }

        if (!"EN_ATTENTE".equalsIgnoreCase(demande.getStatut()) && !"VALIDEE".equalsIgnoreCase(demande.getStatut())) {
            throw new IllegalStateException("Seule une demande en attente ou validée peut être annulée.");
        }

        if ("VALIDEE".equalsIgnoreCase(demande.getStatut())) {
            int joursAReintegrer = demande.getNombreJours() != null ? demande.getNombreJours()
                    : calculerJoursOuvres(demande.getDateDebut(), demande.getDateFin());
            if (demande.getTypeCongeId() != null && demande.getUtilisateurId() != null && demande.getDateDebut() != null
                    && demande.getDateFin() != null) {
                int annee = demande.getDateDebut().getYear();
                Optional<Solde_conge> soldeOpt = soldeRepo.findByUtilisateurIdAndTypeCongeIdAndPeriode(
                        demande.getUtilisateurId(), demande.getTypeCongeId(), annee);
                if (soldeOpt.isPresent()) {
                    Solde_conge solde = soldeOpt.get();
                    int prisActuel = solde.getSoldePris() != null ? solde.getSoldePris() : 0;
                    int acquisActuel = solde.getSoldeAquis() != null ? solde.getSoldeAquis() : 0;
                    int nouveauPris = Math.max(0, prisActuel - joursAReintegrer);
                    solde.setSoldePris(nouveauPris);
                    solde.setSoldeRestant(Math.max(0, acquisActuel - nouveauPris));
                    solde.setDate_maj(new Timestamp(System.currentTimeMillis()));
                    soldeRepo.save(solde);
                }
            }
        }

        demande.setStatut("ANNULEE");
        return demandeRepo.save(demande);
    }

    private int calculerJoursOuvres(LocalDate dateDebut, LocalDate dateFin) {
        return jourFerieService.compterJoursOuvres(dateDebut, dateFin);
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