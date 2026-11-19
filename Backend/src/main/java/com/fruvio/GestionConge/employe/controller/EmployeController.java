package com.fruvio.GestionConge.employe.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fruvio.GestionConge.employe.dto.EmployeLookupResponse;
import com.fruvio.GestionConge.employe.service.EmployeService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/employes")
public class EmployeController {
    private final EmployeService employeService;

    public EmployeController(EmployeService employeService) {
        this.employeService = employeService;
    }

    // Seule la recherche exacte par matricule est exposée à l'Assistant.
    @GetMapping("/recherche-matricule")
    @PreAuthorize("hasRole('ASSISTANT_DIRECTION')")
    public EmployeLookupResponse rechercherParMatricule(
            @RequestParam @NotBlank @Size(max = 50) @Pattern(regexp = "^[0-9]+$") String matricule) {
        return employeService.rechercherPourAssistant(matricule);
    }
}
