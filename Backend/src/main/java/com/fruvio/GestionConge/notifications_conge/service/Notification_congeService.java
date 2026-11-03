package com.fruvio.GestionConge.notifications_conge.service;

import java.sql.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fruvio.GestionConge.notifications_conge.entity.Notification;
import com.fruvio.GestionConge.notifications_conge.repository.NotificationRepository;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;
import com.fruvio.GestionConge.utilisateur.service.EmailService;

@Service
public class Notification_congeService {

    private static final Logger log = LoggerFactory.getLogger(Notification_congeService.class);
    private final NotificationRepository notificationRepository;
    private EmailService emailService;
    private UtilisateurRepository utilisateurRepository;

    public Notification_congeService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Autowired
    public void configureEmailDelivery(EmailService emailService, UtilisateurRepository utilisateurRepository) {
        this.emailService = emailService;
        this.utilisateurRepository = utilisateurRepository;
    }

    public void creerNotification(Long utilisateurId, String contenu, String type, String lien) {
        if (utilisateurId == null || contenu == null || contenu.isBlank()) {
            log.warn("Notification ignorée : destinataire ou contenu invalide.");
            return;
        }

        Notification notification = Notification.builder()
                .utilisateurId(utilisateurId)
                .contenu(contenu)
                .type(type)
                .lue(false)
                .date_envoi(new Date(System.currentTimeMillis()))
                .lien(lien)
                .build();
        notificationRepository.save(notification);

        // L'e-mail est complémentaire : une panne SMTP ne doit pas annuler l'action métier.
        if (emailService == null || utilisateurRepository == null) return;
        try {
            Utilisateur destinataire = utilisateurRepository.findById(utilisateurId).orElse(null);
            if (destinataire == null || !destinataire.isActif()
                    || destinataire.getEmail() == null || destinataire.getEmail().isBlank()) return;
            String sujet = "Gestion des congés - " + (type == null ? "Notification" : type);
            emailService.sendEmail(destinataire.getEmail(), sujet,
                    contenu + (lien == null || lien.isBlank() ? "" : "\n\nAccès : " + lien));
        } catch (Exception e) {
            log.error("Notification interne enregistrée, mais l'e-mail vers l'utilisateur #{} a échoué.",
                    utilisateurId, e);
        }
    }

    public List<Notification> getNotificationsNonLues(Long utilisateurId) {
        return notificationRepository.findByUtilisateurIdAndLueFalse(utilisateurId);
    }

    public void marquerCommeLue(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null) {
            notification.setLue(true);
            notification.setDate_lecture(new Date(System.currentTimeMillis()));
            notificationRepository.save(notification);
        }
    }

    public List<Notification> getToutesNotifications(Long utilisateurId) {
        return notificationRepository.findByUtilisateurId(utilisateurId);
    }
}