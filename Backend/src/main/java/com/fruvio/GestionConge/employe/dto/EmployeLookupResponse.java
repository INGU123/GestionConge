package com.fruvio.GestionConge.employe.dto;
import java.time.LocalDate;
import com.fruvio.GestionConge.employe.entity.Employe;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
public record EmployeLookupResponse(Long id, String matricule, String nom, String prenom, String fonction,
        Long serviceId, String serviceNom, Long departementId, String departementNom,
        LocalDate dateEmbauche, String typeCible, String role) {
    public static EmployeLookupResponse depuis(Employe e, String serviceNom, Long departementId, String departementNom) {
        return new EmployeLookupResponse(e.getId(), e.getMatricule(), e.getNom(), e.getPrenom(), e.getFonction(),
                e.getServiceId(), serviceNom, departementId, departementNom, e.getDateEmbauche(), "EMPLOYE", null);
    }
    public static EmployeLookupResponse depuis(Utilisateur u, String serviceNom, Long departementId, String departementNom) {
        return new EmployeLookupResponse(u.getId(), u.getMatricule(), u.getNom(), u.getPrenom(), u.getFonction(),
                u.getService_id(), serviceNom, departementId, departementNom, u.getDate_embauche(),
                "COLLABORATEUR", u.getRole() == null ? null : u.getRole().name());
    }
}
