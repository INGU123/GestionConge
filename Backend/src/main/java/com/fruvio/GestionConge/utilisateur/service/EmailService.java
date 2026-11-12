package com.fruvio.GestionConge.utilisateur.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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


        /**
         * Envoie un e-mail HTML avec une version texte de secours.
         */
        private void sendHtmlEmail(@NonNull String to, @NonNull String subject,
                        @NonNull String plainText, @NonNull String htmlText) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(
                                        message, true, StandardCharsets.UTF_8.name());
                        helper.setFrom(fromAddress, fromName);
                        helper.setTo(Objects.requireNonNull(to.trim()));
                        helper.setSubject(subject);
                        helper.setText(plainText, htmlText);
                        mailSender.send(message);
                        log.info("E-mail HTML envoyé avec succès vers {} (objet: {})", to, subject);
                } catch (Exception e) {
                        log.error("Échec de l'envoi e-mail HTML vers {} (objet: {}). Type: {}, message: {}",
                                        to, subject, e.getClass().getSimpleName(), e.getMessage(), e);
                        throw new IllegalStateException("Impossible d'envoyer l'e-mail.", e);
                }
        }

        private String escapeHtml(String value) {
                if (value == null) return "";
                return value.replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;")
                                .replace("'", "&#39;");
        }

        // =========================================================
        // E-MAIL DE BIENVENUE
        // =========================================================

        public void sendWelcomeEmail(String toEmail, String matricule, String rawPassword) {
                sendWelcomeEmail(toEmail, matricule, rawPassword, false, null);
        }

        /**
         * Envoie les identifiants du compte avec un lien d'activation clairement identifiable.
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
                StringBuilder plainText = new StringBuilder("Bonjour,\n\n");
                String actionHtml = "";

                if (generatedAutomatically) {
                        if (token == null || token.isBlank()) {
                                throw new IllegalArgumentException("Le lien de première activation est obligatoire.");
                        }

                        String resetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                                        .path("/Components/mdpReset")
                                        .queryParam("token", token)
                                        .build().encode().toUriString();

                        plainText.append("IMPORTANT : votre mot de passe initial a été généré automatiquement.\n")
                                        .append("Pour choisir votre mot de passe personnel, cliquez sur ce lien sécurisé :\n")
                                        .append(resetUrl).append("\n")
                                        .append("Ce lien est valable 24 heures et ne peut être utilisé qu'une seule fois.\n\n");

                        String safeUrl = escapeHtml(resetUrl);
                        actionHtml = """
                                        <div style="margin:24px 0;padding:22px;background:#eff6ff;border:1px solid #bfdbfe;border-radius:12px;text-align:center;">
                                          <p style="margin:0 0 14px;color:#1e3a8a;font-size:15px;font-weight:700;">Action requise : choisissez votre mot de passe personnel</p>
                                          <a href="{{RESET_URL}}" style="display:inline-block;padding:14px 22px;background:#0757a7;color:#ffffff;text-decoration:none;font-size:15px;font-weight:700;border-radius:9px;">DÉFINIR MON MOT DE PASSE</a>
                                          <p style="margin:16px 0 6px;color:#334155;font-size:13px;">Si le bouton ne fonctionne pas, copiez ce lien dans la barre d'adresse de votre navigateur :</p>
                                          <p style="margin:0;word-break:break-all;font-size:12px;"><a href="{{RESET_URL}}" style="color:#0757a7;">{{RESET_URL}}</a></p>
                                          <p style="margin:14px 0 0;color:#64748b;font-size:12px;">Ce lien expire dans 24 heures et ne peut être utilisé qu'une seule fois.</p>
                                        </div>
                                        """.replace("{{RESET_URL}}", safeUrl);
                } else {
                        plainText.append("INFORMATION : ce mot de passe a été défini manuellement par l'administrateur.\n\n");
                        actionHtml = """
                                        <div style="margin:20px 0;padding:16px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;color:#334155;">
                                          Ce mot de passe a été défini manuellement par l'administrateur.
                                        </div>
                                        """;
                }

                plainText.append("Votre compte sur la plateforme de Gestion des Congés a été créé avec succès.\n\n")
                                .append("Vos identifiants :\n")
                                .append("Matricule / Identifiant : ").append(matricule).append("\n")
                                .append("Mot de passe initial : ").append(rawPassword).append("\n\n");

                if (generatedAutomatically) {
                        plainText.append("Nous vous recommandons de définir votre mot de passe personnel dès maintenant.\n\n");
                } else {
                        plainText.append("Conservez ce mot de passe en sécurité. Vous pourrez le modifier depuis votre espace personnel.\n\n");
                }
                plainText.append("Cordialement,\nL'équipe RH");

                String html = """
                                <!doctype html>
                                <html lang="fr">
                                  <body style="margin:0;padding:28px 12px;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;color:#1e293b;">
                                    <div style="max-width:620px;margin:0 auto;padding:30px;background:#ffffff;border:1px solid #e2e8f0;border-radius:16px;">
                                      <div style="padding-bottom:18px;border-bottom:3px solid #0757a7;">
                                        <p style="margin:0;color:#0757a7;font-size:12px;font-weight:700;letter-spacing:1px;">SPAT · GESTION DES CONGÉS</p>
                                        <h1 style="margin:12px 0 0;font-size:22px;color:#0f172a;">Bienvenue sur GestionConge</h1>
                                      </div>
                                      <p style="margin:24px 0 8px;">Bonjour,</p>
                                      <p style="margin:0;line-height:1.6;">Votre compte sur la plateforme de Gestion des Congés a été créé avec succès.</p>
                                      {{ACTION}}
                                      <div style="padding:18px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;">
                                        <p style="margin:0 0 12px;font-weight:700;color:#0f172a;">Vos identifiants de connexion</p>
                                        <p style="margin:7px 0;">Matricule / Identifiant : <strong>{{MATRICULE}}</strong></p>
                                        <p style="margin:7px 0;">Mot de passe initial : <strong style="font-family:monospace;">{{PASSWORD}}</strong></p>
                                      </div>
                                      <p style="margin:22px 0 0;line-height:1.6;">Cordialement,<br><strong>L'équipe RH</strong></p>
                                    </div>
                                  </body>
                                </html>
                                """.replace("{{ACTION}}", actionHtml)
                                        .replace("{{MATRICULE}}", escapeHtml(matricule))
                                        .replace("{{PASSWORD}}", escapeHtml(rawPassword));

                sendHtmlEmail(toEmail, subject, plainText.toString(), html);
        }

        // =========================================================
        // NOTIFICATION D'ARCHIVAGE DE COMPTE
        // =========================================================

        public void sendAccountArchivedEmail(String toEmail, String matricule, String nom, String prenom,
                        LocalDateTime archivedAt, String reason) {

                if (toEmail == null || toEmail.isBlank()) {
                        throw new IllegalArgumentException("L'adresse e-mail est obligatoire.");
                }
                if (matricule == null || matricule.isBlank()) {
                        throw new IllegalArgumentException("Le matricule est obligatoire pour la notification.");
                }
                if (archivedAt == null) {
                        throw new IllegalArgumentException("La date d'archivage est obligatoire.");
                }

                String subject = "Notification de désactivation de votre compte - SPAT";
                String displayName = ((prenom == null ? "" : prenom.trim()) + " "
                        + (nom == null ? "" : nom.trim())).trim();
                StringBuilder text = new StringBuilder("Bonjour")
                        .append(displayName.isEmpty() ? "" : " " + displayName)
                        .append(",\n\n")
                        .append("Nous vous informons que votre compte GestionConge de la SPAT a été archivé par un administrateur.\n\n")
                        .append("Matricule : ").append(matricule).append("\n")
                        .append("Date de désactivation : ")
                        .append(archivedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")))
                        .append("\n");
                if (reason != null && !reason.isBlank()) {
                        text.append("Motif : ").append(reason.trim()).append("\n");
                }
                text.append("\nVotre accès à la plateforme est désormais suspendu. ")
                        .append("Vos anciennes demandes de congé et vos historiques administratifs restent conservés ")
                        .append("dans le système.\n\n")
                        .append("Pour toute information complémentaire, veuillez contacter l'administration de la SPAT.\n\n")
                        .append("Cordialement,\nAdministration - SPAT");

                sendEmail(toEmail, subject, text.toString());
        }
}