package com.fruvio.GestionConge.confirmation_reliquat.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "confirmation_reliquat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmationReliquat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "circuit_demande_conge_id", nullable = false)
    private Long circuitDemandeCongeId;

    @Column(name = "employe_id")
    private Long employeId;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(name = "nombre_jours_confirme", nullable = false)
    private Integer nombreJoursConfirme;

    @Column(name = "periode_reference", nullable = false, length = 100)
    private String periodeReference;

    @Column(name = "confirme_par", nullable = false)
    private Long confirmePar;

    @Column(name = "date_confirmation", nullable = false, updatable = false)
    private LocalDateTime dateConfirmation;

    @Column(name = "reference_confirmation", nullable = false, unique = true, length = 100, updatable = false)
    private String referenceConfirmation;

    @Column(length = 500, updatable = false)
    private String justificatif;

    @Column(length = 1500, updatable = false)
    private String observation;

    @Column(name = "remplace_confirmation_id")
    private Long remplaceConfirmationId;

    @PrePersist
    void definirDateConfirmation() {
        if (dateConfirmation == null) dateConfirmation = LocalDateTime.now();
    }
}
