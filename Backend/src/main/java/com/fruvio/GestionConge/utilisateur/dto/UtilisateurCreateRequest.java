package com.fruvio.GestionConge.utilisateur.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fruvio.GestionConge.utilisateur.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class UtilisateurCreateRequest {

    @NotBlank(message = "Le matricule est obligatoire.")
    @Size(min = 2, max = 50, message = "Le matricule doit contenir entre 2 et 50 caractères.")
    private String matricule;

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(min = 1, max = 100, message = "Le nom doit contenir au maximum 100 caractères.")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(min = 1, max = 100, message = "Le prénom doit contenir au maximum 100 caractères.")
    private String prenom;

    @JsonProperty("numero_cin")
    @Size(max = 30, message = "Le numéro CIN ne peut pas dépasser 30 caractères.")
    private String numeroCin;

    @JsonProperty("date_naissance")
    @Past(message = "La date de naissance doit être dans le passé.")
    private LocalDate dateNaissance;

    @Size(max = 30, message = "Le téléphone ne peut pas dépasser 30 caractères.")
    private String telephone;

    @Size(max = 500, message = "L'adresse ne peut pas dépasser 500 caractères.")
    private String adresse;

    @Size(max = 100, message = "La fonction ne peut pas dépasser 100 caractères.")
    private String fonction;

    @JsonProperty("type_contrat")
    @Size(max = 80, message = "Le type de contrat ne peut pas dépasser 80 caractères.")
    private String typeContrat;

    @NotBlank(message = "L'adresse e-mail est obligatoire.")
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

    private LocalDate date_embauche;

    @Builder.Default
    private Boolean actif = true;

    @JsonProperty("mot_de_pass")
    public void setMotDePass(String motDePass) {
        if (this.password == null || this.password.isBlank()) {
            this.password = motDePass;
        }
    }
}
