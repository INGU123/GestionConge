package com.fruvio.GestionConge.utilisateur.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fruvio.GestionConge.utilisateur.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
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
public class UtilisateurUpdateRequest {

    @NotNull(message = "L'ID de l'utilisateur est obligatoire.")
    private Long id;

    private String matricule;

    @Size(min = 1, max = 100, message = "Le nom ne peut pas dépasser 100 caractères.")
    private String nom;

    @Size(min = 1, max = 100, message = "Le prénom ne peut pas dépasser 100 caractères.")
    private String prenom;

    @JsonProperty("numero_cin")
    @Size(max = 30, message = "Le numéro CIN ne peut pas dépasser 30 caractères.")
    private String numeroCin;

    @JsonProperty("date_naissance")
    @Past(message = "La date de naissance doit être dans le passé.")
    private java.time.LocalDate dateNaissance;

    @Size(max = 30, message = "Le téléphone ne peut pas dépasser 30 caractères.")
    private String telephone;

    @Size(max = 500, message = "L'adresse ne peut pas dépasser 500 caractères.")
    private String adresse;

    @Size(max = 100, message = "La fonction ne peut pas dépasser 100 caractères.")
    private String fonction;

    @JsonProperty("type_contrat")
    @Size(max = 80, message = "Le type de contrat ne peut pas dépasser 80 caractères.")
    private String typeContrat;

    @Email(message = "Format d'adresse e-mail invalide.")
    @Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "Format d'adresse e-mail invalide."
    )
    private String email;

    @Size(min = 6, message = "Le mot de passe doit comporter au moins 6 caractères.")
    private String password;

    private Role role;

    private Long service_id;

    private Long manager_id;

    private Boolean actif;

    @JsonProperty("date_embauche")
    private java.time.LocalDate dateEmbauche;

    @JsonProperty("mot_de_pass")
    public void setMotDePass(String motDePass) {
        if (this.password == null || this.password.isBlank()) {
            this.password = motDePass;
        }
    }
}
