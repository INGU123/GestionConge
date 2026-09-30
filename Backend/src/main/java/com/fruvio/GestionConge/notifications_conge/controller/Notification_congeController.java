package com.fruvio.GestionConge.notifications_conge.controller;

import java.sql.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fruvio.GestionConge.notifications_conge.entity.Notification;
import com.fruvio.GestionConge.notifications_conge.repository.NotificationRepository;
import com.fruvio.GestionConge.notifications_conge.service.Notification_congeService;
import com.fruvio.GestionConge.utilisateur.config.CustomUserDetails;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

@RestController
@RequestMapping("/notification")
public class Notification_congeController {

    private final NotificationRepository notificationRepository;
    private final Notification_congeService notificationService;

    public Notification_congeController(NotificationRepository notificationRepository,
                                        Notification_congeService notificationService) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    // Notifications non lues du compte authentifié
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @GetMapping("/utilisateur/{utilisateurId}/non-lues")
    public ResponseEntity<List<Notification>> getNonLues(@PathVariable Long utilisateurId) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        verifierPropriete(utilisateurId, currentUser);
        return ResponseEntity.ok(notificationService.getNotificationsNonLues(utilisateurId));
    }

    // Toutes les notifications du compte authentifié
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<List<Notification>> getToutes(@PathVariable Long utilisateurId) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        verifierPropriete(utilisateurId, currentUser);
        return ResponseEntity.ok(notificationService.getToutesNotifications(utilisateurId));
    }

    // Marquer comme lue
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @PutMapping("/{notificationId}/lire")
    public ResponseEntity<Void> marquerCommeLue(@PathVariable Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }

        Utilisateur currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null || !Objects.equals(currentUser.getId(), notification.getUtilisateurId())) {
            throw new AccessDeniedException("Vous ne pouvez marquer comme lue que vos propres notifications.");
        }

        notificationService.marquerCommeLue(notificationId);
        return ResponseEntity.ok().build();
    }

    // Créer manuellement une notification (Réservé à l'Admin)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/creer")
    public ResponseEntity<String> creerNotification(
            @RequestBody(required = false) Notification body,
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(required = false) String contenu,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "false") boolean lue,
            @RequestParam(required = false) Date date_envoi,
            @RequestParam(required = false) String lien) {

        Long targetUserId = body != null && body.getUtilisateurId() != null ? body.getUtilisateurId() : utilisateurId;
        String targetContenu = body != null && body.getContenu() != null ? body.getContenu() : contenu;
        String targetType = body != null && body.getType() != null ? body.getType() : type;
        String targetLien = body != null && body.getLien() != null ? body.getLien() : lien;

        if (targetUserId == null || targetContenu == null) {
            return ResponseEntity.badRequest().body("L'identifiant utilisateur et le contenu sont obligatoires.");
        }

        Notification notif = Notification.builder()
                .utilisateurId(targetUserId)
                .contenu(targetContenu)
                .type(targetType != null ? targetType : "INFO")
                .lue(lue)
                .date_envoi(date_envoi != null ? date_envoi : new Date(System.currentTimeMillis()))
                .lien(targetLien)
                .build();

        notificationRepository.save(notif);
        return ResponseEntity.ok("Notification créée avec succès");
    }

    private void verifierPropriete(Long utilisateurId, Utilisateur currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise.");
        }
        if (!Objects.equals(currentUser.getId(), utilisateurId)) {
            throw new AccessDeniedException("Vous ne pouvez consulter que vos propres notifications.");
        }
    }

    private Utilisateur getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUtilisateur();
        }
        if (principal instanceof Utilisateur utilisateur) {
            return utilisateur;
        }

        return null;
    }
}