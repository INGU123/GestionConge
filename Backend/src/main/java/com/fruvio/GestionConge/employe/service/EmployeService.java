package com.fruvio.GestionConge.employe.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fruvio.GestionConge.employe.dto.EmployeLookupResponse;
import com.fruvio.GestionConge.employe.entity.Employe;
import com.fruvio.GestionConge.employe.repository.EmployeRepository;
import com.fruvio.GestionConge.service_conge.entity.Services;
import com.fruvio.GestionConge.service_conge.repository.Service_congeRepository;
import com.fruvio.GestionConge.departement.entity.Departement;
import com.fruvio.GestionConge.departement.repository.DepartementRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;
@Service
public class EmployeService {
    private final EmployeRepository employes;
    private final UtilisateurRepository utilisateurs;
    private final Service_congeRepository services;
    private final DepartementRepository departements;
    public EmployeService(EmployeRepository employes, UtilisateurRepository utilisateurs,
            Service_congeRepository services, DepartementRepository departements) {
        this.employes=employes; this.utilisateurs=utilisateurs; this.services=services; this.departements=departements;
    }
    @Transactional(readOnly=true)
    public EmployeLookupResponse rechercherPourAssistant(String matriculeSaisi) {
        String matricule=normaliserMatricule(matriculeSaisi);
        Employe e=employes.findByMatricule(matricule).filter(Employe::isActif).orElse(null);
        if(e!=null) {
            Services s=services.findById(e.getServiceId()).orElseThrow(() -> new IllegalStateException("Le service associé à cet employé est introuvable."));
            if(s.getDepartementId()==null) throw new IllegalStateException("Le service de cet employé n'est associé à aucun département.");
            Departement d=departements.findById(s.getDepartementId()).orElseThrow(() -> new IllegalStateException("Le département associé à cet employé est introuvable."));
            return EmployeLookupResponse.depuis(e,s.getNom(),d.getId(),d.getNom());
        }
        Utilisateur u=utilisateurs.findByMatricule(matricule).filter(Utilisateur::isEnabled)
                .orElseThrow(() -> new IllegalArgumentException("Aucun employé ou collaborateur actif trouvé pour ce matricule."));
        if (u.getRole() == null)
            throw new IllegalStateException("Le rôle du collaborateur doit être renseigné.");
        Departement d;
        if (u.getRole() == Role.CHEF_DEPARTEMENT) {
            d = departements.findByChefIdAndActifTrue(u.getId())
                    .orElseThrow(() -> new IllegalStateException("Aucun département actif n'est rattaché à ce chef."));
        } else if (u.getRole() == Role.DIRECTEUR) {
            d = departements.findByDirecteurIdAndActifTrue(u.getId())
                    .orElseThrow(() -> new IllegalStateException("Aucun département actif n'est rattaché à ce directeur."));
        } else {
            if(u.getService_id()==null) throw new IllegalStateException("Aucun service n'est associé à ce collaborateur.");
            Services s=services.findById(u.getService_id()).orElseThrow(() -> new IllegalStateException("Le service associé à ce collaborateur est introuvable."));
            if(s.getDepartementId()==null) throw new IllegalStateException("Le service de ce collaborateur n'est associé à aucun département.");
            d=departements.findById(s.getDepartementId()).orElseThrow(() -> new IllegalStateException("Le département associé à ce collaborateur est introuvable."));
        }
        String serviceNom=u.getService_id()==null?null:services.findById(u.getService_id()).map(Services::getNom).orElse(null);
        return EmployeLookupResponse.depuis(u,serviceNom,d.getId(),d.getNom());
    }
    private String normaliserMatricule(String value) {
        if(value==null||value.isBlank()) throw new IllegalArgumentException("Le matricule est obligatoire.");
        String m=value.trim();
        if(!m.matches("[0-9]+")) throw new IllegalArgumentException("Le matricule doit contenir uniquement des chiffres.");
        return m;
    }
}
