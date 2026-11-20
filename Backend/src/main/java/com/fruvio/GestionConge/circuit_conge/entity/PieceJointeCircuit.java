package com.fruvio.GestionConge.circuit_conge.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "piece_jointe_circuit", uniqueConstraints = {
    @UniqueConstraint(name = "uq_piece_jointe_circuit_dossier", columnNames = "circuit_demande_conge_id")
})
public class PieceJointeCircuit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "circuit_demande_conge_id", nullable = false)
    private Long circuitDemandeCongeId;

    @Column(name = "nom_original", nullable = false, length = 255)
    private String nomOriginal;

    @Column(name = "nom_stockage", nullable = false, length = 100)
    private String nomStockage;

    @Column(name = "type_contenu", nullable = false, length = 100)
    private String typeContenu;

    @Column(name = "taille_octets", nullable = false)
    private long tailleOctets;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Column(name = "date_ajout", nullable = false)
    private LocalDateTime dateAjout;

    public Long getId() { return id; }
    public Long getCircuitDemandeCongeId() { return circuitDemandeCongeId; }
    public void setCircuitDemandeCongeId(Long value) { this.circuitDemandeCongeId = value; }
    public String getNomOriginal() { return nomOriginal; }
    public void setNomOriginal(String value) { this.nomOriginal = value; }
    public String getNomStockage() { return nomStockage; }
    public void setNomStockage(String value) { this.nomStockage = value; }
    public String getTypeContenu() { return typeContenu; }
    public void setTypeContenu(String value) { this.typeContenu = value; }
    public long getTailleOctets() { return tailleOctets; }
    public void setTailleOctets(long value) { this.tailleOctets = value; }
    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long value) { this.uploaderId = value; }
    public LocalDateTime getDateAjout() { return dateAjout; }
    public void setDateAjout(LocalDateTime value) { this.dateAjout = value; }
}
