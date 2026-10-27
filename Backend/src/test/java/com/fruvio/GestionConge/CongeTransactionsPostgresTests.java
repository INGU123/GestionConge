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

    @Test
    void deuxTransactionsNePeuventPasDebiterLeMemeSoldeEnParallele() throws Exception {
        Solde_conge initial = soldes.saveAndFlush(Solde_conge.builder()
                .utilisateurId(-987654322L).typeCongeId(-987654322L)
                .periode(2098).soldeAquis(10).soldePris(0).soldeRestant(10).build());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch premierVerrouAcquis = new CountDownLatch(1);
        CountDownLatch libererPremier = new CountDownLatch(1);
        CountDownLatch deuxiemeDemarre = new CountDownLatch(1);
        try {
            Future<?> premier = executor.submit(() -> {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    Solde_conge solde = soldes.verrouillerSolde(-987654322L, -987654322L, 2098)
                            .orElseThrow();
                    premierVerrouAcquis.countDown();
                    try {
                        if (!libererPremier.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timeout du premier verrou");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                    solde.setSoldePris(7);
                    solde.setSoldeRestant(3);
                    soldes.saveAndFlush(solde);
                });
            });
            assertTrue(premierVerrouAcquis.await(10, TimeUnit.SECONDS));
            Future<Integer> deuxieme = executor.submit(() ->
                new TransactionTemplate(transactionManager).execute(status -> {
                    deuxiemeDemarre.countDown();
                    Solde_conge solde = soldes.verrouillerSolde(-987654322L, -987654322L, 2098)
                            .orElseThrow();
                    // Une fois le premier debit valide, le second doit voir le solde actualise.
                    return solde.getSoldeRestant();
                })
            );
            assertTrue(deuxiemeDemarre.await(10, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> deuxieme.get(400, TimeUnit.MILLISECONDS),
                    "La seconde transaction ne doit pas franchir le verrou tant que la premiere le detient");
            libererPremier.countDown();
            premier.get(10, TimeUnit.SECONDS);
            assertEquals(3, deuxieme.get(10, TimeUnit.SECONDS));
            assertEquals(3, soldes.findById(initial.getId()).orElseThrow().getSoldeRestant());
        } finally {
            libererPremier.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
            soldes.deleteById(initial.getId());
        }
    }
}
