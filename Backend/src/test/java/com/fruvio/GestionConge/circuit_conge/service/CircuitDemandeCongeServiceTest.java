package com.fruvio.GestionConge.circuit_conge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import java.util.Optional;

import com.fruvio.GestionConge.circuit_conge.entity.CircuitDemandeConge;
import com.fruvio.GestionConge.circuit_conge.repository.CircuitDemandeCongeRepository;
import com.fruvio.GestionConge.departement.repository.DepartementRepository;
import com.fruvio.GestionConge.jour_ferie.service.JourFerieService;
import com.fruvio.GestionConge.notifications_conge.service.Notification_congeService;
import com.fruvio.GestionConge.service_conge.repository.Service_congeRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;
import com.fruvio.GestionConge.demande_conge.service.DemandeCongeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CircuitDemandeCongeServiceTest {

    @Mock private CircuitDemandeCongeRepository circuits;
    @Mock private UtilisateurRepository utilisateurs;
    @Mock private Service_congeRepository services;
    @Mock private DepartementRepository departements;
    @Mock private JourFerieService joursFeries;
    @Mock private DemandeCongeService demandesHistoriques;
    @Mock private Notification_congeService notifications;

    @InjectMocks
    private CircuitDemandeCongeService service;

    private Utilisateur chef;
    private CircuitDemandeConge dossier;

    @BeforeEach
    void setUp() {
        chef = Utilisateur.builder().id(7L).role(Role.CHEF_DEPARTEMENT).actif(true).build();
        dossier = CircuitDemandeConge.builder()
                .id(10L).chefId(7L).directeurId(8L).statut("CHEF_EN_ATTENTE").build();
        when(circuits.findByIdForUpdate(10L)).thenReturn(Optional.of(dossier));
    }

    @Test
    void chefOpinionIsPersistedBeforeTransmissionToDirector() {
        when(circuits.save(any(CircuitDemandeConge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CircuitDemandeConge result = service.transmettreAuDirecteur(
                10L, "FAVORABLE", "Le dossier est conforme.", chef);

        assertThat(result.getAvisChef()).isEqualTo("FAVORABLE");
        assertThat(result.getCommentaireChef()).isEqualTo("Le dossier est conforme.");
        assertThat(result.getStatut()).isEqualTo("DIRECTEUR_EN_ATTENTE");
        assertThat(result.getDateTransmissionDirecteur()).isNotNull();
        verify(notifications).creerNotification(
                eq(8L), contains("attend votre décision"), eq("CIRCUIT_CONGE"), eq("/dashboard/directeur"));
    }

    @Test
    void negativeChefOpinionRequiresAnObservation() {
        assertThatThrownBy(() -> service.transmettreAuDirecteur(10L, "DEFAVORABLE", "  ", chef))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("observation");
        verify(circuits, never()).save(any(CircuitDemandeConge.class));
    }
}
