package com.fruvio.GestionConge.utilisateur.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmailPasswordResetRequest {
    @NotBlank(message = "L'adresse e-mail est obligatoire.")
    @Email(message = "Format d'adresse e-mail invalide.")
    @Size(max = 254, message = "L'adresse e-mail est trop longue.")
    private String email;
}
