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
        // E-MAIL DE BIENVENUE
        // =========================================================

        public void sendWelcomeEmail(String toEmail, String matricule, String rawPassword) {
                sendWelcomeEmail(toEmail, matricule, rawPassword, false, null);
        }

        /**
         * Envoie les identifiants en indiquant explicitement l'origine du mot de passe.
         * Le lien d'activation est réservé aux mots de passe générés par le système.
         */
        public void sendWelcomeEmail(String toEmail, String matricule, String rawPassword,
                        boolean generatedAutomatically, String token) {

                if (toEmail == null || toEmail.isBlank()) {
                        throw new IllegalArgumentException("L'adresse e-mail est obligatoire.");
                }
                if (matricule == null || matricule.isBlank()) {
                        throw new IllegalArgumentException("Le matricule est obligatoire pour l'e-mail de bienvenue.");
                }
                if (rawPassword == null || rawPassword.isBlank()) {
                        throw new IllegalArgumentException("Le mot de passe est obligatoire.");
                }

                String subject = "Création de votre compte - Gestion des Congés";
                StringBuilder text = new StringBuilder("Bonjour,\\n\\n");
                if (generatedAutomatically) {
                        if (token == null || token.isBlank()) {
                                throw new IllegalArgumentException("Le lien de première activation est obligatoire.");
                        }
                        String resetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                                        .path("/Components/mdpReset")
                                        .queryParam("token", token)
                                        .build().encode().toUriString();

                        text.append("IMPORTANT : CE MOT DE PASSE A ÉTÉ GÉNÉRÉ AUTOMATIQUEMENT PAR LE SYSTÈME.\\n")
                                        .append("Pour le remplacer immédiatement par votre mot de passe personnel, utilisez ce lien sécurisé :\\n")
                                        .append(resetUrl).append("\\n")
                                        .append("Ce lien est valable 24 heures et ne peut être utilisé qu'une seule fois.\\n\\n");
                } else {
                        text.append("INFORMATION : CE MOT DE PASSE A ÉTÉ DÉFINI MANUELLEMENT PAR L'ADMINISTRATEUR.\\n")
                                        .append("Il ne s'agit pas d'un mot de passe généré automatiquement.\\n\\n");
                }

                text.append("Votre compte sur la plateforme de Gestion des Congés a été créé avec succès.\\n\\n")
                                .append("Vos identifiants :\\n")
                                .append(" - Matricule / Identifiant : ").append(matricule).append("\\n")
                                .append(" - Mot de passe initial : ").append(rawPassword).append("\\n\\n");

                if (generatedAutomatically) {
                        text.append("Nous vous recommandons de définir votre mot de passe personnel dès maintenant en utilisant le lien ci-dessus.\\n\\n");
                } else {
                        text.append("Conservez ce mot de passe en sécurité. Vous pourrez le modifier depuis votre espace personnel.\\n\\n");
                }
                text.append("Cordialement,\\nL'équipe RH");

                sendEmail(toEmail, subject, text.toString());
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