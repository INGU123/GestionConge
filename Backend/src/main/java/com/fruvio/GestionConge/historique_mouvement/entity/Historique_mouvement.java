package com.fruvio.GestionConge.historique_mouvement.entity;

import java.sql.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "historique_mouvement")
@Entity
public class Historique_mouvement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // utilisateurId reste renseigné pour les historiques antérieurs.
    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(name = "employe_id")
    private Long employeId;

    @Column(name = "type_mouvement")
    private String typeMouvement;

    @Builder.Default
    private Integer quantite = 0;

    @Column(name = "type_conge_id")
    private Long type_conge_id;

    @Column(name = "demande_id")
    private Long demande_id;

    private String commentaire;

    @Column(name = "effectue_par")
    private Long effectue_par;

    private Date date;
}
