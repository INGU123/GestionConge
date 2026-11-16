package com.fruvio.GestionConge.solde_conge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;
import com.fruvio.GestionConge.historique_mouvement.service.Historique_mouvementService;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.entity.Role;

@ExtendWith(MockitoExtension.class)
class Solde_congeServiceTest {

    @Mock Solde_congeRepository soldeCongeRepository;
    @Mock Type_congeRepository typeCongeRepository;
    @Mock UtilisateurRepository utilisateurRepository;
    @Mock Historique_mouvementService historiqueMouvementService;

    @InjectMocks Solde_congeService service;

    @Test
    void consulterSoldesNeDoitPasCreerDeSoldes() {
        Utilisateur employe = new Utilisateur();
        employe.setId(1L);
        employe.setRole(Role.EMPLOYE);

        when(soldeCongeRepository.findByUtilisateurId(1L)).thenReturn(List.of());
        when(typeCongeRepository.findAll()).thenReturn(List.of());

        assertEquals(List.of(), service.getSoldesParUtilisateur(1L, employe));

        verify(soldeCongeRepository).findByUtilisateurId(1L);
        verify(soldeCongeRepository, never()).save(any());
    }

    @Test
    void neDoitPasAfficherLesSoldesDesTypesSupprimes() {
        Utilisateur employe = new Utilisateur();
        employe.setId(1L);
        employe.setRole(Role.EMPLOYE);

        Solde_conge ancienSolde = Solde_conge.builder()
                .id(1L).utilisateurId(1L).typeCongeId(1L).periode(2026)
                .soldeAquis(30).soldePris(30).soldeRestant(0).build();
        Solde_conge soldeActuel = Solde_conge.builder()
                .id(2L).utilisateurId(1L).typeCongeId(6L).periode(2026)
                .soldeAquis(30).soldePris(0).soldeRestant(30).build();
        Type_conge typeActuel = Type_conge.builder().id(6L).code("CA").libelle("Congé annuel").build();

        when(soldeCongeRepository.findByUtilisateurId(1L)).thenReturn(List.of(ancienSolde, soldeActuel));
        when(typeCongeRepository.findAll()).thenReturn(List.of(typeActuel));

        assertEquals(List.of(soldeActuel), service.getSoldesParUtilisateur(1L, employe));
        verify(soldeCongeRepository, never()).delete(any());
    }
    @Test
    void ajustementSoldeRequiresActiveAdministrator() {
        when(utilisateurRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(
                Utilisateur.builder().id(9L).role(Role.MANAGER).actif(true).build()));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.ajusterSolde(4L, 12, 9L, "Correction administrative"));

        verify(soldeCongeRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void ajustementSoldePreservesConsumedDaysAndLocksTargetBalance() {
        Utilisateur admin = Utilisateur.builder().id(1L).role(Role.ADMIN).actif(true).build();
        Solde_conge solde = Solde_conge.builder().id(7L).utilisateurId(2L).typeCongeId(3L)
                .soldeAquis(30).soldePris(5).soldeRestant(25).build();
        when(utilisateurRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(soldeCongeRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(solde));
        when(soldeCongeRepository.save(solde)).thenReturn(solde);

        Solde_conge updated = service.ajusterSolde(7L, 20, 1L, "Correction justifiée du reliquat");

        assertEquals(25, updated.getSoldeAquis());
        assertEquals(5, updated.getSoldePris());
        assertEquals(20, updated.getSoldeRestant());
        verify(soldeCongeRepository).findByIdForUpdate(7L);
        verify(historiqueMouvementService).enregistrerMouvement(
                2L, "AJUSTEMENT_SOLDE", 5, 3L, null,
                "Ajustement manuel du solde : 25 -> 20 | Motif : Correction justifiée du reliquat", 1L);
    }

}
