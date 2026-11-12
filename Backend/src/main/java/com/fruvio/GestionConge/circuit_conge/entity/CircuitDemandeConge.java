package com.fruvio.GestionConge.circuit_conge.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "circuit_demande_conge")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CircuitDemandeConge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="utilisateur_id", nullable=false) private Long utilisateurId;
    @Column(name="type_conge_id", nullable=false) private Long typeCongeId;
    @Column(name="departement_id") private Long departementId;
    @Column(name="assistant_id", nullable=false) private Long assistantId;
    @Column(name="administration_personnel_id") private Long administrationPersonnelId;
    @Column(name="chef_id") private Long chefId;
    @Column(name="directeur_id") private Long directeurId;
    @Column(name="legacy_demande_id") private Long legacyDemandeId;
    @Column(name="date_debut", nullable=false) private LocalDate dateDebut;
    @Column(name="date_fin", nullable=false) private LocalDate dateFin;
    @Column(name="nombre_jours", nullable=false) private Integer nombreJours;
    @Column(length=1500) private String commentaire;
    @Column(nullable=false, length=40) private String statut;
    @Column(name="reliquat_jours") private Integer reliquatJours;
    @Column(name="reliquat_commentaire", length=1500) private String reliquatCommentaire;
    @Column(name="motif_refus", length=1500) private String motifRefus;
    @Column(name="date_creation", nullable=false) private LocalDateTime dateCreation;
    @Column(name="date_reliquat") private LocalDateTime dateReliquat;
    @Column(name="date_soumission_chef") private LocalDateTime dateSoumissionChef;
    @Column(name="date_transmission_directeur") private LocalDateTime dateTransmissionDirecteur;
    @Column(name="date_decision") private LocalDateTime dateDecision;

    @Transient private String utilisateurNom;
    @Transient private String utilisateurPrenom;
    @Transient private String utilisateurMatricule;
}
