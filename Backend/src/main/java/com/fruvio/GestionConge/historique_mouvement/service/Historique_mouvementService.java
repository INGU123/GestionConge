package com.fruvio.GestionConge.historique_mouvement.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fruvio.GestionConge.historique_mouvement.entity.Historique_mouvement;
import com.fruvio.GestionConge.historique_mouvement.repository.Historique_mouvementRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@Service
public class Historique_mouvementService {
    private final Historique_mouvementRepository historique_mouvementRepository;
    private final UtilisateurRepository utilisateurRepository;

    public Historique_mouvementService(Historique_mouvementRepository repository, UtilisateurRepository utilisateurs) {
        this.historique_mouvementRepository = repository;
        this.utilisateurRepository = utilisateurs;
    }

    @Transactional
    public void enregistrerMouvement(Long utilisateurId, String typeMouvement, Integer quantite, Long typeCongeId,
            Long demandeId, String commentaire, Long effectuePar) {
        Historique_mouvement mouvement = construireMouvement(typeMouvement, quantite, typeCongeId,
                demandeId, commentaire, effectuePar);
        mouvement.setUtilisateurId(utilisateurId);
        historique_mouvementRepository.save(mouvement);
    }

    @Transactional
    public void enregistrerMouvementPourEmploye(Long employeId, String typeMouvement, Integer quantite,
            Long typeCongeId, Long demandeId, String commentaire, Long effectuePar) {
        if (employeId == null) throw new IllegalArgumentException("Employé obligatoire pour ce mouvement.");
        Historique_mouvement mouvement = construireMouvement(typeMouvement, quantite, typeCongeId,
                demandeId, commentaire, effectuePar);
        mouvement.setEmployeId(employeId);
        historique_mouvementRepository.save(mouvement);
    }

    private Historique_mouvement construireMouvement(String type, Integer quantite, Long typeId,
            Long demandeId, String commentaire, Long acteur) {
        Historique_mouvement m = new Historique_mouvement();
        m.setTypeMouvement(type); m.setQuantite(quantite != null ? quantite : 0);
        m.setType_conge_id(typeId); m.setDemande_id(demandeId); m.setCommentaire(commentaire);
        m.setEffectue_par(acteur); m.setDate(new Date(System.currentTimeMillis()));
        return m;
    }

    public List<Historique_mouvement> getAllMouvements(Utilisateur currentUser) {
        if (currentUser == null) throw new AccessDeniedException("Authentification requise.");
        if (currentUser.getRole() == Role.ADMIN) return historique_mouvementRepository.findAll();
        if (currentUser.getRole() == Role.MANAGER) {
            List<Utilisateur> subordinates = utilisateurRepository.findByManagerId(currentUser.getId());
            List<Long> ids = new ArrayList<>(subordinates.stream().map(Utilisateur::getId).toList());
            if (!ids.contains(currentUser.getId())) ids.add(currentUser.getId());
            if (ids.isEmpty()) return Collections.emptyList();
            return historique_mouvementRepository.findByUtilisateurIdIn(ids);
        }
        throw new AccessDeniedException("Accès refusé.");
    }

    public List<Historique_mouvement> getMouvementsParUtilisateur(Long utilisateurId, Utilisateur currentUser) {
        if (currentUser == null) throw new AccessDeniedException("Authentification requise.");
        if (currentUser.getRole() == Role.EMPLOYE && !Objects.equals(currentUser.getId(), utilisateurId))
            throw new AccessDeniedException("Vous ne pouvez consulter que votre propre historique.");
        if (currentUser.getRole() == Role.MANAGER && !Objects.equals(currentUser.getId(), utilisateurId)) {
            Utilisateur target = utilisateurRepository.findById(utilisateurId).orElse(null);
            if (target == null || !Objects.equals(target.getManager_id(), currentUser.getId()))
                throw new AccessDeniedException("Accès non autorisé : cet employé n'appartient pas à votre équipe.");
        }
        return historique_mouvementRepository.findByUtilisateurId(utilisateurId);
    }

    public List<Historique_mouvement> getMouvementsParEmploye(Long employeId, Utilisateur currentUser) {
        if (currentUser == null || (currentUser.getRole() != Role.ADMIN
                && currentUser.getRole() != Role.ADMINISTRATION_PERSONNEL
                ))
            throw new AccessDeniedException("Accès non autorisé.");
        return historique_mouvementRepository.findByEmployeId(employeId);
    }
}
