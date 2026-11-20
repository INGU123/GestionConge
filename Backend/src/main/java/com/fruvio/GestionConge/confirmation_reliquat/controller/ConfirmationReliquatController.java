package com.fruvio.GestionConge.confirmation_reliquat.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.fruvio.GestionConge.confirmation_reliquat.entity.ConfirmationReliquat;
import com.fruvio.GestionConge.confirmation_reliquat.service.ConfirmationReliquatService;
import com.fruvio.GestionConge.utilisateur.config.CustomUserDetails;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/reliquats")
public class ConfirmationReliquatController {
    private final ConfirmationReliquatService service;

    public ConfirmationReliquatController(ConfirmationReliquatService service) {
        this.service = service;
    }

    public record ConfirmationRequest(
            @NotNull @Min(0) Integer nombreJoursConfirme,
            @NotBlank @Size(max = 100) String periodeReference,
            @NotBlank @Size(max = 100) String referenceConfirmation,
            @Size(max = 500) String justificatif,
            @Size(max = 1500) String observation) {}

    @PostMapping("/dossier/{dossierId}/confirmer-automatiquement")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATION_PERSONNEL')")
    public ConfirmationReliquat confirmerAutomatiquement(@PathVariable Long dossierId,
            @RequestBody(required = false) ObservationRequest body) {
        return service.confirmerAutomatiquement(dossierId,
                body == null ? null : body.observation(), currentUser());
    }

    public record ObservationRequest(@Size(max = 1500) String observation) {}

    @PostMapping("/dossier/{dossierId}/confirmer")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATION_PERSONNEL')")
    public ConfirmationReliquat confirmer(@PathVariable Long dossierId,
            @Valid @RequestBody ConfirmationRequest body) {
        return service.confirmer(dossierId, body.nombreJoursConfirme(), body.periodeReference(),
                body.referenceConfirmation(), body.justificatif(), body.observation(), currentUser());
    }

    @GetMapping("/dossier/{dossierId}/historique")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATION_PERSONNEL')")
    public List<ConfirmationReliquat> historique(@PathVariable Long dossierId) {
        return service.historiqueDossier(dossierId, currentUser());
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
