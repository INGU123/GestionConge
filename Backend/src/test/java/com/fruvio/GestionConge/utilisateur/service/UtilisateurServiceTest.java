package com.fruvio.GestionConge.utilisateur.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Optional;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;

import com.fruvio.GestionConge.demande_conge.repository.DemandeCongeRepository;
import com.fruvio.GestionConge.historique_mouvement.repository.Historique_mouvementRepository;
import com.fruvio.GestionConge.utilisateur.config.JwtUtil;
import com.fruvio.GestionConge.utilisateur.controller.PasswordResetController;
import com.fruvio.GestionConge.notifications_conge.repository.NotificationRepository;
import com.fruvio.GestionConge.service_conge.repository.Service_congeRepository;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.solde_conge.service.Solde_congeService;
import com.fruvio.GestionConge.utilisateur.dto.ResetPasswordRequest;
import com.fruvio.GestionConge.utilisateur.dto.UtilisateurCreateRequest;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private Solde_congeService soldeCongeService;

    @Mock
    private EmailService emailService;

    @Mock
    private DemandeCongeRepository demandeCongeRepository;

    @Mock
    private Solde_congeRepository soldeCongeRepository;

    @Mock
    private Historique_mouvementRepository historiqueRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private Service_congeRepository serviceRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UtilisateurService utilisateurService;

    @Test
    void createUtilisateurRejectsAdminRole() {
        UtilisateurCreateRequest request = UtilisateurCreateRequest.builder().matricule("ADM-NEW")
                .email("admin@example.com").role(Role.ADMIN).build();

        when(utilisateurRepository.existsByMatricule("ADM-NEW")).thenReturn(false);
        when(utilisateurRepository.existsByEmail("admin@example.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> utilisateurService.createUtilisateur(request,
                Utilisateur.builder().id(1L).role(Role.ADMIN).build()));

        verify(utilisateurRepository, never()).save(any(Utilisateur.class));
    }

    @Test
    void resetPasswordEndpointAcceptsTokenAndNewPassword() {
        PasswordResetController controller = new PasswordResetController(
                new PasswordResetService(utilisateurRepository, emailService, passwordEncoder));
        ResetPasswordRequest request = ResetPasswordRequest.builder().token("tok-123")
                .newPassword("NouveauMotDePasse123!").build();

        Utilisateur user = Utilisateur.builder().id(10L).role(Role.EMPLOYE).resetToken("tok-123")
                .tokenExpiration(LocalDateTime.now().plusMinutes(15)).build();

        when(utilisateurRepository.findByResetToken("tok-123")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NouveauMotDePasse123!")).thenReturn("hashed-password");

        ResponseEntity<?> response = controller.resetPassword(request, null, null);

        assert (response.getStatusCode().is2xxSuccessful());
        verify(utilisateurRepository).save(user);
    }

    @Test
    void createPasswordResetTokenStoresTokenAndSendsThroughEmailService() {
        PasswordResetService passwordResetService = new PasswordResetService(utilisateurRepository, emailService,
                passwordEncoder);
        Utilisateur user = Utilisateur.builder().id(10L).email("employee@example.com").actif(true).build();
        when(utilisateurRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(user));

        passwordResetService.createPasswordResetToken("employee@example.com");

        assertNotNull(user.getResetToken());
        assertTrue(user.getTokenExpiration().isAfter(LocalDateTime.now()));
        verify(emailService).sendResetPasswordEmail(eq(user.getEmail()), eq(user.getResetToken()));
        verify(utilisateurRepository).save(user);
    }

    @Test
    void createPasswordResetTokenClearsTokenWhenEmailCannotBeSent() {
        PasswordResetService passwordResetService = new PasswordResetService(utilisateurRepository, emailService,
                passwordEncoder);
        Utilisateur user = Utilisateur.builder().id(10L).email("employee@example.com").actif(true).build();
        when(utilisateurRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("SMTP unavailable")).when(emailService)
                .sendResetPasswordEmail(eq(user.getEmail()), any());

        passwordResetService.createPasswordResetToken("employee@example.com");

        assertNull(user.getResetToken());
        assertNull(user.getTokenExpiration());
        verify(utilisateurRepository, org.mockito.Mockito.times(2)).save(user);
    }

    @Test
    void resetEmailUsesConfiguredFrontendAndFruvioSender() throws Exception {
        JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        EmailService service = new EmailService(mailSender, "no-reply@example.com", "https://gestion.example/");

        service.sendResetPasswordEmail("employee@example.com", "test-token");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage sentMessage = messageCaptor.getValue();
        InternetAddress sender = (InternetAddress) sentMessage.getFrom()[0];
        assertEquals("Fruvio", sender.getPersonal());
        assertTrue(((String) sentMessage.getContent())
                .contains("https://gestion.example/Components/mdpReset?token=test-token"));
    }

    @Test
    void createUtilisateurRejectsManagerEscalation() {
        UtilisateurCreateRequest request = UtilisateurCreateRequest.builder().matricule("MGR-NEW")
                .email("manager@example.com").role(Role.ADMIN).build();

        when(utilisateurRepository.existsByMatricule("MGR-NEW")).thenReturn(false);
        when(utilisateurRepository.existsByEmail("manager@example.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> utilisateurService.createUtilisateur(request,
                Utilisateur.builder().id(8L).role(Role.MANAGER).build()));

        verify(utilisateurRepository, never()).save(any(Utilisateur.class));
    }

    @Test
    void updateUtilisateurRejectsManagerRoleGrant() {
        Utilisateur target = Utilisateur.builder().id(2L).role(Role.EMPLOYE).build();
        Utilisateur manager = Utilisateur.builder().id(9L).role(Role.MANAGER).build();

        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThrows(AccessDeniedException.class,
                () -> utilisateurService
                        .updateUtilisateur(com.fruvio.GestionConge.utilisateur.dto.UtilisateurUpdateRequest.builder()
                                .id(2L).role(Role.ADMIN).build(), manager));
    }

    @Test
    void getAllUtilisateursRejectsEmployee() {
        Utilisateur employee = Utilisateur.builder().id(5L).role(Role.EMPLOYE).build();

        assertThrows(AccessDeniedException.class, () -> utilisateurService.getAllUtilisateurs(employee));
    }

    @Test
    void deleteUtilisateurRejectsCurrentAdminAccount() {
        Utilisateur admin = Utilisateur.builder().id(1L).role(Role.ADMIN).build();
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(IllegalStateException.class, () -> utilisateurService.deleteUtilisateur(1L, admin));

        verify(utilisateurRepository, never()).delete(any(Utilisateur.class));
    }

    @Test
    void deleteUtilisateurRejectsCollaboratorWithLeaveHistory() {
        Utilisateur admin = Utilisateur.builder().id(1L).role(Role.ADMIN).build();
        Utilisateur collaborator = Utilisateur.builder().id(2L).role(Role.EMPLOYE).build();
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(collaborator));
        when(demandeCongeRepository.existsByUtilisateurId(2L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> utilisateurService.deleteUtilisateur(2L, admin));

        verify(utilisateurRepository, never()).delete(any(Utilisateur.class));
    }

    @Test
    void deleteUtilisateurRequiresAdmin() {
        Utilisateur employee = Utilisateur.builder().id(1L).role(Role.EMPLOYE).build();

        assertThrows(AccessDeniedException.class, () -> utilisateurService.deleteUtilisateur(2L, employee));

        verify(utilisateurRepository, never()).findById(2L);
    }
}