package com.fruvio.GestionConge.notifications_conge.service;

import java.sql.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import com.fruvio.GestionConge.notifications_conge.entity.Notification;
import com.fruvio.GestionConge.notifications_conge.repository.NotificationRepository;

@Service
public class Notification_congeService {

    private final NotificationRepository notificationRepository;

    public Notification_congeService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Créer une notification
    public void creerNotification(Long utilisateurId, String contenu, String type, String lien) {
        Notification notification = Notification.builder().utilisateurId(utilisateurId).contenu(contenu).type(type)
                .lue(false).date_envoi(new Date(System.currentTimeMillis())).lien(lien).build();

        notificationRepository.save(notification);
    }

    // Récupérer les notifications non lues d’un utilisateur
    public List<Notification> getNotificationsNonLues(Long utilisateurId) {
        return notificationRepository.findByUtilisateurIdAndLueFalse(utilisateurId);
    }

    // Marquer une notification comme lue
    public void marquerCommeLue(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null) {
            notification.setLue(true);
            notification.setDate_lecture(new Date(System.currentTimeMillis()));
            notificationRepository.save(notification);
        }
    }

    // Récupérer toutes les notifications d’un utilisateur
    public List<Notification> getToutesNotifications(Long utilisateurId) {
        return notificationRepository.findByUtilisateurId(utilisateurId);
    }
}
