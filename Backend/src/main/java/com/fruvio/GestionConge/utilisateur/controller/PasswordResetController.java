package com.fruvio.GestionConge.utilisateur.controller;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.fruvio.GestionConge.utilisateur.dto.ForgotPasswordRequest;
import com.fruvio.GestionConge.utilisateur.dto.ResetPasswordRequest;
import com.fruvio.GestionConge.utilisateur.entity.PasswordResetRequestEntity;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.PasswordResetRequestRepository;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;
import com.fruvio.GestionConge.utilisateur.service.EmailService;
import com.fruvio.GestionConge.utilisateur.service.PasswordResetService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);
    private final PasswordResetService passwordResetService;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordResetRequestRepository requestRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetController(PasswordResetService passwordResetService,
            UtilisateurRepository utilisateurRepository,
            PasswordResetRequestRepository requestRepository,
            EmailService emailService, PasswordEncoder passwordEncoder) {
        this.passwordResetService = passwordResetService;
        this.utilisateurRepository = utilisateurRepository;
        this.requestRepository = requestRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest body) {
        String matricule = body.getMatricule().trim();
        Utilisateur user = utilisateurRepository.findByMatricule(matricule).orElse(null);

        // Réponse uniforme : ne pas révéler si un matricule existe.
        if (user != null && user.isActif()) {
            boolean pending = requestRepository.findByStatutOrderByDateDemandeDesc("EN_ATTENTE")
                    .stream().anyMatch(r -> r.getUtilisateur().getId().equals(user.getId()));
            if (!pending) {
                PasswordResetRequestEntity request = requestRepository.save(PasswordResetRequestEntity.builder()
                        .utilisateur(user).statut("EN_ATTENTE").dateDemande(LocalDateTime.now()).build());
                List<Utilisateur> admins = utilisateurRepository.findAll().stream()
                        .filter(u -> u.getRole() == Role.ADMIN && u.isActif())
                        .collect(Collectors.toList());
                String subject = "Demande de réinitialisation de mot de passe - SPAT";
                String text = "Bonjour Administrateur,\n\nUne demande de réinitialisation de mot de passe nécessite votre validation.\n"
                        + "Agent : " + user.getPrenom() + " " + user.getNom() + "\nMatricule : " + user.getMatricule()
                        + "\nDate : " + request.getDateDemande() + "\n\nConnectez-vous à GestionConge, rubrique « Réinitialisations », pour accepter ou refuser cette demande.\n";
                for (Utilisateur admin : admins) {
                    try { emailService.sendEmail(admin.getEmail(), subject, text); }
                    catch (Exception ex) {
                        log.error("La demande de réinitialisation {} est enregistrée, mais l'e-mail d'alerte à l'administrateur (matricule {}) a échoué.",
                                request.getId(), admin.getMatricule(), ex);
                    }
                }
            }
        }
        return ResponseEntity.ok(Map.of("success", true,
                "message", "Si ce matricule correspond à un compte actif, la demande sera transmise à l'administrateur."));
    }

    @GetMapping("/admin/password-reset-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> pendingRequests() {
        List<Map<String, Object>> result = requestRepository.findByStatutOrderByDateDemandeDesc("EN_ATTENTE")
                .stream().map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "matricule", r.getUtilisateur().getMatricule() == null ? "" : r.getUtilisateur().getMatricule(),
                        "nom", r.getUtilisateur().getNom() == null ? "" : r.getUtilisateur().getNom(),
                        "prenom", r.getUtilisateur().getPrenom() == null ? "" : r.getUtilisateur().getPrenom(),
                        "email", r.getUtilisateur().getEmail(),
                        "dateDemande", r.getDateDemande().toString()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin/password-reset-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> approveRequest(@PathVariable Long id) {
        PasswordResetRequestEntity request = requestRepository.findById(id).orElse(null);
        if (request == null || !"EN_ATTENTE".equals(request.getStatut())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Demande introuvable ou déjà traitée."));
        }
        Utilisateur user = request.getUtilisateur();
        String temporaryPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        utilisateurRepository.save(user);
        request.setStatut("ACCEPTEE");
        request.setDateTraitement(LocalDateTime.now());
        requestRepository.save(request);
        emailService.sendEmail(user.getEmail(), "Réinitialisation de votre mot de passe - SPAT",
                "Bonjour " + user.getPrenom() + ",\n\nVotre demande de réinitialisation a été acceptée.\n"
                + "Matricule : " + user.getMatricule() + "\nNouveau mot de passe temporaire : " + temporaryPassword
                + "\n\nConnectez-vous à GestionConge et changez ce mot de passe dès que possible.\n\nService RH - SPAT");
        return ResponseEntity.ok(Map.of("success", true, "message", "Demande acceptée. Les identifiants ont été envoyés à l'agent."));
    }

    @PostMapping("/admin/password-reset-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> rejectRequest(@PathVariable Long id) {
        PasswordResetRequestEntity request = requestRepository.findById(id).orElse(null);
        if (request == null || !"EN_ATTENTE".equals(request.getStatut())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Demande introuvable ou déjà traitée."));
        }
        request.setStatut("REFUSEE");
        request.setDateTraitement(LocalDateTime.now());
        requestRepository.save(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Demande refusée."));
    }

    private String generateTemporaryPassword() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#";
        SecureRandom random = new SecureRandom();
        StringBuilder value = new StringBuilder(12);
        for (int i = 0; i < 12; i++) value.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return value.toString();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @Valid @RequestBody(required = false) ResetPasswordRequest body,
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String newPassword) {
        String resetToken = body != null && body.getToken() != null ? body.getToken() : token;
        String pass = body != null && body.getNewPassword() != null ? body.getNewPassword() : newPassword;
        if (resetToken == null || resetToken.trim().isEmpty() || pass == null || pass.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Le jeton et le nouveau mot de passe sont obligatoires."));
        }
        try {
            passwordResetService.resetPassword(resetToken.trim(), pass.trim());
            return ResponseEntity.ok(Map.of("success", true, "message", "Mot de passe mis à jour avec succès."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
