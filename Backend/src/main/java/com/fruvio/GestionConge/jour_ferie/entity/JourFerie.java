package com.fruvio.GestionConge.jour_ferie.entity;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

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
@Entity
@Table(name = "jour_ferie")
public class JourFerie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private Integer annee;

    @Builder.Default
    @Column(nullable = false)
    private Boolean actif = true;
}
