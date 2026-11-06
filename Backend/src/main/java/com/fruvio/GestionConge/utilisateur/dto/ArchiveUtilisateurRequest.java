package com.fruvio.GestionConge.utilisateur.dto;

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
public class ArchiveUtilisateurRequest {

    @Size(max = 1000, message = "Le motif d'archivage ne peut pas dépasser 1000 caractères.")
    private String motif;
}
