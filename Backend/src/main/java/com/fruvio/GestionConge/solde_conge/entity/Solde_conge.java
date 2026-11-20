package com.fruvio.GestionConge.solde_conge.entity;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.math.RoundingMode;
import jakarta.persistence.Transient;
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

    // Pour les soldes du référentiel employé, les colonnes entières stockent des demi-journées.
    // Les soldes historiques liés aux comptes utilisateurs restent exprimés en jours entiers.
    @Transient
    private BigDecimal soldeAquisJours;

    @Transient
    private BigDecimal soldePrisJours;

    @Transient
    private BigDecimal soldeRestantJours;

    public void exposerSoldesEmployeEnJours() {
        this.soldeAquisJours = BigDecimal.valueOf(soldeAquis == null ? 0 : soldeAquis)
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);
        this.soldePrisJours = BigDecimal.valueOf(soldePris == null ? 0 : soldePris)
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);
        this.soldeRestantJours = BigDecimal.valueOf(soldeRestant == null ? 0 : soldeRestant)
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);
    }

    // Alias pour compatibilité
    public Integer getSoldeAcquis() {
        return soldeAquis;
    }

    public void setSoldeAcquis(Integer soldeAcquis) {
        this.soldeAquis = soldeAcquis;
    }
}
