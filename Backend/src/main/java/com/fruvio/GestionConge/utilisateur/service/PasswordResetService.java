package com.fruvio.GestionConge.utilisateur.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UtilisateurRepository utilisateurRepository, EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Génère un token de réinitialisation sécurisé et envoie un e-mail sans révéler
     * l'existence du compte.
     */
    @Transactional
    public void createPasswordResetToken(String email) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }

        Optional<Utilisateur> userOpt = utilisateurRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            log.info("Demande de réinitialisation de mot de passe pour un e-mail inconnu.");
            return;
        }

        Utilisateur user = userOpt.get();
        if (!user.isActif()) {
            log.warn("Demande de réinitialisation ignorée pour compte inactif : {}", user.getEmail());
            return;
        }

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setTokenExpiration(LocalDateTime.now().plusMinutes(30));
        utilisateurRepository.save(user);

        try {
            emailService.sendResetPasswordEmail(user.getEmail(), token);
        } catch (Exception e) {
            user.setResetToken(null);
            user.setTokenExpiration(null);
            utilisateurRepository.save(user);
            log.error("Échec d'envoi du mail de réinitialisation à {} : {}", user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Réinitialise le mot de passe si le token est valide, non expiré, et
     * l'invalide immédiatement.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Le jeton de réinitialisation est obligatoire.");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit comporter au moins 6 caractères.");
        }

        Utilisateur user = utilisateurRepository.findByResetToken(token.trim())
                .orElseThrow(() -> new IllegalArgumentException("Jeton de réinitialisation invalide ou expiré."));

        if (user.getTokenExpiration() == null || user.getTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Jeton de réinitialisation invalide ou expiré.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setTokenExpiration(null);
        utilisateurRepository.save(user);
    }
}
