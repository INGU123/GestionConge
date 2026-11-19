package com.fruvio.GestionConge.employe.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record EmployeRequest(
        @NotBlank @Size(max = 50) String matricule,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @Size(max = 30) String numeroCin,
        @PastOrPresent LocalDate dateNaissance,
        @Size(max = 30) String telephone,
        @Size(max = 500) String adresse,
        @Size(max = 150) String fonction,
        @Size(max = 80) String typeContrat,
        @Email @Size(max = 254) String email,
        LocalDate dateEmbauche
) {}
