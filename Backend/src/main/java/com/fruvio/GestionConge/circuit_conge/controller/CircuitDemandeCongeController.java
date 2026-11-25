package com.fruvio.GestionConge.circuit_conge.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.fruvio.GestionConge.circuit_conge.entity.CircuitDemandeConge;
import com.fruvio.GestionConge.circuit_conge.service.CircuitDemandeCongeService;
import com.fruvio.GestionConge.export.PdfExportService;
import com.fruvio.GestionConge.utilisateur.config.CustomUserDetails;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;
import com.fruvio.GestionConge.employe.entity.Employe;
import com.fruvio.GestionConge.employe.repository.EmployeRepository;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/circuit-conge")
public class CircuitDemandeCongeController {
    private final CircuitDemandeCongeService service;
    private final PdfExportService pdf;
    private final UtilisateurRepository utilisateurs;
    private final Type_congeRepository typesConge;
    private final EmployeRepository employes;

    @Autowired
    public CircuitDemandeCongeController(CircuitDemandeCongeService service, PdfExportService pdf,
            UtilisateurRepository utilisateurs, Type_congeRepository typesConge, EmployeRepository employes) {
        this.service = service; this.pdf = pdf; this.utilisateurs = utilisateurs; this.typesConge = typesConge; this.employes = employes;
    }

    public CircuitDemandeCongeController(CircuitDemandeCongeService service, PdfExportService pdf,
            UtilisateurRepository utilisateurs, Type_congeRepository typesConge) {
        this(service, pdf, utilisateurs, typesConge, null);
    }

    public record CreationRequest(Long employeId, Long utilisateurId, @NotNull Long typeCongeId,
            @NotNull java.time.LocalDate dateDebut, @NotNull java.time.LocalDate dateFin,
            @Size(max=1500) String commentaire) {}
    public record ReliquatRequest(@NotNull @Min(0) Integer reliquatJours, @Size(max=1500) String commentaire) {}
    public record DecisionRequest(@NotBlank String decision, String motif) {}

    @PostMapping
    @PreAuthorize("hasRole('ASSISTANT_DIRECTION')")
    public ResponseEntity<CircuitDemandeConge> creer(@Valid @RequestBody CreationRequest body) {
        if ((body.employeId() == null) == (body.utilisateurId() == null))
            throw new IllegalArgumentException("Sélectionnez un seul employé ou collaborateur.");
        CircuitDemandeConge dossier = body.employeId() != null
                ? service.creerPourEmploye(body.employeId(), body.typeCongeId(), body.dateDebut(), body.dateFin(), body.commentaire(), currentUser())
                : service.creerPourUtilisateur(body.utilisateurId(), body.typeCongeId(), body.dateDebut(), body.dateFin(), body.commentaire(), currentUser());
        return ResponseEntity.status(201).body(dossier);
    }

    public record CreationPersonnelleRequest(@NotNull Long typeCongeId,
            @NotNull java.time.LocalDate dateDebut, @NotNull java.time.LocalDate dateFin,
            @Size(max=1500) String commentaire) {}

    @PostMapping("/personnelle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CircuitDemandeConge> creerDemandePersonnelle(
            @Valid @RequestBody CreationPersonnelleRequest body) {
        return ResponseEntity.status(201).body(service.creerDemandePersonnelle(
                body.typeCongeId(), body.dateDebut(), body.dateFin(), body.commentaire(), currentUser()));
    }

    @GetMapping("/mes-demandes-personnelles")
    @PreAuthorize("isAuthenticated()")
    public List<CircuitDemandeConge> mesDemandesPersonnelles() {
        return service.mesDemandesPersonnelles(currentUser());
    }

    @PostMapping("/{id}/soumettre-personnelle")
    @PreAuthorize("isAuthenticated()")
    public CircuitDemandeConge soumettreDemandePersonnelle(@PathVariable Long id) {
        return service.soumettreDemandePersonnelle(id, currentUser());
    }

    @GetMapping("/assistant/mes-dossiers")
    @PreAuthorize("hasRole('ASSISTANT_DIRECTION')")
    public List<CircuitDemandeConge> mesDossiers() { return service.dossiersAssistant(currentUser()); }

    @GetMapping("/admin/consultation")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CircuitDemandeConge> consultationAdmin() { return service.consultationAdmin(currentUser()); }

    @GetMapping("/administration/en-attente")
    @PreAuthorize("hasRole('ADMINISTRATION_PERSONNEL')")
    public List<CircuitDemandeConge> enAttenteAdministration() { return service.enAttenteAdministration(currentUser()); }

    @GetMapping("/chef/en-attente")
    @PreAuthorize("hasRole('CHEF_DEPARTEMENT')")
    public List<CircuitDemandeConge> enAttenteChef() { return service.enAttenteChef(currentUser()); }

    @GetMapping("/directeur/en-attente")
    @PreAuthorize("hasRole('DIRECTEUR')")
    public List<CircuitDemandeConge> enAttenteDirecteur() { return service.enAttenteDirecteur(currentUser()); }

    @PostMapping("/{id}/reliquat")
    @PreAuthorize("hasRole('ADMINISTRATION_PERSONNEL')")
    public CircuitDemandeConge retournerReliquat(@PathVariable Long id, @Valid @RequestBody ReliquatRequest body) {
        return service.retournerReliquat(id, body.reliquatJours(), body.commentaire(), currentUser());
    }

    @PostMapping("/{id}/soumettre-chef")
    @PreAuthorize("hasRole('ASSISTANT_DIRECTION')")
    public CircuitDemandeConge soumettreChef(@PathVariable Long id) {
        return service.soumettreAuChef(id, currentUser());
    }

    @PostMapping("/{id}/transmettre-directeur")
    @PreAuthorize("hasRole('CHEF_DEPARTEMENT')")
    public CircuitDemandeConge transmettreDirecteur(@PathVariable Long id) {
        return service.transmettreAuDirecteur(id, currentUser());
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('DIRECTEUR')")
    public CircuitDemandeConge decision(@PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
        return service.decider(id, body.decision(), body.motif(), currentUser());
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> telechargerDecision(@PathVariable Long id) {
        CircuitDemandeConge d = service.dossierPourPdf(id, currentUser());
        Utilisateur employe;
        if (d.getEmployeId() != null) {
            Employe e = employes.findById(d.getEmployeId()).orElseThrow(() -> new IllegalStateException("Employé historique introuvable."));
            employe = new Utilisateur(); employe.setMatricule(e.getMatricule()); employe.setNom(e.getNom());
            employe.setPrenom(e.getPrenom()); employe.setFonction(e.getFonction());
        } else {
            employe = utilisateurs.findById(d.getUtilisateurId()).orElseThrow();
        }
        Utilisateur directeur = utilisateurs.findById(d.getDirecteurId()).orElseThrow();
        String typeLabel = typesConge.findById(d.getTypeCongeId())
                .map(t -> t.getLibelle()).orElse("Type #" + d.getTypeCongeId());
        byte[] content = pdf.generateDecisionCongePdf(d, employe, directeur, typeLabel);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=decision-conge-" + id + ".pdf")
                .body(content);
    }

    private Utilisateur currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Authentification requise.");
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails details) return details.getUtilisateur();
        if (principal instanceof Utilisateur user) return user;
        throw new AccessDeniedException("Utilisateur authentifié introuvable.");
    }
}
