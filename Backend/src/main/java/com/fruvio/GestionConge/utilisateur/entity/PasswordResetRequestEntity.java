package com.fruvio.GestionConge.utilisateur.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "password_reset_request")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetRequestEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
    @Column(nullable = false)
    private String statut;
    @Column(nullable = false)
    private LocalDateTime dateDemande;
    private LocalDateTime dateTraitement;
}
