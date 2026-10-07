package com.fruvio.GestionConge.utilisateur.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fruvio.GestionConge.utilisateur.dto.ForgotPasswordRequest;
import com.fruvio.GestionConge.utilisateur.dto.ResetPasswordRequest;
import com.fruvio.GestionConge.utilisateur.service.PasswordResetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @Valid @RequestBody(required = false) ForgotPasswordRequest body,
            @RequestParam(required = false) String email) {

        String targetEmail = null;

        if (body != null && body.getEmail() != null) {
            targetEmail = body.getEmail();
        } else if (email != null) {
            targetEmail = email;
        }

        if (targetEmail != null && !targetEmail.trim().isEmpty()) {
            passwordResetService.createPasswordResetToken(targetEmail.trim());
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Si un compte correspond à cette adresse, un lien de réinitialisation sera envoyé."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @Valid @RequestBody(required = false) ResetPasswordRequest body,
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String newPassword) {

        String resetToken = body != null && body.getToken() != null ? body.getToken() : token;
        String pass = body != null && body.getNewPassword() != null ? body.getNewPassword() : newPassword;

        if (resetToken == null || resetToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Le jeton de réinitialisation est obligatoire."
            ));
        }

        if (pass == null || pass.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Le nouveau mot de passe est obligatoire."
            ));
        }

        try {
            passwordResetService.resetPassword(resetToken.trim(), pass.trim());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mot de passe mis à jour avec succès."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}