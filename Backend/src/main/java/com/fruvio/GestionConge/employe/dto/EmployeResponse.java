package com.fruvio.GestionConge.employe.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fruvio.GestionConge.employe.entity.Employe;

public record EmployeResponse(
        Long id,
        String matricule,
        String nom,
        String prenom,
        String numeroCin,
        LocalDate dateNaissance,
        String telephone,
        String adresse,
        String fonction,
        String typeContrat,
        String email,
        LocalDate dateEmbauche,
        boolean actif,
        LocalDateTime dateCreation
) {
    public static EmployeResponse depuis(Employe e) {
        return new EmployeResponse(e.getId(), e.getMatricule(), e.getNom(), e.getPrenom(),
                e.getNumeroCin(), e.getDateNaissance(), e.getTelephone(), e.getAdresse(),
                e.getFonction(), e.getTypeContrat(), e.getEmail(), e.getDateEmbauche(),
                e.isActif(), e.getDateCreation());
    }
}
