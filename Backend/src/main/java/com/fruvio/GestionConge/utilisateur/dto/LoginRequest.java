package com.fruvio.GestionConge.utilisateur.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
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
public class LoginRequest {

    @Pattern(regexp = "^[0-9]+$", message = "Le matricule doit contenir uniquement des chiffres.")
    private String matricule;
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    @JsonProperty("mot_de_pass")
    public void setMotDePass(String motDePass) {
        if (this.password == null || this.password.isBlank()) {
            this.password = motDePass;
        }
    }

    @JsonProperty("motDePass")
    public void setMotDePassCamel(String motDePass) {
        if (this.password == null || this.password.isBlank()) {
            this.password = motDePass;
        }
    }

    public String getIdentifier() {
        if (matricule != null && !matricule.trim().isEmpty()) {
            return matricule.trim();
        }
        return null;
    }
}
