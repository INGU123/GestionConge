package com.fruvio.GestionConge.solde_conge.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.service.Solde_congeService;
import com.fruvio.GestionConge.utilisateur.config.CustomUserDetails;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

@RestController
@RequestMapping("/solde")
public class Solde_congeController {

    private final Solde_congeService soldeCongeService;

    public Solde_congeController(Solde_congeService soldeCongeService) {
        this.soldeCongeService = soldeCongeService;
    }

    // Récupérer tous les soldes selon périmètre (Admin : tous, Manager : équipe)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping({ "", "/all" })
    public ResponseEntity<List<Solde_conge>> getAllSoldes() {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        return ResponseEntity.ok(soldeCongeService.getAllSoldes(currentUser));
    }

    // Récupérer tous les soldes d'un utilisateur (Employé : soi-même, Manager :
    // équipe, Admin : tous)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<List<Solde_conge>> getSoldesParUtilisateur(@PathVariable Long utilisateurId) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        return ResponseEntity.ok(soldeCongeService.getSoldesParUtilisateur(utilisateurId, currentUser));
    }

    // Initialiser le solde d'un utilisateur pour une année (Strictement réservé à
    // l'Admin)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/initialiser/{utilisateurId}")
    public ResponseEntity<Void> initialiserSoldesAnnuels(@PathVariable Long utilisateurId, @RequestParam int annee) {
        soldeCongeService.initialiserSoldesPourUtilisateur(utilisateurId, annee);
        return ResponseEntity.ok().build();
    }

    // Ajuster manuellement le solde (Strictement réservé à l'Admin)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/ajuster/{soldeId}")
    public ResponseEntity<Solde_conge> ajusterSolde(@PathVariable Long soldeId,
            @RequestParam int nouveauNombreJoursRestants) {

        Utilisateur currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            throw new IllegalStateException("Authentification requise pour ajuster le solde.");
        }

        Solde_conge soldeAjuste = soldeCongeService.ajusterSolde(soldeId, nouveauNombreJoursRestants,
                currentUser.getId());
        return ResponseEntity.ok(soldeAjuste);
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