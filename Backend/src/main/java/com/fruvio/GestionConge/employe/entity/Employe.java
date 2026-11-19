package com.fruvio.GestionConge.employe.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "employe", indexes = {
    @Index(name = "idx_employe_matricule", columnList = "matricule"),
    @Index(name = "idx_employe_nom_prenom", columnList = "nom, prenom")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String matricule;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(name = "numero_cin", unique = true, length = 30)
    private String numeroCin;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(length = 30)
    private String telephone;

    @Column(length = 500)
    private String adresse;

    @Column(length = 150)
    private String fonction;

    @Column(name = "type_contrat", length = 80)
    private String typeContrat;

    @Column(length = 254)
    private String email;

    @Column(name = "date_embauche")
    private LocalDate dateEmbauche;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void initialiserDateCreation() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
    }
}
