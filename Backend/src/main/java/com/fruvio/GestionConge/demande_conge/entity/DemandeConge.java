package com.fruvio.GestionConge.demande_conge.entity;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "demande_conge")
public class DemandeConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(name = "type_de_conge_id")
    private Long typeCongeId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebut;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFin;

    @Column(name = "nombre_jours")
    private Integer nombreJours;

    private String commentaire;

    private String statut;

    @Column(name = "motif_refus")
    private String motifRefus;

    @Column(name = "validee_par")
    private Long valideePar;

    @Column(name = "date_validation")
    private LocalDate dateValidation;

    @Column(name = "date_creation")
    private LocalDate dateCreation;

    @Transient private String utilisateurNom;
    @Transient private String utilisateurPrenom;
    @Transient private String utilisateurMatricule;
}
