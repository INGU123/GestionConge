package com.fruvio.GestionConge.employe.service;

import java.util.Locale;

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

    private void appliquer(Employe employe, EmployeRequest request, String matricule) {
        employe.setMatricule(matricule);
        employe.setNom(request.nom().trim());
        employe.setPrenom(request.prenom().trim());
        employe.setNumeroCin(texteOuNull(request.numeroCin()));
        employe.setDateNaissance(request.dateNaissance());
        employe.setTelephone(texteOuNull(request.telephone()));
        employe.setAdresse(texteOuNull(request.adresse()));
        employe.setFonction(texteOuNull(request.fonction()));
        employe.setTypeContrat(texteOuNull(request.typeContrat()));
        employe.setEmail(texteOuNull(request.email()) == null ? null : request.email().trim().toLowerCase(Locale.ROOT));
        employe.setDateEmbauche(request.dateEmbauche());
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
