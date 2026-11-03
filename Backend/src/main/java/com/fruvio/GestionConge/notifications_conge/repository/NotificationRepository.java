package com.fruvio.GestionConge.notifications_conge.repository;

import java.sql.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fruvio.GestionConge.notifications_conge.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUtilisateurIdAndLueFalse(Long utilisateurId);

    List<Notification> findByUtilisateurId(Long utilisateurId);

    boolean existsByUtilisateurId(Long utilisateurId);

    @Query("select case when count(n) > 0 then true else false end from Notification n "
            + "where n.utilisateurId = :utilisateurId and n.type = :type and n.date_envoi = :dateEnvoi")
    boolean existsForRecipientAndTypeOnDate(
            @Param("utilisateurId") Long utilisateurId,
            @Param("type") String type,
            @Param("dateEnvoi") Date dateEnvoi);
}