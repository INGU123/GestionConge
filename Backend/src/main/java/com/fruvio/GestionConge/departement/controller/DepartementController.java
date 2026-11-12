package com.fruvio.GestionConge.departement.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fruvio.GestionConge.departement.entity.Departement;
import com.fruvio.GestionConge.departement.repository.DepartementRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/departements")
public class DepartementController {
    private final DepartementRepository repository;
    private final UtilisateurRepository utilisateurs;

    public DepartementController(DepartementRepository repository, UtilisateurRepository utilisateurs) {
        this.repository = repository;
        this.utilisateurs = utilisateurs;
    }

    public record DepartementRequest(@NotBlank String nom, @NotNull Long chefId, @NotNull Long directeurId) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ASSISTANT_DIRECTION','ADMINISTRATION_PERSONNEL','CHEF_DEPARTEMENT','DIRECTEUR')")
    public List<Departement> list() { return repository.findByActifTrueOrderByNomAsc(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Departement create(@Valid @RequestBody DepartementRequest request) {
        if (request.chefId().equals(request.directeurId())) throw new IllegalArgumentException("Le chef et le directeur doivent être deux personnes distinctes.");
        if (repository.existsByNomIgnoreCase(request.nom().trim())) throw new IllegalArgumentException("Ce département existe déjà.");
        var chef = utilisateurs.findById(request.chefId()).orElseThrow(() -> new IllegalArgumentException("Chef introuvable."));
        var directeur = utilisateurs.findById(request.directeurId()).orElseThrow(() -> new IllegalArgumentException("Directeur introuvable."));
        if (!chef.isEnabled() || chef.getRole() != Role.CHEF_DEPARTEMENT) throw new IllegalArgumentException("Le compte choisi doit avoir le rôle CHEF_DEPARTEMENT et être actif.");
        if (!directeur.isEnabled() || directeur.getRole() != Role.DIRECTEUR) throw new IllegalArgumentException("Le compte choisi doit avoir le rôle DIRECTEUR et être actif.");
        if (repository.findByChefIdAndActifTrue(chef.getId()).isPresent() || repository.findByDirecteurIdAndActifTrue(directeur.getId()).isPresent()) throw new IllegalArgumentException("Ce chef ou directeur est déjà affecté à un département.");
        return repository.save(Departement.builder().nom(request.nom().trim()).chefId(chef.getId()).directeurId(directeur.getId()).actif(true).build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Departement update(@PathVariable Long id, @Valid @RequestBody DepartementRequest request) {
        Departement d = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Département introuvable."));
        if (request.chefId().equals(request.directeurId())) throw new IllegalArgumentException("Le chef et le directeur doivent être deux personnes distinctes.");
        var chef = utilisateurs.findById(request.chefId()).orElseThrow(() -> new IllegalArgumentException("Chef introuvable."));
        var directeur = utilisateurs.findById(request.directeurId()).orElseThrow(() -> new IllegalArgumentException("Directeur introuvable."));
        if (chef.getRole() != Role.CHEF_DEPARTEMENT || !chef.isEnabled()) throw new IllegalArgumentException("Chef de département invalide.");
        if (directeur.getRole() != Role.DIRECTEUR || !directeur.isEnabled()) throw new IllegalArgumentException("Directeur invalide.");
        repository.findByChefIdAndActifTrue(chef.getId()).filter(x -> !x.getId().equals(id)).ifPresent(x -> { throw new IllegalArgumentException("Chef déjà affecté."); });
        repository.findByDirecteurIdAndActifTrue(directeur.getId()).filter(x -> !x.getId().equals(id)).ifPresent(x -> { throw new IllegalArgumentException("Directeur déjà affecté."); });
        d.setNom(request.nom().trim()); d.setChefId(chef.getId()); d.setDirecteurId(directeur.getId());
        return repository.save(d);
    }
}
