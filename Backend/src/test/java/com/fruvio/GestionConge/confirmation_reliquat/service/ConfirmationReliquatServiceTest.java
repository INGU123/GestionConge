package com.fruvio.GestionConge.confirmation_reliquat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fruvio.GestionConge.circuit_conge.entity.CircuitDemandeConge;
import com.fruvio.GestionConge.circuit_conge.repository.CircuitDemandeCongeRepository;
import com.fruvio.GestionConge.confirmation_reliquat.entity.ConfirmationReliquat;
import com.fruvio.GestionConge.confirmation_reliquat.repository.ConfirmationReliquatRepository;
import com.fruvio.GestionConge.employe.repository.EmployeRepository;
import com.fruvio.GestionConge.notifications_conge.service.Notification_congeService;
import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.solde_conge.service.Solde_congeService;
import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfirmationReliquatServiceTest {

    @Mock ConfirmationReliquatRepository confirmations;
    @Mock CircuitDemandeCongeRepository circuits;
    @Mock EmployeRepository employes;
    @Mock Notification_congeService notifications;
    @Mock Solde_congeRepository soldes;
    @Mock Solde_congeService soldeService;
    @Mock Type_congeRepository typesConge;
    @Mock UtilisateurRepository utilisateurs;

    private ConfirmationReliquatService service() {
        return new ConfirmationReliquatService(confirmations, circuits, employes,
                notifications, soldes, soldeService, typesConge, utilisateurs);
    }

    private Utilisateur compte(Long id, Role role) {
        return Utilisateur.builder()
                .id(id)
                .matricule("TEST" + id)
                .role(role)
                .actif(true)
                .date_embauche(LocalDate.of(2020, 1, 1))
                .build();
    }

    private CircuitDemandeConge dossier(Long utilisateurId) {
        return CircuitDemandeConge.builder()
                .id(900L)
                .utilisateurId(utilisateurId)
                .employeId(null)
                .typeCongeId(1L)
                .assistantId(2L)
                .dateDebut(LocalDate.of(2026, 12, 1))
                .dateFin(LocalDate.of(2026, 12, 10))
                .nombreJours(15)
                .statut("RELIQUAT_EN_ATTENTE")
                .build();
    }

    private Solde_conge solde(Long utilisateurId, int annee, int acquis, int pris, int restant) {
        return Solde_conge.builder()
                .utilisateurId(utilisateurId)
                .employeId(null)
                .typeCongeId(1L)
                .periode(annee)
                .soldeAquis(acquis)
                .soldePris(pris)
                .soldeRestant(restant)
                .enDemiJournees(false)
                .build();
    }

    private void preparer(Long utilisateurId, Role role) {
        Utilisateur proprietaire = compte(utilisateurId, role);
        when(utilisateurs.findById(utilisateurId)).thenReturn(Optional.of(proprietaire));
        when(typesConge.findById(1L)).thenReturn(Optional.of(
                Type_conge.builder().id(1L).code("CONGE_ANNUEL").libelle("Congé annuel").build()));
        when(circuits.findByIdForUpdate(900L)).thenReturn(Optional.of(dossier(utilisateurId)));
        when(confirmations.findByUtilisateurIdOrderByDateConfirmationDesc(utilisateurId)).thenReturn(List.of());
        when(confirmations.existsByReferenceConfirmation(anyString())).thenReturn(false);
        when(soldes.findByUtilisateurIdAndTypeCongeIdAndPeriode(utilisateurId, 1L, 2025))
                .thenReturn(Optional.of(solde(utilisateurId, 2025, 20, 10, 10)));
        when(soldes.findByUtilisateurIdAndTypeCongeIdAndPeriode(utilisateurId, 1L, 2026))
                .thenReturn(Optional.of(solde(utilisateurId, 2026, 20, 0, 20)));
        when(soldes.findByUtilisateurIdAndTypeCongeId(utilisateurId, 1L))
                .thenReturn(List.of(solde(utilisateurId, 2026, 20, 0, 20)));
        when(confirmations.save(any(ConfirmationReliquat.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void calculeLeReliquatPourToutRoleDeCompte(Role role) {
        long utilisateurId = 100L + role.ordinal();
        preparer(utilisateurId, role);

        ConfirmationReliquatService.CalculReliquat calcul =
                service().calculerReliquat(900L, compte(1L, Role.ADMINISTRATION_PERSONNEL));

        assertEquals(10, calcul.reliquatDisponible());
        assertEquals(10, calcul.reliquatBrut());
        assertEquals("Exercice 2025", calcul.periodeReference());
        verify(soldes).findByUtilisateurIdAndTypeCongeIdAndPeriode(utilisateurId, 1L, 2025);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void confirmeLeReliquatPourToutRoleDeCompte(Role role) {
        long utilisateurId = 200L + role.ordinal();
        preparer(utilisateurId, role);

        ConfirmationReliquat resultat = service().confirmer(900L, 10, "Exercice 2025",
                "TEST-ROLE-" + role.name(), null, null,
                compte(1L, Role.ADMINISTRATION_PERSONNEL));

        assertNotNull(resultat);
        ArgumentCaptor<ConfirmationReliquat> captor = ArgumentCaptor.forClass(ConfirmationReliquat.class);
        verify(confirmations).save(captor.capture());
        assertNull(captor.getValue().getEmployeId());
        assertEquals(utilisateurId, captor.getValue().getUtilisateurId());
        assertEquals(10, captor.getValue().getNombreJoursConfirme());
        verify(circuits).save(any(CircuitDemandeConge.class));
        verify(notifications).creerNotification(eq(2L), contains("10 jour"), eq("CIRCUIT_CONGE"),
                eq("/dashboard/circuit-conge"));
    }

    @Test
    void refuseUnePeriodeDeReferenceAnnuelleQuiNestPasLexercicePrecedent() {
        preparer(300L, Role.ASSISTANT_DIRECTION);

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> service().confirmer(900L, 10, "Exercice 2024",
                        "TEST-MAUVAISE-PERIODE", null, null,
                        compte(1L, Role.ADMINISTRATION_PERSONNEL)));

        assertTrue(erreur.getMessage().contains("2025"));
        verify(confirmations, never()).save(any(ConfirmationReliquat.class));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void permetLeRapprochementManuelPourToutCompteHistoriqueSansCreerDeSolde(Role role) {
        long utilisateurId = 400L + role.ordinal();
        Utilisateur proprietaire = compte(utilisateurId, role);
        when(utilisateurs.findById(utilisateurId)).thenReturn(Optional.of(proprietaire));
        when(typesConge.findById(1L)).thenReturn(Optional.of(
                Type_conge.builder().id(1L).code("CONGE_ANNUEL").libelle("Congé annuel").build()));
        when(circuits.findByIdForUpdate(900L)).thenReturn(Optional.of(dossier(utilisateurId)));
        when(confirmations.existsByReferenceConfirmation("RH-MANUEL-900")).thenReturn(false);
        when(confirmations.save(any(ConfirmationReliquat.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmationReliquat resultat = service().confirmerManuellementCompteUtilisateur(
                900L, 2, "Exercice 2025", "RH-MANUEL-900",
                "Montant rapproché avec le dossier RH papier signé.",
                compte(1L, Role.ADMINISTRATION_PERSONNEL));

        assertNotNull(resultat);
        ArgumentCaptor<ConfirmationReliquat> captor = ArgumentCaptor.forClass(ConfirmationReliquat.class);
        verify(confirmations).save(captor.capture());
        assertNull(captor.getValue().getEmployeId());
        assertEquals(utilisateurId, captor.getValue().getUtilisateurId());
        assertEquals(2, captor.getValue().getNombreJoursConfirme());
        assertEquals("Montant rapproché avec le dossier RH papier signé.", captor.getValue().getObservation());
        verify(soldes, never()).save(any(Solde_conge.class));
        verify(circuits).save(any(CircuitDemandeConge.class));
        verify(notifications).creerNotification(eq(2L), contains("confirmé manuellement"),
                eq("CIRCUIT_CONGE"), eq("/dashboard/circuit-conge"));
    }

    @Test
    void exigeUneObservationPourTouteConfirmationManuelle() {
        Utilisateur proprietaire = compte(501L, Role.DIRECTEUR);
        when(utilisateurs.findById(501L)).thenReturn(Optional.of(proprietaire));
        when(typesConge.findById(1L)).thenReturn(Optional.of(
                Type_conge.builder().id(1L).code("CONGE_ANNUEL").libelle("Congé annuel").build()));
        when(circuits.findByIdForUpdate(900L)).thenReturn(Optional.of(dossier(501L)));

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> service().confirmerManuellementCompteUtilisateur(
                        900L, 2, "Exercice 2025", "RH-MANUEL-SANS-OBS",
                        "  ", compte(1L, Role.ADMINISTRATION_PERSONNEL)));

        assertTrue(erreur.getMessage().contains("observation RH"));
        verify(confirmations, never()).save(any(ConfirmationReliquat.class));
        verify(circuits, never()).save(any(CircuitDemandeConge.class));
    }
}
