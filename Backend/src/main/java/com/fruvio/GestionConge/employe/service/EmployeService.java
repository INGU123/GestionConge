package com.fruvio.GestionConge.employe.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fruvio.GestionConge.employe.dto.EmployeLookupResponse;
import com.fruvio.GestionConge.employe.dto.EmployeRequest;
import com.fruvio.GestionConge.employe.dto.EmployeResponse;
import com.fruvio.GestionConge.employe.entity.Employe;
import com.fruvio.GestionConge.employe.repository.EmployeRepository;

@Service
public class EmployeService {
    private final EmployeRepository employes;

    public EmployeService(EmployeRepository employes) {
        this.employes = employes;
    }

    @Transactional(readOnly = true)
    public List<EmployeResponse> lister() {
        return employes.findAllByOrderByNomAscPrenomAsc().stream()
                .map(EmployeResponse::depuis).toList();
    }

    @Transactional
    public EmployeResponse creer(EmployeRequest request) {
        String matricule = normaliserMatricule(request.matricule());
        if (employes.existsByMatricule(matricule)) {
            throw new IllegalArgumentException("Ce matricule existe déjà.");
        }
        Employe employe = new Employe();
        appliquer(employe, request, matricule);
        employe.setActif(true);
        return EmployeResponse.depuis(employes.save(employe));
    }

    @Transactional
    public EmployeResponse modifier(Long id, EmployeRequest request) {
        Employe employe = employes.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fiche employé introuvable."));
        String matricule = normaliserMatricule(request.matricule());
        if (employes.existsByMatriculeAndIdNot(matricule, id)) {
            throw new IllegalArgumentException("Ce matricule est déjà attribué à un autre employé.");
        }
        appliquer(employe, request, matricule);
        return EmployeResponse.depuis(employes.save(employe));
    }

    @Transactional
    public void desactiver(Long id) {
        Employe employe = employes.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fiche employé introuvable."));
        employe.setActif(false);
        employes.save(employe);
    }

    @Transactional
    public EmployeResponse reactiver(Long id) {
        Employe employe = employes.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fiche employé introuvable."));
        employe.setActif(true);
        return EmployeResponse.depuis(employes.save(employe));
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
