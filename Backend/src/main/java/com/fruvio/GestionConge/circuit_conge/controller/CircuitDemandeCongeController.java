package com.fruvio.GestionConge.circuit_conge.controller;

import java.util.List;
import java.util.Map;
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

    public CircuitDemandeCongeController(CircuitDemandeCongeService service, PdfExportService pdf,
            UtilisateurRepository utilisateurs, Type_congeRepository typesConge) {
        this.service = service; this.pdf = pdf; this.utilisateurs = utilisateurs; this.typesConge = typesConge;
    }

    public record CreationRequest(@NotNull Long utilisateurId, @NotNull Long typeCongeId,
            @NotNull java.time.LocalDate dateDebut, @NotNull java.time.LocalDate dateFin,
            @Size(max=1500) String commentaire) {}
    public record ReliquatRequest(@NotNull @Min(0) Integer reliquatJours, @Size(max=1500) String commentaire) {}
    public record FormulaireAssistantRequest(@NotNull java.time.LocalDate dateDebut,
            @NotNull java.time.LocalDate dateFin, @Size(max=1500) String commentaire) {}
    public record DecisionRequest(@NotBlank String decision, String motif) {}

    @PostMapping
    @PreAuthorize("hasRole('ASSISTANT_DIRECTION')")
    public ResponseEntity<CircuitDemandeConge> creer(@Valid @RequestBody CreationRequest body) {
        return ResponseEntity.status(201).body(service.creer(body.utilisateurId(), body.typeCongeId(),
                body.dateDebut(), body.dateFin(), body.commentaire(), currentUser()));
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
    public CircuitDemandeConge soumettreChef(@PathVariable Long id, @Valid @RequestBody FormulaireAssistantRequest body) {
        return service.soumettreAuChef(id, body.dateDebut(), body.dateFin(), body.commentaire(), currentUser());
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
    @PreAuthorize("hasRole('ASSISTANT_DIRECTION')")
    public ResponseEntity<byte[]> telechargerDecision(@PathVariable Long id) {
        Utilisateur assistant = currentUser();
        CircuitDemandeConge d = service.dossiersAssistant(assistant).stream()
                .filter(x -> x.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AccessDeniedException("Ce dossier ne vous appartient pas."));
        if (!List.of("VALIDEE", "REFUSEE").contains(d.getStatut())) {
            throw new IllegalStateException("Le PDF sera disponible après la décision du directeur.");
        }
        Utilisateur employe = utilisateurs.findById(d.getUtilisateurId()).orElseThrow();
        Utilisateur directeur = utilisateurs.findById(d.getDirecteurId()).orElseThrow();
        String typeLabel = typesConge.findById(d.getTypeCongeId()).map(t -> t.getLibelle()).orElse("Type #" + d.getTypeCongeId());
        byte[] content = pdf.generateDecisionCongePdf(d, employe, directeur, typeLabel);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=decision-conge-" + id + ".pdf")
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
