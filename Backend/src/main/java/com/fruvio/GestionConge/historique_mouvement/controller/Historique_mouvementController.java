package com.fruvio.GestionConge.historique_mouvement.controller;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fruvio.GestionConge.export.PdfExportService;
import com.fruvio.GestionConge.historique_mouvement.entity.Historique_mouvement;
import com.fruvio.GestionConge.historique_mouvement.repository.Historique_mouvementRepository;
import com.fruvio.GestionConge.historique_mouvement.service.Historique_mouvementService;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;
import com.fruvio.GestionConge.utilisateur.config.CustomUserDetails;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@RestController
@RequestMapping("/historiques")
public class Historique_mouvementController {

    private final Historique_mouvementRepository historique_mouvementRepository;
    private final Historique_mouvementService historiqueService;
    private final PdfExportService pdfExportService;
    private final UtilisateurRepository utilisateurRepository;
    private final Type_congeRepository typeCongeRepository;

    public Historique_mouvementController(Historique_mouvementRepository historique_mouvementRepository,
            Historique_mouvementService historiqueService, PdfExportService pdfExportService,
            UtilisateurRepository utilisateurRepository, Type_congeRepository typeCongeRepository) {
        this.historique_mouvementRepository = historique_mouvementRepository;
        this.historiqueService = historiqueService;
        this.pdfExportService = pdfExportService;
        this.utilisateurRepository = utilisateurRepository;
        this.typeCongeRepository = typeCongeRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping({"", "/all"})
    public ResponseEntity<List<Historique_mouvement>> getAll() {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        return ResponseEntity.ok(historiqueService.getAllMouvements(currentUser));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<List<Historique_mouvement>> getParUtilisateur(@PathVariable Long utilisateurId) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        return ResponseEntity.ok(historiqueService.getMouvementsParUtilisateur(utilisateurId, currentUser));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exporterPdf(@RequestParam(defaultValue = "all") String scope) {
        Utilisateur currentUser = getCurrentAuthenticatedUser();
        List<Historique_mouvement> mouvements;
        boolean vueComplete;

        if ("all".equalsIgnoreCase(scope)) {
            mouvements = historiqueService.getAllMouvements(currentUser);
            vueComplete = true;
        } else if ("mine".equalsIgnoreCase(scope)) {
            if (currentUser == null) {
                throw new AccessDeniedException("Authentification requise.");
            }
            mouvements = historiqueService.getMouvementsParUtilisateur(currentUser.getId(), currentUser);
            vueComplete = false;
        } else {
            return ResponseEntity.badRequest().build();
        }

        Set<Long> visibleUserIds = mouvements.stream()
                .map(Historique_mouvement::getUtilisateurId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (currentUser != null && currentUser.getRole() == Role.MANAGER) {
            visibleUserIds.add(currentUser.getId());
        }
        if (currentUser != null && currentUser.getRole() == Role.ADMIN) {
            mouvements.stream().map(Historique_mouvement::getEffectue_par)
                    .filter(id -> id != null).forEach(visibleUserIds::add);
        } else {
            mouvements.stream().map(Historique_mouvement::getEffectue_par)
                    .filter(id -> id != null && visibleUserIds.contains(id)).forEach(visibleUserIds::add);
        }

        Map<Long, String> userNames = utilisateurRepository.findAllById(visibleUserIds).stream()
                .collect(Collectors.toMap(Utilisateur::getId, utilisateur -> {
                    String fullName = ((utilisateur.getPrenom() == null ? "" : utilisateur.getPrenom().trim()) + " "
                            + (utilisateur.getNom() == null ? "" : utilisateur.getNom().trim())).trim();
                    return fullName.isEmpty() ? utilisateur.getEmail() : fullName;
                }));
        Set<Long> typeIds = mouvements.stream()
                .map(Historique_mouvement::getType_conge_id)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> typeLabels = typeCongeRepository.findAllById(typeIds).stream()
                .collect(Collectors.toMap(type -> type.getId(), type -> {
                    String label = type.getLibelle();
                    if (label == null || label.isBlank()) {
                        label = type.getCode();
                    }
                    return label == null || label.isBlank() ? "Type #" + type.getId() : label;
                }));

        byte[] pdf = pdfExportService.generateHistoriquePdf(mouvements, userNames, typeLabels, vueComplete);
        String filename = "historique_conges_" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf);
    }

    // Création d'un mouvement d'historique (Strictement POST et ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/creer")
    public ResponseEntity<String> creerMouvement(
            @RequestBody(required = false) Historique_mouvement body,
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(required = false) String typeMouvement,
            @RequestParam(required = false) Integer quantite,
            @RequestParam(required = false) Long type_conge_id,
            @RequestParam(required = false) Long demande_id,
            @RequestParam(required = false) String commentaire,
            @RequestParam(required = false) Long effectue_par,
            @RequestParam(required = false) Date date) {

        Long targetUserId = body != null && body.getUtilisateurId() != null ? body.getUtilisateurId() : utilisateurId;
        String targetType = body != null && body.getTypeMouvement() != null ? body.getTypeMouvement() : typeMouvement;
        Integer targetQuantite = body != null && body.getQuantite() != null ? body.getQuantite() : (quantite != null ? quantite : 0);
        Long targetTypeCongeId = body != null && body.getType_conge_id() != null ? body.getType_conge_id() : type_conge_id;
        Long targetDemandeId = body != null && body.getDemande_id() != null ? body.getDemande_id() : demande_id;
        String targetCommentaire = body != null && body.getCommentaire() != null ? body.getCommentaire() : commentaire;
        Long targetEffectuePar = body != null && body.getEffectue_par() != null ? body.getEffectue_par() : effectue_par;
        Date targetDate = body != null && body.getDate() != null ? body.getDate() : (date != null ? date : new Date(System.currentTimeMillis()));

        if (targetUserId == null || targetType == null) {
            return ResponseEntity.badRequest().body("L'identifiant utilisateur et le type de mouvement sont obligatoires.");
        }

        Historique_mouvement mouvement = Historique_mouvement.builder()
                .utilisateurId(targetUserId)
                .typeMouvement(targetType)
                .quantite(targetQuantite)
                .type_conge_id(targetTypeCongeId)
                .demande_id(targetDemandeId)
                .commentaire(targetCommentaire)
                .effectue_par(targetEffectuePar)
                .date(targetDate)
                .build();

        historique_mouvementRepository.save(mouvement);
        return ResponseEntity.status(HttpStatus.CREATED).body("Mouvement d'historique enregistré avec succès.");
    }

    private Utilisateur getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUtilisateur();
        }
        if (principal instanceof Utilisateur utilisateur) {
            return utilisateur;
        }

        return null;
    }
}
