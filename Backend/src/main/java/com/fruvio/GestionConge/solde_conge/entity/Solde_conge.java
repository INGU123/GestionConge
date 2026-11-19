package com.fruvio.GestionConge.solde_conge.entity;

import java.sql.Timestamp;
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
@Table(name = "solde_conge")
@Entity
public class Solde_conge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Un solde appartient soit à un compte historique, soit à un employé du référentiel.
    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(name = "employe_id")
    private Long employeId;

    @Column(name = "type_conge_id")
    private Long typeCongeId;

    @Builder.Default
    @Column(name = "solde_aquis")
    private Integer soldeAquis = 0;

    @Builder.Default
    @Column(name = "solde_pris")
    private Integer soldePris = 0;

    @Builder.Default
    @Column(name = "solde_restant")
    private Integer soldeRestant = 0;

    private int periode;

    private Timestamp date_maj;

    // Alias pour compatibilité
    public Integer getSoldeAcquis() {
        return soldeAquis;
    }

    public void setSoldeAcquis(Integer soldeAcquis) {
        this.soldeAquis = soldeAcquis;
    }
}
