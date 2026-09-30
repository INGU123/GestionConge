package com.fruvio.GestionConge.utilisateur.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fruvio.GestionConge.utilisateur.config.CustomUserDetails;
import com.fruvio.GestionConge.utilisateur.config.JwtUtil;
import com.fruvio.GestionConge.utilisateur.dto.ChangePasswordRequest;
import com.fruvio.GestionConge.utilisateur.dto.ForgotPasswordRequest;
import com.fruvio.GestionConge.utilisateur.dto.LoginRequest;
import com.fruvio.GestionConge.utilisateur.dto.LoginResponse;
import com.fruvio.GestionConge.utilisateur.dto.ResetPasswordRequest;
import com.fruvio.GestionConge.utilisateur.dto.UtilisateurCreateRequest;
import com.fruvio.GestionConge.utilisateur.dto.UtilisateurResponse;
import com.fruvio.GestionConge.utilisateur.dto.UtilisateurUpdateRequest;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.service.UtilisateurService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/utilisateur")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final JwtUtil jwtUtil;

    public UtilisateurController(UtilisateurService utilisateurService, JwtUtil jwtUtil) {
        this.utilisateurService = utilisateurService;
        this.jwtUtil = jwtUtil;
    }

    // =========================================================
    // LOGIN (Matricule + Mot de passe + JWT)
    // =========================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String identifier = request.getIdentifier();
        String password = request.getPassword();

        if (identifier == null || identifier.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Le matricule et le mot de passe sont obligatoires.");
        }

        Utilisateur utilisateur = utilisateurService.login(identifier, password);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Matricule ou mot de passe incorrect.");
        }

        String roleName = utilisateur.getRole() != null ? utilisateur.getRole().name() : Role.EMPLOYE.name();
        String token = jwtUtil.generateToken(utilisateur.getMatricule(), roleName);

        UtilisateurResponse userResponse = UtilisateurResponse.fromEntity(utilisateur);

        LoginResponse loginResponse = LoginResponse.builder().token(token).utilisateur(userResponse).build();

        return ResponseEntity.ok(loginResponse);
    }

    // =========================================================
    // MOT DE PASSE OUBLIE
    // =========================================================

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody(required = false) ForgotPasswordRequest body,
            @RequestParam(value = "email", required = false) String paramEmail) {

        String email = null;
        if (body != null && body.getEmail() != null) {
            email = body.getEmail();
        } else if (paramEmail != null) {
            email = paramEmail;
        }

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("L'adresse e-mail est obligatoire.");
        }

        utilisateurService.forgotPassword(email.trim());

        // Réponse générique constante pour prévenir l'énumération des utilisateurs
        return ResponseEntity.ok("Si un compte correspond à cette adresse, un lien de réinitialisation sera envoyé.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody(required = false) ResetPasswordRequest body,
            @RequestParam(value = "token", required = false) String paramToken,
            @RequestParam(value = "newPassword", required = false) String paramNewPassword) {

        String resetToken = body != null && body.getToken() != null ? body.getToken() : paramToken;
        String newPassword = body != null && body.getNewPassword() != null ? body.getNewPassword() : paramNewPassword;

        if (resetToken == null || resetToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Le jeton de réinitialisation est obligatoire.");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Le nouveau mot de passe est obligatoire.");
        }

        try {
            utilisateurService.resetPassword(resetToken.trim(), newPassword.trim());
            return ResponseEntity.ok("Mot de passe réinitialisé avec succès.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================
    // CREATION D'UN COLLABORATEUR (ADMIN uniquement)
    // =========================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody UtilisateurCreateRequest request) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        Utilisateur saved = utilisateurService.createUtilisateur(request, currentUser);

        UtilisateurResponse response = UtilisateurResponse.fromEntity(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("utilisateur", response, "message",
                "Collaborateur créé avec succès. Ses identifiants ont été envoyés par e-mail."));
    }

    // =========================================================
    // MISE A JOUR DU COMPTE
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @PutMapping("/update")
    public ResponseEntity<?> update(@Valid @RequestBody UtilisateurUpdateRequest request) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        Utilisateur updated = utilisateurService.updateUtilisateur(request, currentUser);

        return ResponseEntity.ok(UtilisateurResponse.fromEntity(updated));
    }

    // =========================================================
    // CHANGEMENT DE MOT DE PASSE PAR L'UTILISATEUR
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentification requise.");
        }

        utilisateurService.changePassword(currentUser.getId(), request, currentUser);
        return ResponseEntity.ok("Mot de passe modifié avec succès.");
    }

    // =========================================================
    // LISTE DES COLLABORATEURS (Filtrée selon rôle)
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/all")
    public ResponseEntity<List<UtilisateurResponse>> getAll() {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        List<Utilisateur> list = utilisateurService.getAllUtilisateurs(currentUser);

        List<UtilisateurResponse> responseList = list.stream().map(UtilisateurResponse::fromEntity).toList();

        return ResponseEntity.ok(responseList);
    }

    // =========================================================
    // CONSULTATION D'UN COLLABORATEUR PAR ID
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        Utilisateur user = utilisateurService.getUtilisateurById(id, currentUser);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé.");
        }

        return ResponseEntity.ok(UtilisateurResponse.fromEntity(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        if (!utilisateurService.deleteUtilisateur(id, currentUser)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Collaborateur non trouvé."));
        }

        return ResponseEntity.ok(Map.of("message", "Collaborateur supprimé avec succès."));
    }

    // =========================================================
    // UTILITAIRES DE CONTEXTE DE SECURITE
    // =========================================================

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