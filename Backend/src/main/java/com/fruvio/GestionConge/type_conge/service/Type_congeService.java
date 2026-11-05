package com.fruvio.GestionConge.type_conge.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;

@Service
public class Type_congeService {

    private final Type_congeRepository typeCongeRepository;

    public Type_congeService(Type_congeRepository typeCongeRepository) {
        this.typeCongeRepository = typeCongeRepository;
    }

    public Type_conge save(Type_conge typeConge) {
        if (typeConge == null) {
            throw new IllegalArgumentException("Les informations du type de congé sont obligatoires.");
        }
        normaliserEtVerifierUnicite(typeConge, null);
        if (typeConge.getRegle_acquisition() == null || typeConge.getRegle_acquisition().trim().isEmpty()) {
            typeConge.setRegle_acquisition("STANDARD");
        }
        if (typeConge.getNombreJoursParAn() == null) {
            typeConge.setNombreJoursParAn(0);
        }
        if (typeConge.getActif() == null) {
            typeConge.setActif(true);
        }
        if (typeConge.getCouleur() == null || typeConge.getCouleur().trim().isEmpty()) {
            typeConge.setCouleur("#2563eb");
        }
        return typeCongeRepository.save(typeConge);
    }

    public List<Type_conge> findAll() {
        return typeCongeRepository.findAll();
    }

    public Type_conge findById(Long id) {
        return typeCongeRepository.findById(id).orElse(null);
    }

    public Type_conge update(Long id, Type_conge details) {
        Type_conge existant = findById(id);
        if (existant != null) {
            if (details.getCode() != null) existant.setCode(details.getCode());
            if (details.getLibelle() != null) existant.setLibelle(details.getLibelle());
            normaliserEtVerifierUnicite(existant, id);
            if (details.getNombreJoursParAn() != null) existant.setNombreJoursParAn(details.getNombreJoursParAn());
            existant.setJustificatifObligatoire(details.isJustificatifObligatoire());
            if (details.getActif() != null) existant.setActif(details.getActif());
            if (details.getCouleur() != null) existant.setCouleur(details.getCouleur());
            if (details.getRegle_acquisition() != null) existant.setRegle_acquisition(details.getRegle_acquisition());
            return typeCongeRepository.save(existant);
        }
        return null;
    }

    private void normaliserEtVerifierUnicite(Type_conge typeConge, Long idExclu) {
        String code = typeConge.getCode() == null ? "" : typeConge.getCode().trim().toUpperCase();
        String libelle = typeConge.getLibelle() == null ? "" : typeConge.getLibelle().trim();

        if (code.isEmpty() || libelle.isEmpty()) {
            throw new IllegalArgumentException("Le sigle et le libellé du type de congé sont obligatoires.");
        }

        boolean codeExiste = idExclu == null
                ? typeCongeRepository.existsByCodeIgnoreCase(code)
                : typeCongeRepository.existsByCodeIgnoreCaseAndIdNot(code, idExclu);
        boolean libelleExiste = idExclu == null
                ? typeCongeRepository.existsByLibelleIgnoreCase(libelle)
                : typeCongeRepository.existsByLibelleIgnoreCaseAndIdNot(libelle, idExclu);

        if (codeExiste || libelleExiste) {
            throw new IllegalArgumentException(
                    "Type existant : ce sigle ou ce libellé est déjà utilisé par un autre type de congé.");
        }

        typeConge.setCode(code);
        typeConge.setLibelle(libelle);
    }

    public boolean deleteById(Long id) {
        if (typeCongeRepository.existsById(id)) {
            typeCongeRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
