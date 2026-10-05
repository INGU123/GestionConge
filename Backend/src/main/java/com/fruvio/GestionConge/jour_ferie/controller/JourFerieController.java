package com.fruvio.GestionConge.jour_ferie.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fruvio.GestionConge.jour_ferie.entity.JourFerie;
import com.fruvio.GestionConge.jour_ferie.repository.JourFerieRepository;

@RestController
@RequestMapping("/jours-feries")
public class JourFerieController {

    private final JourFerieRepository jourFerieRepository;

    public JourFerieController(JourFerieRepository jourFerieRepository) {
        this.jourFerieRepository = jourFerieRepository;
    }

    @GetMapping({ "", "/all" })
    public ResponseEntity<List<JourFerie>> getAllJoursFeries() {
        return ResponseEntity.ok(jourFerieRepository.findByActifTrueOrderByDateAsc());
    }

    @GetMapping("/annee/{annee}")
    public ResponseEntity<List<JourFerie>> getJoursFeriesByYear(@PathVariable int annee) {
        return ResponseEntity.ok(jourFerieRepository.findByAnneeAndActifTrueOrderByDateAsc(annee));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<JourFerie> createJourFerie(@RequestBody JourFerie jourFerie) {
        if (jourFerie.getDate() == null || jourFerie.getLibelle() == null || jourFerie.getLibelle().isBlank()) {
            throw new IllegalArgumentException("La date et le libellé du jour férié sont obligatoires.");
        }
        if (jourFerie.getAnnee() == null) {
            jourFerie.setAnnee(jourFerie.getDate().getYear());
        }
        if (jourFerie.getActif() == null) {
            jourFerie.setActif(true);
        }
        if (jourFerieRepository.findByDateAndActifTrue(jourFerie.getDate()).isPresent()) {
            throw new IllegalArgumentException("Ce jour férié existe déjà.");
        }
        return ResponseEntity.ok(jourFerieRepository.save(jourFerie));
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> isJourFerie(LocalDate date) {
        return ResponseEntity.ok(date != null && jourFerieRepository.findByDateAndActifTrue(date).isPresent());
    }
}
