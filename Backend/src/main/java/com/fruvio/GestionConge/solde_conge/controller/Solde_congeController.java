package com.fruvio.GestionConge.solde_conge.controller;

import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.service.Solde_congeService;
import com.fruvio.GestionConge.utilisateur.config.CustomUserDetails;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

@RestController
@RequestMapping("/solde")
public class Solde_congeController {
    private final Solde_congeService soldeCongeService;
    public Solde_congeController(Solde_congeService service) { this.soldeCongeService = service; }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping({"", "/all"})
    public ResponseEntity<List<Solde_conge>> getAllSoldes() {
        return ResponseEntity.ok(soldeCongeService.getAllSoldes(getCurrentAuthenticatedUser()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE', 'ADMINISTRATION_PERSONNEL')")
    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<List<Solde_conge>> getSoldesParUtilisateur(@PathVariable Long utilisateurId) {
        return ResponseEntity.ok(soldeCongeService.getSoldesParUtilisateur(utilisateurId, getCurrentAuthenticatedUser()));
    }

    @PreAuthorize("hasRole('ADMINISTRATION_PERSONNEL')")
    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<Solde_conge>> getSoldesParEmploye(@PathVariable Long employeId) {
        return ResponseEntity.ok(soldeCongeService.getSoldesParEmploye(employeId, getCurrentAuthenticatedUser()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/initialiser/{utilisateurId}")
    public ResponseEntity<Void> initialiserSoldesAnnuels(@PathVariable Long utilisateurId, @RequestParam int annee) {
        soldeCongeService.initialiserSoldesPourUtilisateur(utilisateurId, annee);
        return ResponseEntity.ok().build();
    }

    // Calcul automatique du droit annuel légal à partir de la date d'embauche.
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATION_PERSONNEL')")
    @PostMapping("/employe/{employeId}/calculer-annuel")
    public ResponseEntity<Solde_conge> calculerSoldeAnnuelEmploye(@PathVariable Long employeId,
            @RequestParam Long typeCongeId, @RequestParam int periode) {
        return ResponseEntity.ok(soldeCongeService.calculerSoldeAnnuelEmploye(
                employeId, typeCongeId, periode, getCurrentAuthenticatedUser()));
    }

    // Saisie manuelle conservée pour les types de congé non annuels et les corrections RH.
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATION_PERSONNEL')")
    @PostMapping("/employe/{employeId}/initialiser")
    public ResponseEntity<Solde_conge> enregistrerSoldeEmploye(@PathVariable Long employeId,
            @RequestParam Long typeCongeId, @RequestParam int periode, @RequestParam int joursAcquis) {
        return ResponseEntity.status(201).body(soldeCongeService.enregistrerSoldeEmploye(
                employeId, typeCongeId, periode, joursAcquis, getCurrentAuthenticatedUser()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/ajuster/{soldeId}")
    public ResponseEntity<Solde_conge> ajusterSolde(@PathVariable Long soldeId,
            @RequestParam int nouveauNombreJoursRestants, @RequestParam String motif) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) throw new AccessDeniedException("Authentification requise pour ajuster le solde.");
        return ResponseEntity.ok(soldeCongeService.ajusterSolde(
                soldeId, nouveauNombreJoursRestants, currentUser.getId(), motif));
    }

    private Utilisateur getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails details) return details.getUtilisateur();
        if (principal instanceof Utilisateur utilisateur) return utilisateur;
        return null;
    }
}
