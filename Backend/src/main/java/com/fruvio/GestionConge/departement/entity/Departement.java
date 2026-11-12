package com.fruvio.GestionConge.departement.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Departement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nom;

    @Column(name = "chef_id", nullable = false, unique = true)
    private Long chefId;

    @Column(name = "directeur_id", nullable = false, unique = true)
    private Long directeurId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
