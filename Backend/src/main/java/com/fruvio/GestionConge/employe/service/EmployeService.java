package com.fruvio.GestionConge.employe.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fruvio.GestionConge.employe.dto.EmployeLookupResponse;
import com.fruvio.GestionConge.employe.entity.Employe;
import com.fruvio.GestionConge.employe.repository.EmployeRepository;

@Service
public class EmployeService {
    private final EmployeRepository employes;

    public EmployeService(EmployeRepository employes) {
        this.employes = employes;
    }

    @Transactional(readOnly = true)
    public EmployeLookupResponse rechercherPourAssistant(String matriculeSaisi) {
        String matricule = normaliserMatricule(matriculeSaisi);
        Employe employe = employes.findByMatricule(matricule)
                .filter(Employe::isActif)
                .orElseThrow(() -> new IllegalArgumentException("Aucun employé actif trouvé pour ce matricule."));
        return EmployeLookupResponse.depuis(employe);
    }

    private String normaliserMatricule(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le matricule est obligatoire.");
        }
        return value.trim();
    }

    private String texteOuNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
