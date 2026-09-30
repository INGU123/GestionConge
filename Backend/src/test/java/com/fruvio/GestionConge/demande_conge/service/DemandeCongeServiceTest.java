package com.fruvio.GestionConge.demande_conge.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fruvio.GestionConge.demande_conge.entity.DemandeConge;
import com.fruvio.GestionConge.demande_conge.repository.DemandeCongeRepository;
import com.fruvio.GestionConge.historique_mouvement.service.Historique_mouvementService;
import com.fruvio.GestionConge.notifications_conge.service.Notification_congeService;
import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@ExtendWith(MockitoExtension.class)
class DemandeCongeServiceTest {

    @Mock
    private DemandeCongeRepository demandeRepo;

    @Mock
    private Solde_congeRepository soldeRepo;

    @Mock
    private Historique_mouvementService historiqueService;

    @Mock
    private Notification_congeService notificationService;

    @Mock
    private UtilisateurRepository utilisateurRepo;

    @InjectMocks
    private DemandeCongeService demandeCongeService;

    @Test
    void creerDemandeRejectsOverlappingDatePeriod() {
        DemandeConge existing = DemandeConge.builder().id(1L).utilisateurId(7L).dateDebut(LocalDate.of(2026, 10, 1))
                .dateFin(LocalDate.of(2026, 10, 10)).statut("VALIDEE").build();

        DemandeConge newRequest = DemandeConge.builder().utilisateurId(7L).dateDebut(LocalDate.of(2026, 10, 5))
                .dateFin(LocalDate.of(2026, 10, 15)).build();

        when(demandeRepo.findByUtilisateurId(7L)).thenReturn(List.of(existing));

        assertThrows(IllegalStateException.class, () -> demandeCongeService.creerDemande(7L, newRequest,
                Utilisateur.builder().id(7L).role(Role.EMPLOYE).build()));
    }

    @Test
    void traiterDemandeRejectsAlreadyValidatedRequest() {
        Utilisateur manager = Utilisateur.builder().id(9L).role(Role.MANAGER).build();
        DemandeConge demande = DemandeConge.builder().id(12L).utilisateurId(7L).typeCongeId(1L)
                .dateDebut(LocalDate.of(2026, 11, 1)).dateFin(LocalDate.of(2026, 11, 3)).statut("VALIDEE").build();

        when(demandeRepo.findById(12L)).thenReturn(Optional.of(demande));
        when(utilisateurRepo.findById(7L)).thenReturn(Optional.of(Utilisateur.builder().id(7L).manager_id(9L).build()));

        assertThrows(IllegalStateException.class,
                () -> demandeCongeService.traiterDemande(12L, 9L, "VALIDEE", null, manager));
    }

    @Test
    void traiterDemandeRejectsInsufficientBalance() {
        Utilisateur manager = Utilisateur.builder().id(9L).role(Role.MANAGER).build();
        DemandeConge demande = DemandeConge.builder().id(15L).utilisateurId(7L).typeCongeId(1L)
                .dateDebut(LocalDate.of(2026, 12, 1)).dateFin(LocalDate.of(2026, 12, 4)).statut("EN_ATTENTE").build();

        when(demandeRepo.findById(15L)).thenReturn(Optional.of(demande));
        when(utilisateurRepo.findById(7L)).thenReturn(Optional.of(Utilisateur.builder().id(7L).manager_id(9L).build()));
        when(soldeRepo.findByUtilisateurIdAndTypeCongeIdAndPeriode(7L, 1L, 2026))
                .thenReturn(Optional.of(Solde_conge.builder().utilisateurId(7L).typeCongeId(1L).periode(2026)
                        .soldeAquis(3).soldePris(3).soldeRestant(0).build()));

        assertThrows(IllegalStateException.class,
                () -> demandeCongeService.traiterDemande(15L, 9L, "VALIDEE", null, manager));
    }
}
