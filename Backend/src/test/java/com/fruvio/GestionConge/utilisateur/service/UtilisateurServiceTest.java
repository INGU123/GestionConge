package com.fruvio.GestionConge.utilisateur.service;

import com.fruvio.GestionConge.utilisateur.config.JwtUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.time.LocalDateTime;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.fruvio.GestionConge.demande_conge.repository.DemandeCongeRepository;
import com.fruvio.GestionConge.historique_mouvement.repository.Historique_mouvementRepository;
import com.fruvio.GestionConge.notifications_conge.repository.NotificationRepository;
import com.fruvio.GestionConge.service_conge.repository.Service_congeRepository;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.solde_conge.service.Solde_congeService;
import com.fruvio.GestionConge.utilisateur.dto.ForgotPasswordRequest;
import com.fruvio.GestionConge.utilisateur.dto.UtilisateurCreateRequest;
import com.fruvio.GestionConge.utilisateur.controller.PasswordResetController;
import com.fruvio.GestionConge.utilisateur.entity.PasswordResetRequestEntity;
import com.fruvio.GestionConge.utilisateur.repository.PasswordResetRequestRepository;
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
    void loginDoesNotAcceptEmailAsIdentifier() {
        when(utilisateurRepository.findByMatricule("employee@example.com")).thenReturn(Optional.empty());

        assertNull(utilisateurService.login("employee@example.com", "any-password"));

        verify(utilisateurRepository, never()).findByEmail("employee@example.com");
    }

    @Test
    void loginSucceedsWithValidMatriculeAndPassword() {
        String hash = "$2a$10$mockedBcryptHash";
        Utilisateur employee = Utilisateur.builder()
                .id(11L).matricule("EMP-LOGIN-01")
                .actif(true).password(hash).build();

        when(utilisateurRepository.findByMatricule("EMP-LOGIN-01"))
                .thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("Valid123!", hash)).thenReturn(true);

        assertEquals(employee, utilisateurService.login("EMP-LOGIN-01", "Valid123!"));
        verify(passwordEncoder).matches("Valid123!", hash);
    }

    @Test
    void loginRejectsIncorrectPassword() {
        String hash = "$2a$10$mockedBcryptHash";
        Utilisateur employee = Utilisateur.builder()
                .id(12L).matricule("EMP-LOGIN-02")
                .actif(true).password(hash).build();

        when(utilisateurRepository.findByMatricule("EMP-LOGIN-02"))
                .thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("Wrong123!", hash)).thenReturn(false);

        assertNull(utilisateurService.login("EMP-LOGIN-02", "Wrong123!"));
        verify(passwordEncoder).matches("Wrong123!", hash);
    }

    @Test
    void loginRejectsInactiveAccount() {
        String hash = "$2a$10$mockedBcryptHash";
        Utilisateur employee = Utilisateur.builder()
                .id(13L).matricule("EMP-LOGIN-03")
                .actif(false).password(hash).build();

        when(utilisateurRepository.findByMatricule("EMP-LOGIN-03"))
                .thenReturn(Optional.of(employee));

        assertNull(utilisateurService.login("EMP-LOGIN-03", "Valid123!"));
        verify(passwordEncoder, never()).matches("Valid123!", hash);
    }

    @Test
    void loginRejectsUnknownMatricule() {
        when(utilisateurRepository.findByMatricule("EMP-UNKNOWN"))
                .thenReturn(Optional.empty());

        assertNull(utilisateurService.login("EMP-UNKNOWN", "Valid123!"));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void loginRejectsNonBcryptPassword() {
        Utilisateur employee = Utilisateur.builder()
                .id(14L).matricule("EMP-LOGIN-04")
                .actif(true).password("plain-text-password").build();

        when(utilisateurRepository.findByMatricule("EMP-LOGIN-04"))
                .thenReturn(Optional.of(employee));

        assertNull(utilisateurService.login("EMP-LOGIN-04", "plain-text-password"));
        verify(passwordEncoder, never()).matches("plain-text-password", "plain-text-password");
    }

    @Test
    void forgotPasswordRequestLooksUpMatriculeAndNotEmail() {
        PasswordResetRequestRepository requestRepository = org.mockito.Mockito.mock(PasswordResetRequestRepository.class);
        PasswordResetController controller = new PasswordResetController(
                utilisateurRepository, requestRepository, emailService, passwordEncoder);
        Utilisateur employee = Utilisateur.builder()
                .id(10L).matricule("EMP-001").email("employee@example.com")
                .nom("Test").prenom("Agent").actif(true).build();

        when(utilisateurRepository.findByMatricule("EMP-001")).thenReturn(Optional.of(employee));
        when(requestRepository.findByStatutOrderByDateDemandeDesc("EN_ATTENTE")).thenReturn(java.util.List.of());
        when(requestRepository.save(any(PasswordResetRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(utilisateurRepository.findAll()).thenReturn(java.util.List.of());

        var response = controller.forgotPassword(ForgotPasswordRequest.builder().matricule("EMP-001").build());

        assertEquals(200, response.getStatusCode().value());
        verify(utilisateurRepository).findByMatricule("EMP-001");
        verify(utilisateurRepository, never()).findByEmail("employee@example.com");
        verify(requestRepository).save(any(PasswordResetRequestEntity.class));
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

        assertThrows(IllegalStateException.class, () -> utilisateurService.deleteUtilisateur(1L, null, admin));

        verify(utilisateurRepository, never()).delete(any(Utilisateur.class));
    }

    @Test
    void archiveUtilisateurKeepsLeaveHistoryAndSendsNotification() {
        Utilisateur admin = Utilisateur.builder().id(1L).role(Role.ADMIN).build();
        Utilisateur collaborator = Utilisateur.builder().id(2L).role(Role.EMPLOYE)
                .matricule("EMP-002").email("employee@example.com")
                .nom("Agent").prenom("Test").actif(true).build();
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(collaborator));

        assertEquals(true, utilisateurService.deleteUtilisateur(2L, "Fin de contrat", admin));

        assertEquals(false, collaborator.isActif());
        assertEquals(true, collaborator.isArchived());
        assertEquals(1L, collaborator.getDeletedBy());
        assertEquals("Fin de contrat", collaborator.getDeletionReason());
        assertEquals(true, collaborator.getArchiveEmailSentAt() != null);
        verify(utilisateurRepository).save(collaborator);
        verify(emailService).sendAccountArchivedEmail(
                "employee@example.com", "EMP-002", "Agent", "Test",
                collaborator.getDeletedAt(), "Fin de contrat");
        verify(demandeCongeRepository, never()).existsByUtilisateurId(2L);
    }

    @Test
    void archiveUtilisateurRejectsWhenNotificationCannotBeSent() {
        Utilisateur admin = Utilisateur.builder().id(1L).role(Role.ADMIN).build();
        Utilisateur collaborator = Utilisateur.builder().id(3L).role(Role.EMPLOYE)
                .matricule("EMP-003").email("employee@example.com").actif(true).build();
        when(utilisateurRepository.findById(3L)).thenReturn(Optional.of(collaborator));
        org.mockito.Mockito.doThrow(new IllegalStateException("SMTP indisponible"))
                .when(emailService).sendAccountArchivedEmail(
                        org.mockito.ArgumentMatchers.eq("employee@example.com"),
                        org.mockito.ArgumentMatchers.eq("EMP-003"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        any(LocalDateTime.class),
                        org.mockito.ArgumentMatchers.isNull());

        assertThrows(IllegalStateException.class,
                () -> utilisateurService.deleteUtilisateur(3L, null, admin));
        verify(utilisateurRepository, never()).save(collaborator);
    }

    @Test
    void restoreUtilisateurRestoresPreviousActiveState() {
        Utilisateur admin = Utilisateur.builder().id(1L).role(Role.ADMIN).build();
        Utilisateur collaborator = Utilisateur.builder().id(4L).role(Role.EMPLOYE)
                .actif(false).actifAvantArchivage(false)
                .deletedAt(LocalDateTime.now()).deletedBy(1L).build();
        when(utilisateurRepository.findById(4L)).thenReturn(Optional.of(collaborator));

        assertEquals(true, utilisateurService.restoreUtilisateur(4L, admin));
        assertEquals(false, collaborator.isActif());
        assertEquals(false, collaborator.isArchived());
        verify(utilisateurRepository).save(collaborator);
    }

    @Test
    void deleteUtilisateurRequiresAdmin() {
        Utilisateur employee = Utilisateur.builder().id(1L).role(Role.EMPLOYE).build();

        assertThrows(AccessDeniedException.class, () -> utilisateurService.deleteUtilisateur(2L, null, employee));

        verify(utilisateurRepository, never()).findById(2L);
    }
}