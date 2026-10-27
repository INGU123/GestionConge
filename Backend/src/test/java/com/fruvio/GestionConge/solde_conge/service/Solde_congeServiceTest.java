package com.fruvio.GestionConge.solde_conge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
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

        when(soldeCongeRepository.findByUtilisateurId(1L))
                .thenReturn(List.of());

        assertEquals(List.of(),
                service.getSoldesParUtilisateur(1L, employe));

        verify(soldeCongeRepository).findByUtilisateurId(1L);
        verify(soldeCongeRepository, never()).save(any());
        verifyNoInteractions(typeCongeRepository);
    }
}
