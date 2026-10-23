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
import com.fruvio.GestionConge.solde_conge.entity.AffectationConge;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.solde_conge.repository.AffectationCongeRepository;
import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.type_conge.service.ReglesCongeMadagascar;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@Service
public class DemandeCongeService {

    private static final Logger log = LoggerFactory.getLogger(DemandeCongeService.class);

    private final DemandeCongeRepository demandeRepo;
    private final Solde_congeRepository soldeRepo;
    private final AffectationCongeRepository affectationRepo;
    private final Type_congeRepository typeCongeRepo;
    private final Historique_mouvementService historiqueService;
    private final Notification_congeService notificationService;
    private final UtilisateurRepository utilisateurRepo;
    private final JourFerieService jourFerieService;

    public DemandeCongeService(DemandeCongeRepository demandeRepo, Solde_congeRepository soldeRepo,
            AffectationCongeRepository affectationRepo, Type_congeRepository typeCongeRepo,
            Historique_mouvementService historiqueService, Notification_congeService notificationService,
            UtilisateurRepository utilisateurRepo, JourFerieService jourFerieService) {
        this.demandeRepo = demandeRepo;
        this.soldeRepo = soldeRepo;
        this.affectationRepo = affectationRepo;
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

        demande.setUtilisateurId(utilisateurId);
        demande.setStatut("EN_ATTENTE");
        demande.setDateCreation(LocalDate.now());

        if (demande.getDateDebut() == null || demande.getDateFin() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }
        if (demande.getDateFin().isBefore(demande.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de début.");
        }
        if (demande.getTypeCongeId() == null) {
            throw new IllegalArgumentException("Veuillez sélectionner un type de congé.");
        }

        Type_conge typeConge = typeCongeRepo.findById(demande.getTypeCongeId())
                .orElseThrow(() -> new IllegalArgumentException("Le type de congé sélectionné n'existe pas."));

        boolean congeAnnuel = ReglesCongeMadagascar.estCongeAnnuel(typeConge);
        int nombreJours = congeAnnuel
                ? jourFerieService.compterJoursCongeAnnuel(demande.getDateDebut(), demande.getDateFin())
                : calculerJoursOuvres(demande.getDateDebut(), demande.getDateFin());

        if (nombreJours <= 0) {
            throw new IllegalArgumentException(congeAnnuel
                    ? "La période demandée ne contient aucun jour de congé décomptable après exclusion des jours fériés."
                    : "Cette période ne contient aucune journée ouvrée. La demande doit inclure au moins un jour ouvré.");
        }

        if (congeAnnuel) {
            Utilisateur demandeur = utilisateurRepo.findById(utilisateurId)
                    .orElseThrow(() -> new IllegalArgumentException("Collaborateur demandeur introuvable."));
            ReglesCongeMadagascar.verifierOuvertureDroit(demandeur, demande.getDateDebut());
            verifierPremiereFraction(demande, demandeur, nombreJours);
        }

        Integer limiteJours = typeConge.getNombreJoursParAn();
        if (limiteJours != null && limiteJours > 0 && nombreJours > limiteJours) {
            throw new IllegalArgumentException(
                    "Demande non autorisée : le type « " + typeConge.getLibelle()
                            + " » autorise au maximum " + limiteJours
                            + " jour(s) par demande, mais vous en demandez " + nombreJours + ".");
        }
        demande.setNombreJours(nombreJours);

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

        String motifRefusNormalise = motifRefus == null ? "" : motifRefus.trim();
        if ("REFUSEE".equals(statutNormalise) && motifRefusNormalise.isEmpty()) {
            throw new IllegalArgumentException(
                    "Le motif de refus est obligatoire. Veuillez justifier votre décision avant de continuer.");
        }

        if (!"EN_ATTENTE".equalsIgnoreCase(demande.getStatut())) {
            throw new IllegalStateException("Seule une demande en attente peut être traitée.");
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
            Type_conge typeConge = typeCongeRepo.findById(demande.getTypeCongeId())
                    .orElseThrow(() -> new IllegalStateException("Le type de congé de cette demande n'existe plus."));
            boolean congeAnnuel = ReglesCongeMadagascar.estCongeAnnuel(typeConge);
            int jours = congeAnnuel
                    ? jourFerieService.compterJoursCongeAnnuel(demande.getDateDebut(), demande.getDateFin())
                    : calculerJoursOuvres(demande.getDateDebut(), demande.getDateFin());

            if (jours <= 0) {
                throw new IllegalStateException(congeAnnuel
                        ? "La période ne contient aucun jour de congé annuel décomptable."
                        : "La période demandée ne contient aucune journée ouvrée. Aucune validation ne peut être effectuée.");
            }
            if (congeAnnuel) {
                ReglesCongeMadagascar.verifierOuvertureDroit(demandeur, demande.getDateDebut());
                verifierPremiereFraction(demande, demandeur, jours);
            }
            demande.setNombreJours(jours);

            int annee = demande.getDateDebut().getYear();

            if (demande.getTypeCongeId() != null && demande.getUtilisateurId() != null) {
                if (congeAnnuel) {
                    debiterDroitsCongesAnnuels(demande, typeConge, jours, annee, demandeur);
                } else {
                    Optional<Solde_conge> soldeOpt = soldeRepo.findByUtilisateurIdAndTypeCongeIdAndPeriode(
                            demande.getUtilisateurId(), demande.getTypeCongeId(), annee);
                    if (soldeOpt.isEmpty()) {
                        throw new IllegalStateException(
                                "Aucun solde trouvé pour le collaborateur #" + demande.getUtilisateurId()
                                        + ", le type #" + demande.getTypeCongeId() + " pour l'année " + annee);
                    }

                    Solde_conge solde = soldeOpt.get();
                    int prisActuel = solde.getSoldePris() != null ? solde.getSoldePris() : 0;
                    int acquisActuel = solde.getSoldeAquis() != null ? solde.getSoldeAquis() : 0;
                    int disponibles = Math.max(0, acquisActuel - prisActuel);
                    if (disponibles < jours) {
                        throw new IllegalStateException("Solde insuffisant : " + jours
                                + " jour(s) demandés pour " + disponibles + " jour(s) disponibles.");
                    }
                    int nouveauPris = prisActuel + jours;
                    solde.setSoldePris(nouveauPris);
                    solde.setSoldeRestant(Math.max(0, acquisActuel - nouveauPris));
                    solde.setDate_maj(new Timestamp(System.currentTimeMillis()));
                    soldeRepo.save(solde);
                }

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

        DemandeConge demande = demandeRepo.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande non trouvée avec l'ID : " + demandeId));

        if (currentUser.getRole() != Role.ADMIN && !Objects.equals(currentUser.getId(), demande.getUtilisateurId())) {
            throw new AccessDeniedException("Vous ne pouvez annuler que vos propres demandes.");
        }

        if (!"EN_ATTENTE".equalsIgnoreCase(demande.getStatut()) && !"VALIDEE".equalsIgnoreCase(demande.getStatut())) {
            throw new IllegalStateException("Seule une demande en attente ou validée peut être annulée.");
        }

        if ("VALIDEE".equalsIgnoreCase(demande.getStatut())) {
            Type_conge typeConge = demande.getTypeCongeId() == null ? null
                    : typeCongeRepo.findById(demande.getTypeCongeId()).orElse(null);
            List<AffectationConge> affectations = affectationRepo.findByDemandeCongeId(demandeId);

            if (ReglesCongeMadagascar.estCongeAnnuel(typeConge) && !affectations.isEmpty()) {
                for (AffectationConge affectation : affectations) {
                    soldeRepo.findById(affectation.getSoldeCongeId()).ifPresent(solde -> {
                        int prisActuel = solde.getSoldePris() != null ? solde.getSoldePris() : 0;
                        int acquisActuel = solde.getSoldeAquis() != null ? solde.getSoldeAquis() : 0;
                        int nouveauPris = Math.max(0, prisActuel - affectation.getNombreJours());
                        solde.setSoldePris(nouveauPris);
                        solde.setSoldeRestant(Math.max(0, acquisActuel - nouveauPris));
                        solde.setDate_maj(new Timestamp(System.currentTimeMillis()));
                        soldeRepo.save(solde);
                    });
                }
                affectationRepo.deleteByDemandeCongeId(demandeId);
            } else if (demande.getTypeCongeId() != null && demande.getUtilisateurId() != null
                    && demande.getDateDebut() != null && demande.getDateFin() != null) {
                // Compatibilité avec les anciennes demandes qui n'ont pas d'affectation historisée.
                int joursAReintegrer = demande.getNombreJours() != null ? demande.getNombreJours()
                        : (ReglesCongeMadagascar.estCongeAnnuel(typeConge)
                                ? jourFerieService.compterJoursCongeAnnuel(demande.getDateDebut(), demande.getDateFin())
                                : calculerJoursOuvres(demande.getDateDebut(), demande.getDateFin()));
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



    /**
     * Débite les droits annuels les plus anciens en premier. Les reliquats des
     * trois dernières années peuvent ainsi être utilisés conformément à l'article 130.
     * Chaque débit est historisé afin que l'annulation restitue les bons millésimes.
     */
    private void debiterDroitsCongesAnnuels(DemandeConge demande, Type_conge typeConge,
            int joursDemandes, int anneeDemande, Utilisateur demandeur) {
        if (demandeur.getDate_embauche() == null) {
            throw new IllegalStateException("La date d'embauche est nécessaire pour calculer les droits annuels.");
        }
        int minimumLegal = Math.max(ReglesCongeMadagascar.JOURS_ANNUELS_PAR_AN,
                typeConge.getNombreJoursParAn() == null ? 0 : typeConge.getNombreJoursParAn());
        LocalDate ouvertureDroit = ReglesCongeMadagascar.dateOuvertureDroit(demandeur);

        // Garantit l'existence des millésimes nécessaires au report sur trois ans.
        int premiereAnneeUtile = Math.max(demandeur.getDate_embauche().getYear(), anneeDemande - 2);
        for (int annee = premiereAnneeUtile; annee <= anneeDemande; annee++) {
            if (!soldeRepo.existsByUtilisateurIdAndTypeCongeIdAndPeriode(
                    demande.getUtilisateurId(), demande.getTypeCongeId(), annee)) {
                int acquis = LocalDate.of(annee, 12, 31).isBefore(ouvertureDroit) ? 0 : minimumLegal;
                Solde_conge nouveau = new Solde_conge();
                nouveau.setUtilisateurId(demande.getUtilisateurId());
                nouveau.setTypeCongeId(demande.getTypeCongeId());
                nouveau.setPeriode(annee);
                nouveau.setSoldeAquis(acquis);
                nouveau.setSoldePris(0);
                nouveau.setSoldeRestant(acquis);
                nouveau.setDate_maj(new Timestamp(System.currentTimeMillis()));
                soldeRepo.save(nouveau);
            }
        }

        List<Solde_conge> soldes = soldeRepo.findByUtilisateurIdAndTypeCongeId(
                demande.getUtilisateurId(), demande.getTypeCongeId());
        Solde_conge soldeAnneeCourante = soldes.stream()
                .filter(s -> s.getPeriode() == anneeDemande)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Impossible d'initialiser le solde de congé annuel pour l'année " + anneeDemande + "."));

        int acquisCourant = soldeAnneeCourante.getSoldeAquis() == null ? 0 : soldeAnneeCourante.getSoldeAquis();
        int prisCourant = soldeAnneeCourante.getSoldePris() == null ? 0 : soldeAnneeCourante.getSoldePris();

        if (prisCourant == 0 && acquisCourant < minimumLegal
                && demandeur.getDate_embauche() != null
                && !demande.getDateDebut().isBefore(demandeur.getDate_embauche().plusYears(1))) {
            soldeAnneeCourante.setSoldeAquis(minimumLegal);
            soldeAnneeCourante.setSoldeRestant(minimumLegal);
            soldeAnneeCourante.setDate_maj(new Timestamp(System.currentTimeMillis()));
            soldeRepo.save(soldeAnneeCourante);
        }

        List<Solde_conge> soldesUtilisables = soldes.stream()
                .filter(s -> s.getPeriode() >= anneeDemande - 2 && s.getPeriode() <= anneeDemande)
                .sorted((a, b) -> Integer.compare(a.getPeriode(), b.getPeriode()))
                .toList();

        int totalDisponible = soldesUtilisables.stream()
                .mapToInt(s -> Math.max(0, (s.getSoldeAquis() == null ? 0 : s.getSoldeAquis())
                        - (s.getSoldePris() == null ? 0 : s.getSoldePris())))
                .sum();

        if (totalDisponible < joursDemandes) {
            throw new IllegalStateException("Solde de congé annuel insuffisant : " + joursDemandes
                    + " jour(s) demandés, " + totalDisponible
                    + " jour(s) disponibles en tenant compte des reliquats utilisables sur trois ans.");
        }

        int restantAImputer = joursDemandes;
        for (Solde_conge solde : soldesUtilisables) {
            if (restantAImputer == 0) {
                break;
            }
            int acquis = solde.getSoldeAquis() == null ? 0 : solde.getSoldeAquis();
            int pris = solde.getSoldePris() == null ? 0 : solde.getSoldePris();
            int disponible = Math.max(0, acquis - pris);
            int imputation = Math.min(disponible, restantAImputer);
            if (imputation == 0) {
                continue;
            }

            solde.setSoldePris(pris + imputation);
            solde.setSoldeRestant(Math.max(0, acquis - (pris + imputation)));
            solde.setDate_maj(new Timestamp(System.currentTimeMillis()));
            soldeRepo.save(solde);

            AffectationConge affectation = new AffectationConge();
            affectation.setDemandeCongeId(demande.getId());
            affectation.setSoldeCongeId(solde.getId());
            affectation.setNombreJours(imputation);
            affectationRepo.save(affectation);

            restantAImputer -= imputation;
        }
    }

    /**
     * Vérifie la première fraction de congé annuel : 15 jours continus au cours
     * des trois mois suivant l'ouverture du droit, en tenant compte des congés
     * annuels déjà validés dans cette fenêtre.
     */
    private void verifierPremiereFraction(DemandeConge demande, Utilisateur demandeur, int nombreJours) {
        if (!ReglesCongeMadagascar.estDansFenetrePremiereFraction(demandeur, demande.getDateDebut())) {
            return;
        }
        if (nombreJours >= ReglesCongeMadagascar.PREMIERE_FRACTION_JOURS) {
            return;
        }

        LocalDate ouverture = ReglesCongeMadagascar.dateOuvertureDroit(demandeur);
        LocalDate finFenetre = ouverture.plusMonths(3).minusDays(1);
        boolean fractionDejaPrise = demandeRepo.findByUtilisateurId(demande.getUtilisateurId()).stream()
                .filter(existing -> existing != null
                        && !Objects.equals(existing.getId(), demande.getId())
                        && Objects.equals(existing.getTypeCongeId(), demande.getTypeCongeId())
                        && "VALIDEE".equalsIgnoreCase(existing.getStatut())
                        && existing.getDateDebut() != null
                        && !existing.getDateDebut().isBefore(ouverture)
                        && !existing.getDateDebut().isAfter(finFenetre))
                .anyMatch(existing -> existing.getNombreJours() != null
                        && existing.getNombreJours() >= ReglesCongeMadagascar.PREMIERE_FRACTION_JOURS);

        if (!fractionDejaPrise) {
            throw new IllegalArgumentException(
                    "La première fraction du congé annuel doit être prise de manière ininterrompue sur au moins "
                            + ReglesCongeMadagascar.PREMIERE_FRACTION_JOURS
                            + " jours dans les trois mois suivant l'ouverture du droit (article 130).");
        }
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