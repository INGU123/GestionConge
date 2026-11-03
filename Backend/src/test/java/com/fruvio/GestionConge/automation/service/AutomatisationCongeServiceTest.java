package com.fruvio.GestionConge.automation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fruvio.GestionConge.demande_conge.entity.DemandeConge;
import com.fruvio.GestionConge.demande_conge.repository.DemandeCongeRepository;
import com.fruvio.GestionConge.historique_mouvement.service.Historique_mouvementService;
import com.fruvio.GestionConge.notifications_conge.repository.NotificationRepository;
import com.fruvio.GestionConge.notifications_conge.service.Notification_congeService;
import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@ExtendWith(MockitoExtension.class)
class AutomatisationCongeServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Africa/Antananarivo");

    @Mock UtilisateurRepository utilisateurRepository;
    @Mock Type_congeRepository typeCongeRepository;
    @Mock Solde_congeRepository soldeCongeRepository;
    @Mock DemandeCongeRepository demandeCongeRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock Notification_congeService notificationService;
    @Mock Historique_mouvementService historiqueService;

    @InjectMocks AutomatisationCongeService service;

    @Test
    void initialiseLeSoldeAnnuelManquantAvecLeQuotaConfigure() {
        int annee = LocalDate.now(ZONE).getYear();
        Utilisateur employe = Utilisateur.builder().id(10L).actif(true).build();
        Type_conge type = Type_conge.builder().id(20L).actif(true).nombreJoursParAn(30).build();

        when(utilisateurRepository.findByActifTrue()).thenReturn(List.of(employe));
        when(typeCongeRepository.findByActifTrue()).thenReturn(List.of(type));
        when(soldeCongeRepository.existsByUtilisateurIdAndTypeCongeIdAndPeriode(10L, 20L, annee))
                .thenReturn(false);

        ReflectionTestUtils.setField(service, "annualBalancesEnabled", true);
        service.initialiserSoldesAnnuelsManquants();

        ArgumentCaptor<Solde_conge> captor = ArgumentCaptor.forClass(Solde_conge.class);
        verify(soldeCongeRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getUtilisateurId());
        assertEquals(20L, captor.getValue().getTypeCongeId());
        assertEquals(annee, captor.getValue().getPeriode());
        assertEquals(30, captor.getValue().getSoldeAquis());
        assertEquals(0, captor.getValue().getSoldePris());
        assertEquals(30, captor.getValue().getSoldeRestant());
        verify(historiqueService).enregistrerMouvement(
                10L, "ACQUISITION_ANNUELLE", 30, 20L, null,
                "Attribution annuelle automatique pour " + annee, null);
    }

    @Test
    void neCreePasUnSoldeAnnuelDejaExistant() {
        int annee = LocalDate.now(ZONE).getYear();
        when(utilisateurRepository.findByActifTrue())
                .thenReturn(List.of(Utilisateur.builder().id(10L).actif(true).build()));
        when(typeCongeRepository.findByActifTrue())
                .thenReturn(List.of(Type_conge.builder().id(20L).actif(true).nombreJoursParAn(30).build()));
        when(soldeCongeRepository.existsByUtilisateurIdAndTypeCongeIdAndPeriode(10L, 20L, annee))
                .thenReturn(true);

        ReflectionTestUtils.setField(service, "annualBalancesEnabled", true);
        service.initialiserSoldesAnnuelsManquants();

        org.mockito.Mockito.verify(soldeCongeRepository, org.mockito.Mockito.never()).save(any(Solde_conge.class));
        org.mockito.Mockito.verifyNoInteractions(historiqueService);
    }

    @Test
    void neGenerePasDeRappelQuandAucuneDemandeAncienneNestEnAttente() {
        LocalDate cutoff = LocalDate.now(ZONE).minusDays(3);
        when(demandeCongeRepository.findByStatutAndDateCreationLessThanEqual("EN_ATTENTE", cutoff))
                .thenReturn(List.of());

        service.rappelerDemandesEnAttente();

        org.mockito.Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void envoieLeBilanMensuelAuxAdministrateursActifs() {
        LocalDate today = LocalDate.now(ZONE);
        YearMonth previousMonth = YearMonth.from(today).minusMonths(1);
        DemandeConge request = DemandeConge.builder()
                .id(1L)
                .dateCreation(previousMonth.atDay(5))
                .statut("VALIDEE")
                .build();
        Utilisateur admin = Utilisateur.builder().id(99L).role(Role.ADMIN).actif(true).build();

        when(demandeCongeRepository.findAll()).thenReturn(List.of(request));
        when(utilisateurRepository.findByRoleAndActifTrue(Role.ADMIN)).thenReturn(List.of(admin));
        when(notificationRepository.existsForRecipientAndTypeOnDate(
                99L, "RAPPORT_RH", Date.valueOf(today))).thenReturn(false);

        service.envoyerBilanMensuelRH();

        org.mockito.Mockito.verify(notificationService).creerNotification(
                eq(99L), org.mockito.ArgumentMatchers.contains("Bilan RH de " + previousMonth),
                eq("RAPPORT_RH"), eq("/dashboard/demande"));
    }
}