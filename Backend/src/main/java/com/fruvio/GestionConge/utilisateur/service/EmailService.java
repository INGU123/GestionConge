package com.fruvio.GestionConge.utilisateur.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

@Service
public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);

    private final Resend resend;

    private final String fromAddress;

    /**
     * Initialise le client Resend.
     *
     * Les valeurs sont récupérées depuis application.properties
     * et donc depuis les variables du fichier .env.
     */
    public EmailService(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:}") String fromAddress) {

        if (apiKey == null || apiKey.isBlank()) {

            throw new IllegalStateException(
                    "RESEND_API_KEY est manquante. "
                            + "Configurez la clé API Resend dans le fichier .env."
            );
        }

        if (fromAddress == null || fromAddress.isBlank()) {

            throw new IllegalStateException(
                    "RESEND_FROM est manquante. "
                            + "Configurez l'adresse expéditeur Resend dans le fichier .env."
            );
        }

        this.resend = new Resend(apiKey);

        this.fromAddress =
                fromAddress.trim();
    }

    // =========================================================
    // ENVOI GENERIQUE
    // =========================================================

    /**
     * Envoie un e-mail texte via Resend.
     *
     * Cette méthode conserve la même signature que l'ancien
     * EmailService SMTP afin de ne pas obliger les autres
     * services/controllers à changer leur code.
     */
    public void sendEmail(
            String to,
            String subject,
            String text) {

        if (to == null || to.isBlank()) {

            throw new IllegalArgumentException(
                    "L'adresse e-mail du destinataire est obligatoire."
            );
        }

        if (subject == null || subject.isBlank()) {

            throw new IllegalArgumentException(
                    "L'objet de l'e-mail est obligatoire."
            );
        }

        String emailText =
                text != null
                        ? text
                        : "";

        try {

            log.info(
                    "Envoi Resend vers {} (objet: {}) depuis {}",
                    to,
                    subject,
                    fromAddress
            );

            CreateEmailOptions params =
                    CreateEmailOptions.builder()
                            .from(fromAddress)
                            .to(to.trim())
                            .subject(subject)
                            .text(emailText)
                            .build();

            CreateEmailResponse response =
                    resend.emails().send(params);

            if (response == null) {

                throw new IllegalStateException(
                        "Resend n'a retourné aucune réponse."
                );
            }

            log.info(
                    "E-mail envoyé avec succès via Resend vers {} (objet: {})",
                    to,
                    subject
            );

            if (response.getId() != null) {

                log.debug(
                        "Resend email ID: {}",
                        response.getId()
                );
            }

        } catch (IllegalArgumentException e) {

            /*
             * Les erreurs de validation ne sont pas transformées.
             */
            throw e;

        } catch (Exception e) {

            /*
             * Le SDK Resend peut retourner différentes exceptions
             * selon le type d'erreur rencontré :
             *
             * - clé API invalide
             * - adresse expéditeur non autorisée
             * - destinataire invalide
             * - problème réseau
             * - erreur API Resend
             *
             * On transforme tout cela en IllegalStateException
             * afin que le reste de l'application n'ait aucune
             * dépendance au SDK Resend.
             */
            log.error(
                    "Échec de l'envoi Resend vers {} (objet: {}). Type: {}. Message: {}",
                    to,
                    subject,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );

            throw new IllegalStateException(
                    "Impossible d'envoyer l'e-mail via Resend.",
                    e
            );
        }
    }

    // =========================================================
    // REINITIALISATION DU MOT DE PASSE
    // =========================================================

    public void sendResetPasswordEmail(
            String toEmail,
            String token) {

        if (toEmail == null
                || toEmail.isBlank()) {

            throw new IllegalArgumentException(
                    "L'adresse e-mail est obligatoire."
            );
        }

        if (token == null
                || token.isBlank()) {

            throw new IllegalArgumentException(
                    "Le token de réinitialisation est obligatoire."
            );
        }

        String resetUrl =
                "http://localhost:3000/Components/mdpReset?token="
                        + token;

        String subject =
                "Réinitialisation de votre mot de passe";

        String text =
                "Bonjour,\n\n"
                + "Vous avez demandé la réinitialisation de votre mot de passe.\n\n"
                + "Veuillez cliquer sur le lien ci-dessous pour créer un nouveau mot de passe :\n\n"
                + resetUrl
                + "\n\n"
                + "Ce lien est temporaire et doit être utilisé dans les 30 minutes.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet e-mail.\n\n"
                + "Cordialement,\n"
                + "L'équipe RH";

        sendEmail(
                toEmail,
                subject,
                text
        );
    }

    // =========================================================
    // E-MAIL DE BIENVENUE
    // =========================================================

    public void sendWelcomeEmail(
            String toEmail,
            String matricule,
            String rawPassword) {

        if (toEmail == null
                || toEmail.isBlank()) {

            throw new IllegalArgumentException(
                    "L'adresse e-mail est obligatoire."
            );
        }

        if (matricule == null
                || matricule.isBlank()) {

            throw new IllegalArgumentException(
                    "Le matricule est obligatoire pour l'e-mail de bienvenue."
            );
        }

        if (rawPassword == null
                || rawPassword.isBlank()) {

            throw new IllegalArgumentException(
                    "Le mot de passe temporaire est obligatoire."
            );
        }

        String subject =
                "Création de votre compte - Gestion des Congés";

        String text =
                "Bonjour,\n\n"
                + "Votre compte sur la plateforme de Gestion des Congés "
                + "a été créé avec succès.\n\n"
                + "Voici vos identifiants de connexion :\n"
                + " - Matricule / Identifiant : "
                + matricule
                + "\n"
                + " - Mot de passe temporaire : "
                + rawPassword
                + "\n\n"
                + "Veuillez vous connecter à l'application et modifier "
                + "votre mot de passe dès que possible.\n\n"
                + "Cordialement,\n"
                + "L'équipe RH";

        sendEmail(
                toEmail,
                subject,
                text
        );
    }

    // =========================================================
    // E-MAIL DE SUPPRESSION DE COMPTE
    // =========================================================

    public void sendAccountDeletedEmail(
            String toEmail,
            String matricule) {

        if (toEmail == null
                || toEmail.isBlank()) {

            throw new IllegalArgumentException(
                    "L'adresse e-mail est obligatoire."
            );
        }

        if (matricule == null
                || matricule.isBlank()) {

            throw new IllegalArgumentException(
                    "Le matricule est obligatoire pour l'e-mail de suppression."
            );
        }

        String subject =
                "Suppression de votre compte - Gestion des Congés";

        String text =
                "Bonjour,\n\n"
                + "Votre compte associé au matricule "
                + matricule
                + " a été supprimé de la plateforme de Gestion des Congés.\n\n"
                + "Cordialement,\n"
                + "L'équipe RH";

        sendEmail(
                toEmail,
                subject,
                text
        );
    }
}