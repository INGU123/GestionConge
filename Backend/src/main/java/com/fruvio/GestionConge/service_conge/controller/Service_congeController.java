package com.fruvio.GestionConge.service_conge.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fruvio.GestionConge.service_conge.entity.Services;
import com.fruvio.GestionConge.service_conge.repository.Service_congeRepository;
import com.fruvio.GestionConge.departement.repository.DepartementRepository;

@RestController
@RequestMapping("/services")
public class Service_congeController {

    private final Service_congeRepository serviceRepository;
    private final DepartementRepository departementRepository;

    public Service_congeController(Service_congeRepository serviceRepository, DepartementRepository departementRepository) {
        this.serviceRepository = serviceRepository;
        this.departementRepository = departementRepository;
    }

    // Récupérer tous les services (Consultation pour tous les utilisateurs authentifiés)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE', 'ASSISTANT_DIRECTION', 'ADMINISTRATION_PERSONNEL', 'CHEF_DEPARTEMENT', 'DIRECTEUR')")
    @GetMapping
    public List<Services> getAllServices() {
        return serviceRepository.findAll();
    }

    // Récupérer un service par ID
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @GetMapping("/{id}")
    public ResponseEntity<Services> getServiceById(@PathVariable Long id) {
        return serviceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Créer un nouveau service (Strictement ADMIN)
    @PreAuthorize("denyAll()")
    @PostMapping("/create")
    public Services createService(@RequestBody Services service) {
        return serviceRepository.save(service);
    }

    // Mettre à jour un service existant (Strictement ADMIN)
    @PreAuthorize("denyAll()")
    @PutMapping("/{id}")
    public ResponseEntity<Services> updateService(@PathVariable Long id, @RequestBody Services updatedService) {
        return serviceRepository.findById(id)
                .map(service -> {
                    service.setNom(updatedService.getNom());
                    service.setResponsable_id(updatedService.getResponsable_id());
                    service.setEffectifMinimum(updatedService.getEffectifMinimum());
                    return ResponseEntity.ok(serviceRepository.save(service));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/departement/{departementId}")
    public ResponseEntity<Services> rattacherDepartement(@PathVariable Long id, @PathVariable Long departementId) {
        return serviceRepository.findById(id).map(service -> {
            if (!departementRepository.existsById(departementId)) {
                throw new IllegalArgumentException("Département introuvable.");
            }
            service.setDepartementId(departementId);
            return ResponseEntity.ok(serviceRepository.save(service));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Supprimer un service (Strictement ADMIN)
    @PreAuthorize("denyAll()")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        return serviceRepository.findById(id)
                .map(service -> {
                    serviceRepository.delete(service);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
