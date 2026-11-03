package com.fruvio.GestionConge.automation.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fruvio.GestionConge.demande_conge.entity.DemandeConge;
import com.fruvio.GestionConge.demande_conge.repository.DemandeCongeRepository;
import com.fruvio.GestionConge.historique_mouvement.service.Historique_mouvementService;
import com.fruvio.GestionConge.notifications_conge.repository.NotificationRepository;
import com.fruvio.GestionConge.notifications_conge.service.Notification_congeService;
import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@Service
public class AutomatisationCongeService {

    private static final Logger log = LoggerFactory.getLogger(AutomatisationCongeService.class);
    private static final ZoneId ZONE = ZoneId.of("Africa/Antananarivo");

    private final UtilisateurRepository utilisateurRepository;
    private final Type_congeRepository typeCongeRepository;
    private final Solde_congeRepository soldeCongeRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final NotificationRepository notificationRepository;
    private final Notification_congeService notificationService;
    private final Historique_mouvementService historiqueService;

    public AutomatisationCongeService(
            UtilisateurRepository utilisateurRepository,
            Type_congeRepository typeCongeRepository,
            Solde_congeRepository soldeCongeRepository,
            DemandeCongeRepository demandeCongeRepository,
            NotificationRepository notificationRepository,
            Notification_congeService notificationService,
            Historique_mouvementService historiqueService) {
        this.utilisateurRepository = utilisateurRepository;
        this.typeCongeRepository = typeCongeRepository;
        this.soldeCongeRepository = soldeCongeRepository;
        this.demandeCongeRepository = demandeCongeRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.historiqueService = historiqueService;
    }

    /*
     * Contrôle quotidien idempotent : les droits viennent uniquement de
     * nombreJoursParAn configuré par l'administration. Aucun taux légal
     * ni prorata non confirmé n'est introduit par cette automatisation.
     */
    @Scheduled(cron = "0 15 2 * * *", zone = "Africa/Antananarivo")
    @Transactional
    public void initialiserSoldesAnnuelsManquants() {
        int annee = LocalDate.now(ZONE).getYear();
        List<Utilisateur> utilisateurs = utilisateurRepository.findByActifTrue();
        List<Type_conge> types = typeCongeRepository.findByActifTrue();
        int created = 0;

        for (Utilisateur utilisateur : utilisateurs) {
            if (utilisateur.getId() == null) {
                continue;
            }
            for (Type_conge type : types) {
                if (type.getId() == null
                        || soldeCongeRepository.existsByUtilisateurIdAndTypeCongeIdAndPeriode(
                                utilisateur.getId(), type.getId(), annee)) {
                    continue;
                }

                int jours = type.getNombreJoursParAn() == null
                        ? 0 : Math.max(0, type.getNombreJoursParAn());

                Solde_conge solde = new Solde_conge();
                solde.setUtilisateurId(utilisateur.getId());
                solde.setTypeCongeId(type.getId());
                solde.setPeriode(annee);
                solde.setSoldeAquis(jours);
                solde.setSoldePris(0);
                solde.setSoldeRestant(jours);
                solde.setDate_maj(new Timestamp(System.currentTimeMillis()));
                soldeCongeRepository.save(solde);

                historiqueService.enregistrerMouvement(
                        utilisateur.getId(), "ACQUISITION_ANNUELLE", jours, type.getId(), null,
                        "Attribution annuelle automatique pour " + annee, null);
                created++;
            }
        }

        log.info("Initialisation annuelle terminée : {} solde(s) créé(s) pour {}.", created, annee);
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Africa/Antananarivo")
    @Transactional
    public void rappelerDemandesEnAttente() {
        int reminderDays = 3;
        LocalDate today = LocalDate.now(ZONE);
        LocalDate cutoff = today.minusDays(reminderDays);
        List<DemandeConge> pending = demandeCongeRepository
                .findByStatutAndDateCreationLessThanEqual("EN_ATTENTE", cutoff);

        Map<Long, Integer> countByRecipient = new HashMap<>();
        for (DemandeConge demande : pending) {
            Utilisateur demandeur = utilisateurRepository.findById(demande.getUtilisateurId()).orElse(null);
            if (demandeur == null) {
                continue;
            }

            Utilisateur manager = demandeur.getManager_id() == null
                    ? null : utilisateurRepository.findById(demandeur.getManager_id()).orElse(null);

            if (manager != null && manager.isActif() && manager.getRole() == Role.MANAGER) {
                countByRecipient.merge(manager.getId(), 1, Integer::sum);
            } else {
                for (Utilisateur admin : utilisateurRepository.findByRoleAndActifTrue(Role.ADMIN)) {
                    countByRecipient.merge(admin.getId(), 1, Integer::sum);
                }
            }
        }

        Date sqlToday = Date.valueOf(today);
        for (Map.Entry<Long, Integer> entry : countByRecipient.entrySet()) {
            if (notificationRepository.existsByUtilisateurIdAndTypeAndDate_envoi(
                    entry.getKey(), "RAPPEL_CONGE", sqlToday)) {
                continue;
            }

            String message = entry.getValue() + " demande(s) de congé sont en attente depuis au moins "
                    + reminderDays + " jour(s). Merci de vérifier votre espace de traitement.";
            notificationService.creerNotification(
                    entry.getKey(), message, "RAPPEL_CONGE", "/dashboard/demande");
        }

        log.info("Rappels traités pour {} destinataire(s).", countByRecipient.size());
    }

    @Scheduled(cron = "0 30 8 1 * *", zone = "Africa/Antananarivo")
    @Transactional
    public void envoyerBilanMensuelRH() {
        LocalDate today = LocalDate.now(ZONE);
        YearMonth month = YearMonth.from(today).minusMonths(1);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<DemandeConge> requests = demandeCongeRepository.findAll().stream()
                .filter(d -> d.getDateCreation() != null
                        && !d.getDateCreation().isBefore(start)
                        && !d.getDateCreation().isAfter(end))
                .toList();

        long validated = requests.stream().filter(d -> "VALIDEE".equalsIgnoreCase(d.getStatut())).count();
        long refused = requests.stream().filter(d -> "REFUSEE".equalsIgnoreCase(d.getStatut())).count();
        long pending = requests.stream().filter(d -> "EN_ATTENTE".equalsIgnoreCase(d.getStatut())).count();
        long cancelled = requests.stream().filter(d -> "ANNULEE".equalsIgnoreCase(d.getStatut())).count();

        String message = "Bilan RH de " + month + "\n"
                + "Demandes créées : " + requests.size() + "\n"
                + "Validées : " + validated + "\n"
                + "Refusées : " + refused + "\n"
                + "En attente : " + pending + "\n"
                + "Annulées : " + cancelled + "\n\n"
                + "Bilan généré automatiquement à partir des demandes enregistrées dans GestionConge.";

        Date sqlToday = Date.valueOf(today);
        List<Utilisateur> admins = utilisateurRepository.findByRoleAndActifTrue(Role.ADMIN);
        for (Utilisateur admin : admins) {
            if (notificationRepository.existsByUtilisateurIdAndTypeAndDate_envoi(
                    admin.getId(), "RAPPORT_RH", sqlToday)) {
                continue;
            }
            notificationService.creerNotification(
                    admin.getId(), message, "RAPPORT_RH", "/dashboard/demande");
        }

        log.info("Bilan RH {} généré pour {} administrateur(s).", month, admins.size());
    }
}