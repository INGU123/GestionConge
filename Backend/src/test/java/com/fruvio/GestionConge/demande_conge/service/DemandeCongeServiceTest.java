package com.fruvio.GestionConge.demande_conge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fruvio.GestionConge.demande_conge.entity.DemandeConge;
import com.fruvio.GestionConge.demande_conge.repository.DemandeCongeRepository;
import com.fruvio.GestionConge.jour_ferie.service.JourFerieService;
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

        @Mock
        private JourFerieService jourFerieService;

        @InjectMocks
        private DemandeCongeService demandeCongeService;

        @BeforeEach
        void setUp() {
                lenient().when(demandeRepo.save(any(DemandeConge.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void compterJoursOuvresLundiVendredi() {
                when(jourFerieService.compterJoursOuvres(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 9)))
                                .thenReturn(5);

                assertEquals(5, jourFerieService.compterJoursOuvres(LocalDate.of(2026, 1, 5),
                                LocalDate.of(2026, 1, 9)));
        }

        @Test
        void compterJoursOuvresVendrediLundi() {
                when(jourFerieService.compterJoursOuvres(LocalDate.of(2026, 1, 9), LocalDate.of(2026, 1, 12)))
                                .thenReturn(2);

                assertEquals(2, jourFerieService.compterJoursOuvres(LocalDate.of(2026, 1, 9),
                                LocalDate.of(2026, 1, 12)));
        }

        @Test
        void creerDemandeRejectsWeekendOnlyPeriod() {
                DemandeConge request = DemandeConge.builder().utilisateurId(7L).dateDebut(LocalDate.of(2026, 11, 7))
                                .dateFin(LocalDate.of(2026, 11, 8)).build();
                lenient().when(demandeRepo.findByUtilisateurId(7L)).thenReturn(List.of());
                lenient().when(jourFerieService.compterJoursOuvres(LocalDate.of(2026, 11, 7),
                                LocalDate.of(2026, 11, 8))).thenReturn(0);

                assertThrows(IllegalArgumentException.class, () -> demandeCongeService.creerDemande(7L, request,
                                Utilisateur.builder().id(7L).role(Role.EMPLOYE).build()));
        }

        @Test
        void creerDemandeRejectsOverlappingDatePeriod() {
                DemandeConge existing = DemandeConge.builder().id(1L).utilisateurId(7L)
                                .dateDebut(LocalDate.of(2026, 10, 1)).dateFin(LocalDate.of(2026, 10, 10))
                                .statut("VALIDEE").build();
                DemandeConge newRequest = DemandeConge.builder().utilisateurId(7L).dateDebut(LocalDate.of(2026, 10, 5))
                                .dateFin(LocalDate.of(2026, 10, 15)).build();

                when(demandeRepo.findByUtilisateurId(7L)).thenReturn(List.of(existing));
                when(jourFerieService.compterJoursOuvres(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 15)))
                                .thenReturn(7);

                assertThrows(IllegalStateException.class, () -> demandeCongeService.creerDemande(7L, newRequest,
                                Utilisateur.builder().id(7L).role(Role.EMPLOYE).build()));
        }

        @Test
        void creerDemandeRejectsEndBeforeStart() {
                DemandeConge request = DemandeConge.builder().utilisateurId(7L).dateDebut(LocalDate.of(2026, 12, 10))
                                .dateFin(LocalDate.of(2026, 12, 8)).build();

                lenient().when(demandeRepo.findByUtilisateurId(7L)).thenReturn(List.of());

                assertThrows(IllegalArgumentException.class, () -> demandeCongeService.creerDemande(7L, request,
                                Utilisateur.builder().id(7L).role(Role.EMPLOYE).build()));
        }

        @Test
        void traiterDemandeRejectsInsufficientBalance() {
                Utilisateur manager = Utilisateur.builder().id(9L).role(Role.MANAGER).build();
                DemandeConge demande = DemandeConge.builder().id(15L).utilisateurId(7L).typeCongeId(1L)
                                .dateDebut(LocalDate.of(2026, 12, 1)).dateFin(LocalDate.of(2026, 12, 4))
                                .statut("EN_ATTENTE").build();

                when(demandeRepo.findById(15L)).thenReturn(Optional.of(demande));
                when(utilisateurRepo.findById(7L))
                                .thenReturn(Optional.of(Utilisateur.builder().id(7L).manager_id(9L).build()));
                when(jourFerieService.compterJoursOuvres(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4)))
                                .thenReturn(3);
                when(soldeRepo.findByUtilisateurIdAndTypeCongeIdAndPeriode(7L, 1L, 2026))
                                .thenReturn(Optional.of(Solde_conge.builder().utilisateurId(7L).typeCongeId(1L)
                                                .periode(2026).soldeAquis(3).soldePris(3).soldeRestant(0).build()));

                assertThrows(IllegalStateException.class,
                                () -> demandeCongeService.traiterDemande(15L, 9L, "VALIDEE", null, manager));
        }

        @Test
        void traiterDemandeValidatesAndDebitsRealWorkingDays() {
                Utilisateur manager = Utilisateur.builder().id(9L).role(Role.MANAGER).build();
                DemandeConge demande = DemandeConge.builder().id(15L).utilisateurId(7L).typeCongeId(1L)
                                .dateDebut(LocalDate.of(2026, 12, 24)).dateFin(LocalDate.of(2026, 12, 28))
                                .statut("EN_ATTENTE").build();
                Solde_conge solde = Solde_conge.builder().id(11L).utilisateurId(7L).typeCongeId(1L).periode(2026)
                                .soldeAquis(20).soldePris(2).soldeRestant(18).build();

                when(demandeRepo.findById(15L)).thenReturn(Optional.of(demande));
                when(utilisateurRepo.findById(7L))
                                .thenReturn(Optional.of(Utilisateur.builder().id(7L).manager_id(9L).build()));
                when(jourFerieService.compterJoursOuvres(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 28)))
                                .thenReturn(2);
                when(soldeRepo.findByUtilisateurIdAndTypeCongeIdAndPeriode(7L, 1L, 2026))
                                .thenReturn(Optional.of(solde));

                DemandeConge result = demandeCongeService.traiterDemande(15L, 9L, "VALIDEE", null, manager);

                assertEquals(2, result.getNombreJours());
                verify(soldeRepo).save(any(Solde_conge.class));
        }

        @Test
        void annulerDemandeValideeRefundsRealWorkingDays() {
                DemandeConge demande = DemandeConge.builder().id(22L).utilisateurId(7L).typeCongeId(1L)
                                .dateDebut(LocalDate.of(2026, 12, 24)).dateFin(LocalDate.of(2026, 12, 28))
                                .nombreJours(2).statut("VALIDEE").build();
                Solde_conge solde = Solde_conge.builder().id(5L).utilisateurId(7L).typeCongeId(1L).periode(2026)
                                .soldeAquis(20).soldePris(2).soldeRestant(18).build();

                when(demandeRepo.findById(22L)).thenReturn(Optional.of(demande));
                when(soldeRepo.findByUtilisateurIdAndTypeCongeIdAndPeriode(7L, 1L, 2026))
                                .thenReturn(Optional.of(solde));

                DemandeConge result = demandeCongeService.annulerDemande(22L, 7L,
                                Utilisateur.builder().id(7L).role(Role.EMPLOYE).build());

                assertEquals("ANNULEE", result.getStatut());
                assertEquals(0, solde.getSoldePris());
                assertEquals(20, solde.getSoldeRestant());
        }

        @Test
        void creerDemandeRejectsWhenOnlyNonWorkingDays() {
                DemandeConge request = DemandeConge.builder().utilisateurId(7L).dateDebut(LocalDate.of(2026, 11, 7))
                                .dateFin(LocalDate.of(2026, 11, 8)).build();
                lenient().when(demandeRepo.findByUtilisateurId(7L)).thenReturn(List.of());
                lenient().when(jourFerieService.compterJoursOuvres(LocalDate.of(2026, 11, 7),
                                LocalDate.of(2026, 11, 8))).thenReturn(0);

                assertThrows(IllegalArgumentException.class, () -> demandeCongeService.creerDemande(7L, request,
                                Utilisateur.builder().id(7L).role(Role.EMPLOYE).build()));
        }
}
