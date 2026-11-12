package com.fruvio.GestionConge.utilisateur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {

    @NotBlank(message = "Le matricule est obligatoire.")
    @Size(max = 50, message = "Le matricule est trop long.")
    @Pattern(regexp = "^[0-9]+$", message = "Le matricule doit contenir uniquement des chiffres.")
    private String matricule;
}
