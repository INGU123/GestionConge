package com.fruvio.GestionConge.employe.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import com.fruvio.GestionConge.employe.dto.EmployeLookupResponse;
import com.fruvio.GestionConge.employe.dto.EmployeRequest;
import com.fruvio.GestionConge.employe.dto.EmployeResponse;
import com.fruvio.GestionConge.employe.service.EmployeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/employes")
public class EmployeController {
    private final EmployeService employeService;

    public EmployeController(EmployeService employeService) {
        this.employeService = employeService;
    }

    // Le référentiel complet est strictement réservé à l'ADMIN.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<EmployeResponse> lister() {
        return employeService.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeResponse creer(@Valid @RequestBody EmployeRequest request) {
        return employeService.creer(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeResponse modifier(@PathVariable Long id, @Valid @RequestBody EmployeRequest request) {
        return employeService.modifier(id, request);
    }

    // Suppression logique : les dossiers et historiques futurs garderont leur référence.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void desactiver(@PathVariable Long id) {
        employeService.desactiver(id);
    }

    @PostMapping("/{id}/reactiver")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeResponse reactiver(@PathVariable Long id) {
        return employeService.reactiver(id);
    }

    // Aucun endpoint de liste ou de recherche par nom n'est exposé à l'Assistant.
    @GetMapping("/recherche-matricule")
    @PreAuthorize("hasRole('ASSISTANT_DIRECTION')")
    public EmployeLookupResponse rechercherParMatricule(
            @RequestParam @NotBlank @Size(max = 50) String matricule) {
        return employeService.rechercherPourAssistant(matricule);
    }
}
