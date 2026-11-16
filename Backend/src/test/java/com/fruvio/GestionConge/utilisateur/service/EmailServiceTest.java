package com.fruvio.GestionConge.utilisateur.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void welcomeEmailNeverExposesAutomaticallyGeneratedPassword() throws Exception {
        MimeMessage message = prepareMessage();
        EmailService service = new EmailService(mailSender, "noreply@example.com", "https://gestion.example.com/");

        service.sendWelcomeEmail("agent@example.com", "2026100", "Generated-Secret-42!", true, "one-time-token");

        String body = extractText(message);
        assertThat(body).contains("2026100", "one-time-token", "24 heures");
        assertThat(body).doesNotContain("Generated-Secret-42!");
        verify(mailSender).send(message);
    }

    @Test
    void welcomeEmailNeverTransmitsManuallyDefinedPassword() throws Exception {
        MimeMessage message = prepareMessage();
        EmailService service = new EmailService(mailSender, "noreply@example.com", "https://gestion.example.com");

        service.sendWelcomeEmail("agent@example.com", "2026101", "Manual-Secret-84!", false, null);

        String body = extractText(message);
        assertThat(body).contains("2026101", "n'est pas transmis par e-mail");
        assertThat(body).doesNotContain("Manual-Secret-84!");
        verify(mailSender).send(message);
    }

    private MimeMessage prepareMessage() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doNothing().when(mailSender).send(message);
        return message;
    }

    private String extractText(Part part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                result.append(extractText(multipart.getBodyPart(i))).append('\n');
            }
            return result.toString();
        }
        return "";
    }
}
