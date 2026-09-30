package com.fruvio.GestionConge.notifications_conge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Date;

@Entity
@Table(name = "notification_conge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long utilisateurId; // correspond à @RequestParam utilisateurId
    private String contenu; // correspond à @RequestParam contenu
    private String type; // correspond à @RequestParam type
    private boolean lue; // correspond à @RequestParam lue
    private Date date_envoi; // correspond à @RequestParam date_envoi
    private Date date_lecture; // correspond à @RequestParam date_lecture
    private String lien; // correspond à @RequestParam lien
}
