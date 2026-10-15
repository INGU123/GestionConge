package com.fruvio.GestionConge.utilisateur.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ForgotPasswordRequest {
    @NotBlank(message = "Le numéro matricule est obligatoire.")
    private String matricule;
}
