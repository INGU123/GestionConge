package com.fruvio.GestionConge.circuit_conge.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fruvio.GestionConge.circuit_conge.entity.PieceJointeCircuit;
import com.fruvio.GestionConge.circuit_conge.service.PieceJointeCircuitService;
import com.fruvio.GestionConge.utilisateur.config.CustomUserDetails;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

@RestController
@RequestMapping("/circuit-conge/{id}/piece-jointe")
public class PieceJointeCircuitController {
    private final PieceJointeCircuitService service;

    public PieceJointeCircuitController(PieceJointeCircuitService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ASSISTANT_DIRECTION')")
    public ResponseEntity<?> deposer(@PathVariable Long id, @RequestParam("fichier") MultipartFile fichier)
            throws IOException {
        PieceJointeCircuit piece = service.enregistrer(id, fichier, currentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of(
                "nomOriginal", piece.getNomOriginal(),
                "tailleOctets", piece.getTailleOctets(),
                "dateAjout", piece.getDateAjout().toString()));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InputStreamResource> telecharger(@PathVariable Long id) throws IOException {
        PieceJointeCircuit piece = service.obtenir(id, currentUser());
        var path = service.chemin(piece);
        String safeName = piece.getNomOriginal().replace("\"", "");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(piece.getTypeContenu()))
                .contentLength(Files.size(path))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(safeName, StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(Files.newInputStream(path)));
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
