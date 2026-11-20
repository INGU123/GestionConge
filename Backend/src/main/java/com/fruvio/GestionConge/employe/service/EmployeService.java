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

@Service
public class EmployeService {
    private final EmployeRepository employes;
    private final Service_congeRepository services;
    private final DepartementRepository departements;

    public EmployeService(EmployeRepository employes, Service_congeRepository services,
            DepartementRepository departements) {
        this.employes = employes;
        this.services = services;
        this.departements = departements;
    }

    @Transactional(readOnly = true)
    public EmployeLookupResponse rechercherPourAssistant(String matriculeSaisi) {
        String matricule = normaliserMatricule(matriculeSaisi);
        Employe employe = employes.findByMatricule(matricule)
                .filter(Employe::isActif)
                .orElseThrow(() -> new IllegalArgumentException("Aucun employé actif trouvé pour ce matricule."));
        Services service = services.findById(employe.getServiceId())
                .orElseThrow(() -> new IllegalStateException("Le service associé à cet employé est introuvable."));
        if (service.getDepartementId() == null)
            throw new IllegalStateException("Le service de cet employé n'est associé à aucun département.");
        Departement departement = departements.findById(service.getDepartementId())
                .orElseThrow(() -> new IllegalStateException("Le département associé à cet employé est introuvable."));
        return EmployeLookupResponse.depuis(employe, service.getNom(), departement.getId(), departement.getNom());
    }

    private String normaliserMatricule(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le matricule est obligatoire.");
        }
        String matricule = value.trim();
        if (!matricule.matches("[0-9]+")) throw new IllegalArgumentException("Le matricule doit contenir uniquement des chiffres.");
        return matricule;
    }

    private String texteOuNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
