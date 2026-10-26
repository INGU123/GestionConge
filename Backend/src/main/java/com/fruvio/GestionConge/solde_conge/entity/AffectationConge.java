package com.fruvio.GestionConge.solde_conge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Allocation d'une demande approuvée à un millésime de droits.
 * Permet de restituer exactement les jours lors de l'annulation.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "affectation_conge")
public class AffectationConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demande_conge_id", nullable = false)
    private Long demandeCongeId;

    @Column(name = "solde_conge_id", nullable = false)
    private Long soldeCongeId;

    @Column(name = "nombre_jours", nullable = false)
    private Integer nombreJours;
}
