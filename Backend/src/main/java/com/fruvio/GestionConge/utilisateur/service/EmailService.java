package com.fruvio.GestionConge.utilisateur.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender, @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (!fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    public void sendResetPasswordEmail(String toEmail, String token) {
        String resetUrl = "http://localhost:3000/reset-password?token=" + token;
        String subject = "Réinitialisation de votre mot de passe";
        String text = "Bonjour,\n\n"
                + "Vous avez demandé la réinitialisation de votre mot de passe.\n"
                + "Veuillez cliquer sur le lien ci-dessous pour créer un nouveau mot de passe :\n\n"
                + resetUrl + "\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet e-mail.";

        sendEmail(toEmail, subject, text);
    }

    // NOUVELLE MÉTHODE : Envoi des identifiants au nouvel utilisateur
    public void sendWelcomeEmail(String toEmail, String matricule, String rawPassword) {
        String subject = "Création de votre compte - Gestion des Congés";
        String text = "Bonjour,\n\n"
                + "Votre compte sur la plateforme de Gestion des Congés a été créé avec succès.\n\n"
                + "Voici vos identifiants de connexion :\n"
                + " - Matricule / Identifiant : " + matricule + "\n"
                + " - Mot de passe temporaire : " + rawPassword + "\n\n"
                + "Veuillez vous connecter à l'application et modifier votre mot de passe dès que possible.\n\n"
                + "Cordialement,\n"
                + "L'équipe RH";

        sendEmail(toEmail, subject, text);
    }

    public void sendAccountDeletedEmail(String toEmail, String matricule) {
        String subject = "Suppression de votre compte - Gestion des Congés";
        String text = "Bonjour,\n\n"
                + "Votre compte associé au matricule " + matricule
                + " a été supprimé de la plateforme de Gestion des Congés.\n\n"
                + "Cordialement,\n"
                + "L'équipe RH";

        sendEmail(toEmail, subject, text);
    }
}