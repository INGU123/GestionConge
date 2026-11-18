package com.fruvio.GestionConge.type_conge.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.type_conge.service.Type_congeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/type-conge")
public class Type_congeController {

    private final Type_congeService typeCongeService;

    public Type_congeController(Type_congeService typeCongeService) {
        this.typeCongeService = typeCongeService;
    }

    // Créer un nouveau type de congé (Admin uniquement)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<?> createType_conge(@Valid @RequestBody Type_conge typeConge) {
        try {
            Type_conge nouveauType = typeCongeService.save(typeConge);
            return new ResponseEntity<>(nouveauType, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Erreur de création : " + e.getMessage());
        }
    }

    // Récupérer tous les types de congés
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @GetMapping("/all")
    public ResponseEntity<List<Type_conge>> getAllTypes() {
        return ResponseEntity.ok(typeCongeService.findAll());
    }

    // Récupérer un type par son ID
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @GetMapping("/{id}")
    public ResponseEntity<Type_conge> getTypeById(@PathVariable Long id) {
        Type_conge type = typeCongeService.findById(id);
        if (type != null) {
            return ResponseEntity.ok(type);
        }
        return ResponseEntity.notFound().build();
    }

    // Modifier un type de congé (Admin uniquement)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateType_conge(@PathVariable Long id, @Valid @RequestBody Type_conge details) {
        try {
            Type_conge misAJour = typeCongeService.update(id, details);
            if (misAJour != null) {
                return ResponseEntity.ok(misAJour);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Type de congé non trouvé");
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Erreur de modification : " + e.getMessage());
        }
    }

    // Supprimer un type de congé (Admin uniquement)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteType_conge(@PathVariable Long id) {
        if (typeCongeService.deleteById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}