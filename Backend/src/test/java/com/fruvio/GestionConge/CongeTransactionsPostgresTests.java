package com.fruvio.GestionConge;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fruvio.GestionConge.demande_conge.entity.DemandeConge;
import com.fruvio.GestionConge.demande_conge.repository.DemandeCongeRepository;
import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;

/**
 * Tests PostgreSQL reels : lancer UNIQUEMENT sur une base de test isolee.
 * Ces tests creent leurs propres lignes puis les suppriment.
 */
@SpringBootTest
class CongeTransactionsPostgresTests {

    @Autowired private Solde_congeRepository soldes;
    @Autowired private DemandeCongeRepository demandes;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void verrouSoldeEtRollbackReel() {
        Solde_conge solde = soldes.saveAndFlush(Solde_conge.builder()
                .utilisateurId(-987654321L).typeCongeId(-987654321L)
                .periode(2099).soldeAquis(10).soldePris(2).soldeRestant(8).build());
        try {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
                Solde_conge verrouille = soldes.verrouillerSolde(-987654321L, -987654321L, 2099)
                        .orElseThrow();
                verrouille.setSoldePris(9);
                verrouille.setSoldeRestant(1);
                soldes.saveAndFlush(verrouille);
                throw new IllegalStateException("Forcer le rollback");
            }));
            Solde_conge apres = soldes.findById(solde.getId()).orElseThrow();
            assertEquals(2, apres.getSoldePris());
            assertEquals(8, apres.getSoldeRestant());
        } finally {
            soldes.deleteById(solde.getId());
        }
    }

    @Test
    void verrouDemandeEtRollbackReel() {
        DemandeConge demande = demandes.saveAndFlush(DemandeConge.builder()
                .utilisateurId(-987654321L).typeCongeId(-987654321L)
                .dateDebut(LocalDate.of(2099, 1, 5))
                .dateFin(LocalDate.of(2099, 1, 6))
                .statut("EN_ATTENTE").build());
        try {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
                DemandeConge verrouille = demandes.verrouillerDemande(demande.getId()).orElseThrow();
                verrouille.setStatut("VALIDEE");
                demandes.saveAndFlush(verrouille);
                throw new IllegalStateException("Forcer le rollback");
            }));
            assertEquals("EN_ATTENTE", demandes.findById(demande.getId()).orElseThrow().getStatut());
        } finally {
            demandes.deleteById(demande.getId());
        }
    }
}
