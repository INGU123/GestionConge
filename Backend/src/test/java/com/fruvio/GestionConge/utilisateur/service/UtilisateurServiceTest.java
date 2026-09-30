package com.fruvio.GestionConge.utilisateur.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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

    @InjectMocks
    private UtilisateurService utilisateurService;

    @Test
    void createUtilisateurRejectsAdminRole() {
        UtilisateurCreateRequest request = UtilisateurCreateRequest.builder()
                .matricule("ADM-NEW")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        when(utilisateurRepository.existsByMatricule("ADM-NEW")).thenReturn(false);
        when(utilisateurRepository.existsByEmail("admin@example.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> utilisateurService.createUtilisateur(request,
                        Utilisateur.builder().id(1L).role(Role.ADMIN).build()));

        verify(utilisateurRepository, never()).save(any(Utilisateur.class));
    }

    @Test
    void deleteUtilisateurRejectsCurrentAdminAccount() {
        Utilisateur admin = Utilisateur.builder().id(1L).role(Role.ADMIN).build();
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(IllegalStateException.class,
                () -> utilisateurService.deleteUtilisateur(1L, admin));

        verify(utilisateurRepository, never()).delete(any(Utilisateur.class));
    }

    @Test
    void deleteUtilisateurRejectsCollaboratorWithLeaveHistory() {
        Utilisateur admin = Utilisateur.builder().id(1L).role(Role.ADMIN).build();
        Utilisateur collaborator = Utilisateur.builder().id(2L).role(Role.EMPLOYE).build();
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(collaborator));
        when(demandeCongeRepository.existsByUtilisateurId(2L)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> utilisateurService.deleteUtilisateur(2L, admin));

        verify(utilisateurRepository, never()).delete(any(Utilisateur.class));
    }

    @Test
    void deleteUtilisateurRequiresAdmin() {
        Utilisateur employee = Utilisateur.builder().id(1L).role(Role.EMPLOYE).build();

        assertThrows(AccessDeniedException.class,
                () -> utilisateurService.deleteUtilisateur(2L, employee));

        verify(utilisateurRepository, never()).findById(2L);
    }
}