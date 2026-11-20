package com.fruvio.GestionConge.circuit_conge.service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fruvio.GestionConge.circuit_conge.entity.CircuitDemandeConge;
import com.fruvio.GestionConge.circuit_conge.entity.PieceJointeCircuit;
import com.fruvio.GestionConge.circuit_conge.repository.CircuitDemandeCongeRepository;
import com.fruvio.GestionConge.circuit_conge.repository.PieceJointeCircuitRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

@Service
public class PieceJointeCircuitService {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private final CircuitDemandeCongeRepository dossiers;
    private final PieceJointeCircuitRepository pieces;
    private final Path storage;

    public PieceJointeCircuitService(
            CircuitDemandeCongeRepository dossiers,
            PieceJointeCircuitRepository pieces,
            @Value("${app.circuit.upload-dir:./data/pieces-jointes}") String directory) {
        this.dossiers = dossiers;
        this.pieces = pieces;
        this.storage = Paths.get(directory).toAbsolutePath().normalize();
    }

    @Transactional
    public PieceJointeCircuit enregistrer(Long dossierId, MultipartFile fichier, Utilisateur acteur) throws IOException {
        CircuitDemandeConge dossier = dossier(dossierId);
        autoriserDepot(dossier, acteur);

        if (fichier == null || fichier.isEmpty()) throw new IllegalArgumentException("Sélectionnez un scan.");
        if (fichier.getSize() > MAX_SIZE) throw new IllegalArgumentException("Le fichier dépasse 10 Mo.");

        byte[] debut = fichier.getBytes();
        String mime = detecterType(debut);
        if (mime == null) throw new IllegalArgumentException("Format refusé : utilisez un PDF, JPG ou PNG.");

        if (pieces.findByCircuitDemandeCongeId(dossierId).isPresent())
            throw new IllegalStateException("Un scan est déjà attaché à ce dossier.");

        String original = fichier.getOriginalFilename() == null ? "document" : fichier.getOriginalFilename();
        original = Paths.get(original.replace("\\\\", "/")).getFileName().toString();
        if (original.length() > 255) original = original.substring(original.length() - 255);

        String extension = mime.equals("application/pdf") ? ".pdf"
                : mime.equals("image/png") ? ".png" : ".jpg";
        String stored = UUID.randomUUID() + extension;

        Files.createDirectories(storage);
        Path destination = storage.resolve(stored).normalize();
        if (!destination.startsWith(storage)) throw new IOException("Chemin de stockage invalide.");

        Files.write(destination, debut, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

        try {
            PieceJointeCircuit piece = new PieceJointeCircuit();
            piece.setCircuitDemandeCongeId(dossierId);
            piece.setNomOriginal(original);
            piece.setNomStockage(stored);
            piece.setTypeContenu(mime);
            piece.setTailleOctets(fichier.getSize());
            piece.setUploaderId(acteur.getId());
            piece.setDateAjout(LocalDateTime.now());
            return pieces.save(piece);
        } catch (RuntimeException erreur) {
            Files.deleteIfExists(destination);
            throw erreur;
        }
    }

    @Transactional(readOnly = true)
    public PieceJointeCircuit obtenir(Long dossierId, Utilisateur acteur) {
        CircuitDemandeConge dossier = dossier(dossierId);
        autoriserLecture(dossier, acteur);
        return pieces.findByCircuitDemandeCongeId(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Aucun scan joint à ce dossier."));
    }

    public Path chemin(PieceJointeCircuit piece) throws IOException {
        Path fichier = storage.resolve(piece.getNomStockage()).normalize();
        if (!fichier.startsWith(storage) || !Files.isRegularFile(fichier))
            throw new IOException("Fichier joint introuvable.");
        return fichier;
    }

    private CircuitDemandeConge dossier(Long id) {
        return dossiers.findById(id).orElseThrow(() -> new IllegalArgumentException("Dossier introuvable."));
    }

    private void autoriserDepot(CircuitDemandeConge d, Utilisateur u) {
        verifierActeur(u);
        if (u.getRole() != Role.ASSISTANT_DIRECTION || !u.getId().equals(d.getAssistantId()))
            throw new AccessDeniedException("Seul l'assistant créateur peut joindre le scan.");
        if (!"RELIQUAT_EN_ATTENTE".equals(d.getStatut()) && !"RELIQUAT_RETOURNE".equals(d.getStatut()))
            throw new IllegalStateException("Le scan doit être joint avant la transmission au chef.");
    }

    private void autoriserLecture(CircuitDemandeConge d, Utilisateur u) {
        verifierActeur(u);
        boolean autorise = switch (u.getRole()) {
            case ADMIN -> true;
            case ASSISTANT_DIRECTION -> u.getId().equals(d.getAssistantId());
            case ADMINISTRATION_PERSONNEL -> true;
            case CHEF_DEPARTEMENT -> u.getId().equals(d.getChefId());
            case DIRECTEUR -> u.getId().equals(d.getDirecteurId());
            default -> false;
        };
        if (!autorise) throw new AccessDeniedException("Accès au scan non autorisé.");
    }

    private void verifierActeur(Utilisateur u) {
        if (u == null || u.getId() == null || !u.isEnabled())
            throw new AccessDeniedException("Authentification requise.");
    }

    private String detecterType(byte[] b) {
        if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-')
            return "application/pdf";
        if (b.length >= 8 && (b[0] & 255) == 137 && b[1] == 80 && b[2] == 78 && b[3] == 71
                && b[4] == 13 && b[5] == 10 && b[6] == 26 && b[7] == 10)
            return "image/png";
        if (b.length >= 3 && (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255)
            return "image/jpeg";
        return null;
    }
}
