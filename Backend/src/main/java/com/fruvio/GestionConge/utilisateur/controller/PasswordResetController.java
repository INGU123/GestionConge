package com.fruvio.GestionConge.utilisateur.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.fruvio.GestionConge.notifications_conge.service.Notification_congeService;
import com.fruvio.GestionConge.utilisateur.dto.CompletePasswordResetRequest;
import com.fruvio.GestionConge.utilisateur.dto.ForgotPasswordRequest;
import com.fruvio.GestionConge.utilisateur.entity.PasswordResetRequestEntity;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.PasswordResetRequestRepository;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;
import com.fruvio.GestionConge.utilisateur.service.EmailService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordResetRequestRepository requestRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final Notification_congeService notificationService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl = "http://localhost:3000";

    @Autowired
    public PasswordResetController(
            UtilisateurRepository utilisateurRepository,
            PasswordResetRequestRepository requestRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            Notification_congeService notificationService) {
        this.utilisateurRepository = utilisateurRepository;
        this.requestRepository = requestRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    // Compatibilité avec les tests/unités qui construisent encore le contrôleur à quatre dépendances.
    public PasswordResetController(
            UtilisateurRepository utilisateurRepository,
            PasswordResetRequestRepository requestRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this(utilisateurRepository, requestRepository, emailService, passwordEncoder, null);
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
                String adminMessage = "Une demande de réinitialisation de mot de passe nécessite votre validation.\n"
                        + "Agent : " + safe(user.getPrenom()) + " " + safe(user.getNom())
                        + "\nMatricule : " + safe(user.getMatricule())
                        + "\nDate : " + request.getDateDemande()
                        + "\n\nOuvrir GestionConge : " + frontendUrl() + "/dashboard/reinitialisations";

                for (Utilisateur admin : admins) {
                    try {
                        emailService.sendEmail(admin.getEmail(), subject,
                                "Bonjour Administrateur,\n\n" + adminMessage + "\n");
                    } catch (Exception ex) {
                        log.error("La demande {} est enregistrée, mais l'e-mail d'alerte admin a échoué.", request.getId(), ex);
                    }
                    if (notificationService != null && admin.getId() != null) {
                        try {
                            notificationService.creerNotification(admin.getId(),
                                    "Demande de réinitialisation de mot de passe : "
                                            + safe(user.getPrenom()) + " " + safe(user.getNom())
                                            + " (" + safe(user.getMatricule()) + ").",
                                    "INFO", "/dashboard/reinitialisations");
                        } catch (Exception ex) {
                            log.error("Impossible de créer la notification admin pour la demande {}.", request.getId(), ex);
                        }
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
                        "matricule", safe(r.getUtilisateur().getMatricule()),
                        "nom", safe(r.getUtilisateur().getNom()),
                        "prenom", safe(r.getUtilisateur().getPrenom()),
                        "email", safe(r.getUtilisateur().getEmail()),
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

        String resetLink = frontendUrl() + "/Components/mdpReset?matricule="
                + URLEncoder.encode(safe(user.getMatricule()), StandardCharsets.UTF_8);
        String emailText = "Bonjour " + safe(user.getPrenom()) + " " + safe(user.getNom()) + ",\n\n"
                + "Votre demande de réinitialisation de mot de passe a été ACCEPTÉE.\n\n"
                + "Nom : " + safe(user.getNom()) + "\nPrénom : " + safe(user.getPrenom())
                + "\nMatricule : " + safe(user.getMatricule())
                + "\nMot de passe temporaire : " + temporaryPassword
                + "\n\nConnectez-vous à l'interface de réinitialisation pour remplacer ce mot de passe temporaire par un mot de passe personnel :\n"
                + resetLink + "\n\nPour votre sécurité, changez ce mot de passe temporaire dès que possible.\n\nService RH - SPAT";

        try {
            emailService.sendEmail(user.getEmail(), "Demande acceptée : réinitialisation de mot de passe - SPAT", emailText);
        } catch (Exception ex) {
            log.error("L'e-mail d'acceptation de la demande {} a échoué ; la transaction doit être annulée.", id, ex);
            throw new IllegalStateException("L'e-mail contenant le mot de passe temporaire n'a pas pu être envoyé.", ex);
        }

        return ResponseEntity.ok(Map.of("success", true,
                "message", "Demande acceptée. Les informations et le lien de changement de mot de passe ont été envoyés à l'agent."));
    }

    @PostMapping("/admin/password-reset-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> rejectRequest(@PathVariable Long id) {
        PasswordResetRequestEntity request = requestRepository.findById(id).orElse(null);
        if (request == null || !"EN_ATTENTE".equals(request.getStatut())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Demande introuvable ou déjà traitée."));
        }

        Utilisateur user = request.getUtilisateur();
        request.setStatut("REFUSEE");
        request.setDateTraitement(LocalDateTime.now());
        requestRepository.save(request);

        String newRequestLink = frontendUrl() + "/Components/mdpForget";
        String emailText = "Bonjour " + safe(user.getPrenom()) + " " + safe(user.getNom()) + ",\n\n"
                + "Votre demande de réinitialisation de mot de passe a été REFUSÉE par l'administration SPAT.\n\n"
                + "Nom : " + safe(user.getNom()) + "\nPrénom : " + safe(user.getPrenom())
                + "\nMatricule : " + safe(user.getMatricule())
                + "\nAucun mot de passe temporaire n'a été généré. Si vous pensez qu'il s'agit d'une erreur, contactez l'administration ou effectuez une nouvelle demande :\n"
                + newRequestLink + "\n\nService RH - SPAT";
        try {
            emailService.sendEmail(user.getEmail(), "Demande refusée : réinitialisation de mot de passe - SPAT", emailText);
        } catch (Exception ex) {
            log.error("L'e-mail de refus de la demande {} a échoué ; la transaction doit être annulée.", id, ex);
            throw new IllegalStateException("L'e-mail de notification du refus n'a pas pu être envoyé.", ex);
        }

        return ResponseEntity.ok(Map.of("success", true,
                "message", "Demande refusée. Un e-mail d'information a été envoyé à l'agent."));
    }

    @PostMapping("/complete-password-reset")
    @Transactional
    public ResponseEntity<Map<String, Object>> completePasswordReset(
            @Valid @RequestBody CompletePasswordResetRequest body) {
        String matricule = body.getMatricule().trim();
        Utilisateur user = utilisateurRepository.findByMatricule(matricule).orElse(null);

        if (user == null || !user.isActif() || user.getId() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "message", "Matricule ou mot de passe temporaire incorrect, ou demande non acceptée."));
        }

        PasswordResetRequestEntity acceptedRequest = requestRepository
                .findFirstByUtilisateur_IdAndStatutOrderByDateTraitementDesc(user.getId(), "ACCEPTEE")
                .orElse(null);
        if (acceptedRequest == null || user.getPassword() == null
                || !passwordEncoder.matches(body.getAncienMotDePasse(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "message", "Matricule ou mot de passe temporaire incorrect, ou demande non acceptée."));
        }

        if (body.getNouveauMotDePasse().equals(body.getAncienMotDePasse())) {
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "message", "Le nouveau mot de passe doit être différent du mot de passe temporaire."));
        }
        if (!body.getNouveauMotDePasse().equals(body.getConfirmation())) {
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "message", "La confirmation ne correspond pas au nouveau mot de passe."));
        }

        user.setPassword(passwordEncoder.encode(body.getNouveauMotDePasse()));
        utilisateurRepository.save(user);
        acceptedRequest.setStatut("TERMINEE");
        acceptedRequest.setDateTraitement(LocalDateTime.now());
        requestRepository.save(acceptedRequest);

        return ResponseEntity.ok(Map.of("success", true,
                "message", "Votre mot de passe a été modifié avec succès. Vous pouvez maintenant vous connecter."));
    }

    private String frontendUrl() {
        String value = frontendUrl == null ? "http://localhost:3000" : frontendUrl.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String generateTemporaryPassword() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#";
        SecureRandom random = new SecureRandom();
        StringBuilder value = new StringBuilder(12);
        for (int i = 0; i < 12; i++) value.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return value.toString();
    }
}
