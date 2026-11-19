package com.fruvio.GestionConge.employe.dto;

import com.fruvio.GestionConge.employe.entity.Employe;

public record EmployeLookupResponse(
        Long id,
        String matricule,
        String nom,
        String prenom,
        String fonction
) {
    public static EmployeLookupResponse depuis(Employe e) {
        return new EmployeLookupResponse(e.getId(), e.getMatricule(), e.getNom(),
                e.getPrenom(), e.getFonction());
    }
}
