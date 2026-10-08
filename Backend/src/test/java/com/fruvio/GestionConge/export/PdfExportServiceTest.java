package com.fruvio.GestionConge.export;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fruvio.GestionConge.historique_mouvement.entity.Historique_mouvement;

class PdfExportServiceTest {

    private final PdfExportService pdfExportService = new PdfExportService();

    @Test
    void generatesPdfFromHistoryMovements() {
        Historique_mouvement movement = Historique_mouvement.builder()
                .id(12L)
                .utilisateurId(7L)
                .typeMouvement("DEBIT_CONGE")
                .quantite(3)
                .type_conge_id(2L)
                .commentaire("Congé annuel")
                .effectue_par(4L)
                .date(Date.valueOf("2026-10-08"))
                .build();

        byte[] pdf = pdfExportService.generateHistoriquePdf(
                List.of(movement),
                Map.of(7L, "Agent SPAT", 4L, "Responsable"),
                Map.of(2L, "Congé annuel"),
                true);

        assertTrue(pdf.length > 0);
        assertTrue(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"));
    }
}
