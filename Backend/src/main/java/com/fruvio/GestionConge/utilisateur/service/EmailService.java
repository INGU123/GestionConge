package com.fruvio.GestionConge.utilisateur.service;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

        private static final Logger log = LoggerFactory.getLogger(EmailService.class);

        private final JavaMailSender mailSender;
        @NonNull
        private final String fromAddress;
        @NonNull
        private final String fromName;
        @NonNull
        private final String frontendUrl;

        public EmailService(JavaMailSender mailSender, @Value("${mail.from.email}") @NonNull String fromAddress,
                        @Value("${app.frontend.url:http://localhost:3000}") @NonNull String frontendUrl) {

                if (fromAddress == null || fromAddress.isBlank()) {
                        throw new IllegalStateException("MAIL_FROM_EMAIL est manquante.");
                }

                this.mailSender = mailSender;
                this.fromAddress = Objects.requireNonNull(fromAddress.trim());
                this.fromName = "Fruvio";
                if (frontendUrl == null || frontendUrl.isBlank()) {
                        throw new IllegalStateException("FRONTEND_URL est manquante.");
                }
                this.frontendUrl = Objects.requireNonNull(frontendUrl.trim().replaceAll("/+$", ""));
        }

        // =========================================================
        // ENVOI GENERIQUE
        // =========================================================

        public void sendEmail(@NonNull String to, @NonNull String subject, @Nullable String text) {

                if (to == null || to.isBlank()) {
                        throw new IllegalArgumentException("L'adresse e-mail du destinataire est obligatoire.");
                }

                if (subject == null || subject.isBlank()) {
                        throw new IllegalArgumentException("L'objet de l'e-mail est obligatoire.");
                }

                String emailText = text != null ? text : "";

                try {

                        log.info("Envoi e-mail vers {} (objet: {}) depuis {}", to, subject, fromAddress);

                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
                        helper.setFrom(fromAddress, fromName);
                        helper.setTo(Objects.requireNonNull(to.trim()));
                        helper.setSubject(subject);
                        helper.setText(emailText, false);

                        mailSender.send(message);

                        log.info("E-mail envoyé avec succès vers {} (objet: {})", to, subject);

                } catch (IllegalArgumentException e) {

                        throw e;

                } catch (Exception e) {

                        log.error("Échec de l'envoi e-mail vers {} (objet: {}). Type: {}, message: {}", to, subject,
                                        e.getClass().getSimpleName(), e.getMessage(), e);

                        throw new IllegalStateException("Impossible d'envoyer l'e-mail.", e);
                }
        }

        // =========================================================
        // REINITIALISATION DU MOT DE PASSE
        // =========================================================

        public void sendResetPasswordEmail(String toEmail, String token) {

                if (toEmail == null || toEmail.isBlank()) {
                        throw new IllegalArgumentException("L'adresse e-mail est obligatoire.");
                }

                if (token == null || token.isBlank()) {
                        throw new IllegalArgumentException("Le token de réinitialisation est obligatoire.");
                }

                String resetUrl = UriComponentsBuilder.fromUriString(frontendUrl).path("/Components/mdpReset")
                                .queryParam("token", token).build().encode().toUriString();

                String subject = "Réinitialisation de votre mot de passe";

                String text = "Bonjour,\n\n" + "Vous avez demandé la réinitialisation de votre mot de passe.\n\n"
                                + "Veuillez cliquer sur le lien ci-dessous pour créer un nouveau mot de passe :\n\n"
                                + resetUrl + "\n\n"
                                + "Ce lien est temporaire et doit être utilisé dans les 30 minutes.\n\n"
                                + "Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet e-mail.\n\n"
                                + "Cordialement,\n" + "L'équipe RH";

                sendEmail(toEmail, subject, text);
        }

        // =========================================================
        // E-MAIL DE BIENVENUE
        // =========================================================

        public void sendWelcomeEmail(String toEmail, String matricule, String rawPassword) {

                if (toEmail == null || toEmail.isBlank()) {
                        throw new IllegalArgumentException("L'adresse e-mail est obligatoire.");
                }

                if (matricule == null || matricule.isBlank()) {
                        throw new IllegalArgumentException("Le matricule est obligatoire pour l'e-mail de bienvenue.");
                }

                if (rawPassword == null || rawPassword.isBlank()) {
                        throw new IllegalArgumentException("Le mot de passe temporaire est obligatoire.");
                }

                String subject = "Création de votre compte - Gestion des Congés";

                String text = "Bonjour,\n\n" + "Votre compte sur la plateforme de Gestion des Congés "
                                + "a été créé avec succès.\n\n" + "Voici vos identifiants de connexion :\n"
                                + " - Matricule / Identifiant : " + matricule + "\n" + " - Mot de passe temporaire : "
                                + rawPassword + "\n\n" + "Veuillez vous connecter à l'application et modifier "
                                + "votre mot de passe dès que possible.\n\n" + "Cordialement,\n" + "L'équipe RH";

                sendEmail(toEmail, subject, text);
        }

        // =========================================================
        // E-MAIL DE SUPPRESSION DE COMPTE
        // =========================================================

        public void sendAccountDeletedEmail(String toEmail, String matricule) {

                if (toEmail == null || toEmail.isBlank()) {
                        throw new IllegalArgumentException("L'adresse e-mail est obligatoire.");
                }

                if (matricule == null || matricule.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Le matricule est obligatoire pour l'e-mail de suppression.");
                }

                String subject = "Suppression de votre compte - Gestion des Congés";

                String text = "Bonjour,\n\n" + "Votre compte associé au matricule " + matricule
                                + " a été supprimé de la plateforme de Gestion des Congés.\n\n" + "Cordialement,\n"
                                + "L'équipe RH";

                sendEmail(toEmail, subject, text);
        }
}