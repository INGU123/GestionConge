package com.fruvio.GestionConge.demande_conge.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fruvio.GestionConge.demande_conge.entity.DemandeConge;
import com.fruvio.GestionConge.demande_conge.service.DemandeCongeService;
import com.fruvio.GestionConge.utilisateur.config.CustomUserDetails;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

@RestController
@RequestMapping("/conge")
public class Demande_CongeController {

    private final DemandeCongeService congeService;

    public Demande_CongeController(DemandeCongeService congeService) {
        this.congeService = congeService;
    }

    // Soumettre une demande de congé
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @PostMapping("/demander/{utilisateurId}")
    public ResponseEntity<DemandeConge> demanderConge(
            @PathVariable Long utilisateurId,
            @RequestBody DemandeConge demande) {

        Utilisateur currentUser = getCurrentAuthenticatedUser();
        DemandeConge nouvelleDemande = congeService.creerDemande(utilisateurId, demande, currentUser);
        return new ResponseEntity<>(nouvelleDemande, HttpStatus.CREATED);
    }

    // Valider ou refuser une demande (Réservé au Manager et Admin)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/{demandeId}/traiter")
    public ResponseEntity<DemandeConge> traiterDemande(
            @PathVariable Long demandeId,
            @RequestParam(required = false) Long managerId,
            @RequestParam String statut,
            @RequestParam(required = false) String motifRefus) {

        Utilisateur currentUser = getCurrentAuthenticatedUser();
        DemandeConge demandeTraitee = congeService.traiterDemande(demandeId, managerId, statut, motifRefus, currentUser);
        return ResponseEntity.ok(demandeTraitee);
    }

    // Annuler une demande
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @PutMapping("/{demandeId}/annuler")
    public ResponseEntity<DemandeConge> annulerDemande(
            @PathVariable Long demandeId,
            @RequestParam(required = false) Long utilisateurId) {

        Utilisateur currentUser = getCurrentAuthenticatedUser();
        DemandeConge demandeAnnulee = congeService.annulerDemande(demandeId, utilisateurId, currentUser);
        return ResponseEntity.ok(demandeAnnulee);
    }

    // Liste des demandes d'un collaborateur
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @GetMapping("/mes-demandes/{utilisateurId}")
    public ResponseEntity<List<DemandeConge>> getMesDemandes(@PathVariable Long utilisateurId) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        return ResponseEntity.ok(congeService.getDemandesParUtilisateur(utilisateurId, currentUser));
    }

    // Liste des demandes en attente pour un manager
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/manager/{managerId}/en-attente")
    public ResponseEntity<List<DemandeConge>> getDemandesEnAttenteManager(@PathVariable Long managerId) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        return ResponseEntity.ok(congeService.getDemandesEnAttentePourManager(managerId, currentUser));
    }

    // Liste de toutes les demandes en attente (selon périmètre)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/en-attente")
    public ResponseEntity<List<DemandeConge>> getToutesDemandesEnAttente() {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        return ResponseEntity.ok(congeService.getToutesLesDemandesEnAttente(currentUser));
    }

    // Liste de toutes les demandes (selon périmètre)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/all")
    public ResponseEntity<List<DemandeConge>> getAllDemandes() {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        return ResponseEntity.ok(congeService.getToutesLesDemandes(currentUser));
    }

    private Utilisateur getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUtilisateur();
        }
        if (principal instanceof Utilisateur utilisateur) {
            return utilisateur;
        }

        return null;
    }
}