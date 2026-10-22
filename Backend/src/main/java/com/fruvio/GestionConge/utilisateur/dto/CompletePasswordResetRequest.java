package com.fruvio.GestionConge.utilisateur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompletePasswordResetRequest {
    @NotBlank(message = "Le matricule est obligatoire.")
    private String matricule;

    @NotBlank(message = "L'ancien mot de passe est obligatoire.")
    private String ancienMotDePasse;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
    @Size(min = 6, message = "Le nouveau mot de passe doit comporter au moins 6 caractères.")
    private String nouveauMotDePasse;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire.")
    private String confirmation;
}
