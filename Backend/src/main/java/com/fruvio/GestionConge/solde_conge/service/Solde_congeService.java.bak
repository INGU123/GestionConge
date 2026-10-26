package com.fruvio.GestionConge.solde_conge.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fruvio.GestionConge.historique_mouvement.service.Historique_mouvementService;
import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@Service
public class Solde_congeService {

    private final Solde_congeRepository soldeCongeRepository;
    private final Type_congeRepository typeCongeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final Historique_mouvementService historiqueMouvementService;

    public Solde_congeService(Solde_congeRepository soldeCongeRepository, Type_congeRepository typeCongeRepository,
            UtilisateurRepository utilisateurRepository, Historique_mouvementService historiqueMouvementService) {
        this.soldeCongeRepository = soldeCongeRepository;
        this.typeCongeRepository = typeCongeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.historiqueMouvementService = historiqueMouvementService;
    }

    // Récupérer tous les soldes d’un utilisateur avec contrôle d'autorisation
    public List<Solde_conge> getSoldesParUtilisateur(Long utilisateurId, Utilisateur currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise.");
        }

        if (currentUser.getRole() == Role.EMPLOYE && !Objects.equals(currentUser.getId(), utilisateurId)) {
            throw new AccessDeniedException("Vous ne pouvez consulter que vos propres soldes.");
        }

        if (currentUser.getRole() == Role.MANAGER) {
            if (!Objects.equals(currentUser.getId(), utilisateurId)) {
                Utilisateur target = utilisateurRepository.findById(utilisateurId).orElse(null);
                if (target == null || !Objects.equals(target.getManager_id(), currentUser.getId())) {
                    throw new AccessDeniedException(
                            "Accès non autorisé : cet employé n'appartient pas à votre équipe.");
                }
            }
        }

        List<Solde_conge> soldes = soldeCongeRepository.findByUtilisateurId(utilisateurId);
        if (soldes.isEmpty()) {
            int annee = java.time.LocalDate.now().getYear();
            initialiserSoldesPourUtilisateur(utilisateurId, annee);
            soldes = soldeCongeRepository.findByUtilisateurId(utilisateurId);
        }
        return soldes;
    }

    // Récupérer tous les soldes selon le rôle
    public List<Solde_conge> getAllSoldes(Utilisateur currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise.");
        }

        if (currentUser.getRole() == Role.ADMIN) {
            return soldeCongeRepository.findAll();
        }

        if (currentUser.getRole() == Role.MANAGER) {
            List<Utilisateur> subordinates = utilisateurRepository.findByManagerId(currentUser.getId());
            List<Long> ids = new ArrayList<>(subordinates.stream().map(Utilisateur::getId).toList());
            if (!ids.contains(currentUser.getId())) {
                ids.add(currentUser.getId());
            }
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            return soldeCongeRepository.findByUtilisateurIdIn(ids);
        }

        throw new AccessDeniedException(
                "Accès refusé : consultation globale réservée aux managers et administrateurs.");
    }

    // Initialiser les soldes annuels pour un utilisateur (Admin)
    @Transactional
    public void initialiserSoldesPourUtilisateur(Long utilisateurId, int annee) {
        List<Type_conge> types = typeCongeRepository.findAll();

        for (Type_conge type : types) {
            if (!soldeCongeRepository.existsByUtilisateurIdAndTypeCongeIdAndPeriode(utilisateurId, type.getId(),
                    annee)) {
                Solde_conge solde = new Solde_conge();
                solde.setUtilisateurId(utilisateurId);
                solde.setTypeCongeId(type.getId());
                solde.setPeriode(annee);
                int nbJours = (type.getNombreJoursParAn() != null) ? type.getNombreJoursParAn() : 0;
                solde.setSoldeAquis(nbJours);
                solde.setSoldePris(0);
                solde.setSoldeRestant(nbJours);
                solde.setDate_maj(new Timestamp(System.currentTimeMillis()));

                soldeCongeRepository.save(solde);
            }
        }
    }

    // Ajuster manuellement le solde (Strictement réservé à l'Admin)
    @Transactional
    public Solde_conge ajusterSolde(Long soldeId, int nouveauNombreJoursRestants) {
        return ajusterSolde(soldeId, nouveauNombreJoursRestants, null);
    }

    @Transactional
    public Solde_conge ajusterSolde(Long soldeId, int nouveauNombreJoursRestants, Long acteurId) {
        if (nouveauNombreJoursRestants < 0) {
            throw new IllegalArgumentException("Le solde ne peut pas être négatif.");
        }

        Solde_conge solde = soldeCongeRepository.findById(soldeId)
                .orElseThrow(() -> new IllegalArgumentException("Solde introuvable"));

        int ancienRestant = solde.getSoldeRestant() != null ? solde.getSoldeRestant() : 0;
        int acquis = solde.getSoldeAquis() != null ? solde.getSoldeAquis() : 0;
        int pris = Math.max(0, acquis - nouveauNombreJoursRestants);

        solde.setSoldeRestant(nouveauNombreJoursRestants);
        solde.setSoldePris(pris);
        solde.setDate_maj(new Timestamp(System.currentTimeMillis()));

        Solde_conge saved = soldeCongeRepository.save(solde);

        if (acteurId != null && solde.getUtilisateurId() != null) {
            int delta = Math.abs(nouveauNombreJoursRestants - ancienRestant);
            historiqueMouvementService.enregistrerMouvement(solde.getUtilisateurId(), "AJUSTEMENT_SOLDE", delta,
                    solde.getTypeCongeId(), null,
                    "Ajustement manuel du solde : " + ancienRestant + " -> " + nouveauNombreJoursRestants, acteurId);
        }

        return saved;
    }
}
