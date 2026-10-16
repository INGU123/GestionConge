
package com.fruvio.GestionConge.utilisateur.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class ResetPasswordRequest {

    @NotBlank(message = "Le jeton de réinitialisation est obligatoire.")
    private String token;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
    @Size(
        min = 6,
        message = "Le mot de passe doit comporter au moins 6 caractères."
    )
    private String newPassword;

    @JsonProperty("nouveauMotDePasse")
    public void setNouveauMotDePasse(String nouveauMotDePasse) {
        if (this.newPassword == null || this.newPassword.isBlank()) {
            this.newPassword = nouveauMotDePasse;
        }
    }
}
