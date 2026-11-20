package com.fruvio.GestionConge.employe.dto;

import com.fruvio.GestionConge.employe.entity.Employe;

public record EmployeLookupResponse(
        Long id,
        String matricule,
        String nom,
        String prenom,
        String fonction,
        Long serviceId,
        String serviceNom,
        Long departementId,
        String departementNom
) {
    public static EmployeLookupResponse depuis(Employe e, String serviceNom, Long departementId, String departementNom) {
        return new EmployeLookupResponse(e.getId(), e.getMatricule(), e.getNom(),
                e.getPrenom(), e.getFonction(), e.getServiceId(), serviceNom, departementId, departementNom);
    }
}
