package com.fruvio.GestionConge.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fruvio.GestionConge.historique_mouvement.entity.Historique_mouvement;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class PdfExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generateHistoriquePdf(List<Historique_mouvement> mouvements,
            Map<Long, String> utilisateurs, Map<Long, String> typesConge, boolean vueComplete) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 32, 32, 34, 38);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            Font regular = new Font(baseFont, 8);
            Font header = new Font(baseFont, 8, Font.BOLD, new java.awt.Color(255, 255, 255));
            Font title = new Font(baseFont, 18, Font.BOLD, new java.awt.Color(15, 35, 61));
            Font subtitle = new Font(baseFont, 9, Font.NORMAL, new java.awt.Color(71, 85, 105));
            Font section = new Font(baseFont, 8, Font.BOLD, new java.awt.Color(37, 99, 235));

            writer.setPageEvent(new PageNumberEvent(regular));
            document.open();

            Paragraph institution = new Paragraph(
                    "SPAT - Société du Port à Gestion Autonome de Toamasina", section);
            institution.setSpacingAfter(5);
            document.add(institution);

            Paragraph heading = new Paragraph("Historique des mouvements de congés", title);
            heading.setSpacingAfter(7);
            document.add(heading);

            LocalDate generatedAt = LocalDate.now();
            Date firstDate = mouvements.stream().map(Historique_mouvement::getDate)
                    .filter(date -> date != null).min(Comparator.naturalOrder()).orElse(null);
            Date lastDate = mouvements.stream().map(Historique_mouvement::getDate)
                    .filter(date -> date != null).max(Comparator.naturalOrder()).orElse(null);
            String period = firstDate == null ? "Aucune date enregistrée"
                    : formatDate(firstDate) + " - " + formatDate(lastDate);

            Paragraph metadata = new Paragraph(
                    "Généré le " + generatedAt.format(DATE_FORMAT)
                            + "    |    Vue : " + (vueComplete ? "tous les mouvements autorisés" : "mes mouvements")
                            + "    |    Période : " + period,
                    subtitle);
            metadata.setSpacingAfter(16);
            document.add(metadata);

            PdfPTable table = new PdfPTable(new float[] { 10, 18, 16, 16, 9, 23, 16 });
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            table.setSplitRows(true);

            String[] columns = { "Date", "Collaborateur", "Mouvement", "Type de congé", "Quantité",
                    "Commentaire", "Effectué par" };
            for (String column : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(column, header));
                cell.setBackgroundColor(new java.awt.Color(15, 35, 61));
                cell.setBorderColor(new java.awt.Color(203, 213, 225));
                cell.setPadding(7);
                table.addCell(cell);
            }

            if (mouvements.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Aucun mouvement pour cette vue.", regular));
                empty.setColspan(columns.length);
                empty.setPadding(9);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(empty);
            } else {
                List<Historique_mouvement> ordered = mouvements.stream()
                        .sorted(Comparator.comparing(Historique_mouvement::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList();
                boolean alternate = false;
                for (Historique_mouvement mouvement : ordered) {
                    alternate = !alternate;
                    java.awt.Color background = alternate
                            ? new java.awt.Color(248, 250, 252) : java.awt.Color.WHITE;
                    String type = mouvement.getTypeMouvement() == null ? "-"
                            : mouvement.getTypeMouvement().replace('_', ' ');
                    String quantite = (mouvement.getTypeMouvement() != null
                            && mouvement.getTypeMouvement().startsWith("DEBIT") ? "-" : "+")
                            + (mouvement.getQuantite() == null ? 0 : mouvement.getQuantite()) + " j";
                    String effectuePar = mouvement.getEffectue_par() == null ? "Système"
                            : utilisateurs.getOrDefault(mouvement.getEffectue_par(),
                                    "Utilisateur #" + mouvement.getEffectue_par());

                    addDataCell(table, formatDate(mouvement.getDate()), regular, background);
                    addDataCell(table, utilisateurs.getOrDefault(mouvement.getUtilisateurId(),
                            "Utilisateur #" + mouvement.getUtilisateurId()), regular, background);
                    addDataCell(table, type, regular, background);
                    addDataCell(table, typesConge.getOrDefault(mouvement.getType_conge_id(),
                            mouvement.getType_conge_id() == null ? "-" : "Type #" + mouvement.getType_conge_id()),
                            regular, background);
                    addDataCell(table, quantite, regular, background);
                    addDataCell(table, mouvement.getCommentaire() == null ? "-" : mouvement.getCommentaire(),
                            regular, background);
                    addDataCell(table, effectuePar, regular, background);
                }
            }

            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (DocumentException | IOException exception) {
            if (document.isOpen()) {
                document.close();
            }
            throw new IllegalStateException("Impossible de générer l'export PDF de l'historique.", exception);
        }
    }

    private static String formatDate(Date date) {
        return date == null ? "-" : date.toLocalDate().format(DATE_FORMAT);
    }

    private static void addDataCell(PdfPTable table, String value, Font font, java.awt.Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "-" : value, font));
        cell.setBackgroundColor(background);
        cell.setBorderColor(new java.awt.Color(226, 232, 240));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static class PageNumberEvent extends PdfPageEventHelper {
        private final Font font;

        private PageNumberEvent(Font font) {
            this.font = font;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Phrase page = new Phrase("SPAT - Page " + writer.getPageNumber(), font);
            com.lowagie.text.pdf.ColumnText.showTextAligned(writer.getDirectContent(),
                    Element.ALIGN_RIGHT, page,
                    document.right(), document.bottom() - 18, 0);
        }
    }
}
