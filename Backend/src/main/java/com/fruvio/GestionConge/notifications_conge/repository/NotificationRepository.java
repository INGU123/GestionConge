package com.fruvio.GestionConge.notifications_conge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fruvio.GestionConge.notifications_conge.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUtilisateurIdAndLueFalse(Long utilisateurId);
    List<Notification> findByUtilisateurId(Long utilisateurId);
    boolean existsByUtilisateurId(Long utilisateurId);
}
