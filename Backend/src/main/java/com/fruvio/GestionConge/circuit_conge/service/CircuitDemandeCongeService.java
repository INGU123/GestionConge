package com.fruvio.GestionConge.circuit_conge.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fruvio.GestionConge.circuit_conge.entity.CircuitDemandeConge;
import com.fruvio.GestionConge.circuit_conge.repository.CircuitDemandeCongeRepository;
import com.fruvio.GestionConge.demande_conge.entity.DemandeConge;
import com.fruvio.GestionConge.demande_conge.service.DemandeCongeService;
import com.fruvio.GestionConge.departement.entity.Departement;
import com.fruvio.GestionConge.departement.repository.DepartementRepository;
import com.fruvio.GestionConge.jour_ferie.service.JourFerieService;
import com.fruvio.GestionConge.notifications_conge.service.Notification_congeService;
import com.fruvio.GestionConge.service_conge.entity.Services;
import com.fruvio.GestionConge.service_conge.repository.Service_congeRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@Service
public class CircuitDemandeCongeService {
    private final CircuitDemandeCongeRepository circuits;
    private final UtilisateurRepository utilisateurs;
    private final Service_congeRepository services;
    private final DepartementRepository departements;
    private final JourFerieService joursFeries;
    private final DemandeCongeService demandesHistoriques;
    private final Notification_congeService notifications;

    public CircuitDemandeCongeService(CircuitDemandeCongeRepository circuits, UtilisateurRepository utilisateurs,
            Service_congeRepository services, DepartementRepository departements, JourFerieService joursFeries,
            DemandeCongeService demandesHistoriques, Notification_congeService notifications) {
        this.circuits = circuits; this.utilisateurs = utilisateurs; this.services = services;
        this.departements = departements; this.joursFeries = joursFeries; this.demandesHistoriques = demandesHistoriques;
        this.notifications = notifications;
    }

    @Transactional
    public CircuitDemandeConge creer(Long employeId, Long typeId, java.time.LocalDate debut,
            java.time.LocalDate fin, String commentaire, Utilisateur assistant) {
        exiger(assistant, Role.ASSISTANT_DIRECTION);
        Utilisateur employe = utilisateurs.findByIdForUpdate(employeId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborateur introuvable."));
        if (!employe.isEnabled()) throw new IllegalArgumentException("Le collaborateur est inactif ou archivé.");
        if (employe.getService_id() == null) throw new IllegalStateException("Le collaborateur n'est affecté à aucun service.");
        Services serviceEmploye = services.findById(employe.getService_id()).orElseThrow(() -> new IllegalStateException("Service du collaborateur introuvable."));
        if (serviceEmploye.getDepartementId() == null) throw new IllegalStateException("Le service du collaborateur doit être rattaché à un département avant la création du dossier.");
        if (debut == null || fin == null || fin.isBefore(debut)) throw new IllegalArgumentException("Période de congé invalide.");
        if (typeId == null) throw new IllegalArgumentException("Le type de congé est obligatoire.");
        int nombre = demandesHistoriques.verifierEligibiliteWorkflow(employeId, typeId, debut, fin);
        verifierChevauchementCircuit(employeId, debut, fin, null);
        CircuitDemandeConge saved = circuits.save(CircuitDemandeConge.builder().utilisateurId(employeId).typeCongeId(typeId)
                .assistantId(assistant.getId()).dateDebut(debut).dateFin(fin).nombreJours(nombre)
                .commentaire(commentaire).statut("RELIQUAT_EN_ATTENTE").dateCreation(LocalDateTime.now()).build());
        for (Utilisateur agent : utilisateurs.findByRoleAndActifTrue(Role.ADMINISTRATION_PERSONNEL)) {
            notifications.creerNotification(agent.getId(), "Nouvelle demande de reliquat (dossier #" + saved.getId() + ").", "CIRCUIT_CONGE", "/dashboard/reliquats");
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CircuitDemandeConge> dossiersAssistant(Utilisateur user) {
        exiger(user, Role.ASSISTANT_DIRECTION);
        return circuits.findByAssistantIdOrderByDateCreationDesc(user.getId()).stream().map(this::enrichir).toList();
    }

    @Transactional(readOnly = true)
    public List<CircuitDemandeConge> consultationAdmin(Utilisateur user) {
        exiger(user, Role.ADMIN);
        return circuits.findAll().stream()
                .sorted(java.util.Comparator.comparing(CircuitDemandeConge::getDateCreation).reversed())
                .map(this::enrichir).toList();
    }

    @Transactional(readOnly = true)
    public List<CircuitDemandeConge> enAttenteAdministration(Utilisateur user) {
        exiger(user, Role.ADMINISTRATION_PERSONNEL);
        return circuits.findByStatutInOrderByDateCreationAsc(
                List.of("RELIQUAT_EN_ATTENTE", "RELIQUAT_A_CORRIGER")).stream().map(this::enrichir).toList();
    }

    @Transactional(readOnly = true)
    public List<CircuitDemandeConge> enAttenteChef(Utilisateur user) {
        exiger(user, Role.CHEF_DEPARTEMENT);
        return circuits.findByChefIdAndStatutOrderByDateCreationAsc(user.getId(), "CHEF_EN_ATTENTE").stream().map(this::enrichir).toList();
    }

    @Transactional(readOnly = true)
    public List<CircuitDemandeConge> enAttenteDirecteur(Utilisateur user) {
        exiger(user, Role.DIRECTEUR);
        return circuits.findByDirecteurIdAndStatutOrderByDateCreationAsc(user.getId(), "DIRECTEUR_EN_ATTENTE").stream().map(this::enrichir).toList();
    }

    @Transactional
    public CircuitDemandeConge retournerReliquat(Long id, Integer reliquat, String commentaire, Utilisateur actor) {
        exiger(actor, Role.ADMINISTRATION_PERSONNEL);
        if (reliquat == null || reliquat < 0) throw new IllegalArgumentException("Le reliquat doit être positif ou nul.");
        CircuitDemandeConge d = dossierVerrouille(id);
        if (!List.of("RELIQUAT_EN_ATTENTE", "RELIQUAT_A_CORRIGER").contains(d.getStatut()))
            throw new IllegalStateException("Ce dossier n'attend pas une vérification administrative.");
        int joursVerifies = demandesHistoriques.verifierEligibiliteWorkflow(
                d.getUtilisateurId(), d.getTypeCongeId(), d.getDateDebut(), d.getDateFin());
        if (joursVerifies != d.getNombreJours())
            throw new IllegalStateException("Le nombre de jours ouvrés du dossier a changé. Corrigez le dossier avant de continuer.");
        int disponible = demandesHistoriques.calculerReliquatDisponiblePourPeriode(
                d.getUtilisateurId(), d.getTypeCongeId(), d.getDateDebut(), d.getDateFin());
        if (reliquat != disponible)
            throw new IllegalArgumentException("Discordance du reliquat : le système calcule " + disponible
                    + " jour(s), mais " + reliquat + " jour(s) ont été saisis. Vérifiez les soldes par exercice.");
        d.setReliquatJours(reliquat); d.setReliquatCommentaire(commentaire);
        d.setAdministrationPersonnelId(actor.getId()); d.setDateReliquat(LocalDateTime.now());
        d.setStatut("RELIQUAT_RETOURNE");
        CircuitDemandeConge saved = circuits.save(d);
        notifications.creerNotification(d.getAssistantId(), "Le reliquat du dossier #" + d.getId() + " a été retourné.", "CIRCUIT_CONGE", "/dashboard/circuit-conge");
        return saved;
    }

    @Transactional
    public CircuitDemandeConge soumettreAuChef(Long id, Utilisateur assistant) {
        exiger(assistant, Role.ASSISTANT_DIRECTION);
        CircuitDemandeConge d = dossierVerrouille(id);
        verifierAssistantProprietaire(d, assistant);
        if (!"RELIQUAT_RETOURNE".equals(d.getStatut())) throw new IllegalStateException("Le reliquat doit d'abord être retourné.");
        verifierChevauchementCircuit(d.getUtilisateurId(), d.getDateDebut(), d.getDateFin(), d.getId());
        int joursVerifies = demandesHistoriques.verifierEligibiliteWorkflow(
                d.getUtilisateurId(), d.getTypeCongeId(), d.getDateDebut(), d.getDateFin());
        if (joursVerifies != d.getNombreJours())
            throw new IllegalStateException("Le nombre de jours ouvrés a changé. Retournez le dossier pour correction.");
        int disponibleActuel = demandesHistoriques.calculerReliquatDisponiblePourPeriode(
                d.getUtilisateurId(), d.getTypeCongeId(), d.getDateDebut(), d.getDateFin());
        if (d.getReliquatJours() == null || !d.getReliquatJours().equals(disponibleActuel))
            throw new IllegalStateException("Le solde a changé depuis le contrôle RH. Retournez le dossier à l'Administration du personnel pour une nouvelle vérification.");
        if (disponibleActuel < d.getNombreJours())
            throw new IllegalStateException("Reliquat insuffisant : " + d.getNombreJours() + " jour(s) demandés, "
                    + disponibleActuel + " jour(s) disponibles.");
        Utilisateur employe = utilisateurActif(d.getUtilisateurId());
        if (employe.getService_id() == null) throw new IllegalStateException("Le collaborateur n'est affecté à aucun service.");
        Services service = services.findById(employe.getService_id()).orElseThrow(() -> new IllegalStateException("Service du collaborateur introuvable."));
        if (service.getDepartementId() == null) throw new IllegalStateException("Le service du collaborateur n'est rattaché à aucun département.");
        Departement dep = departements.findById(service.getDepartementId()).orElseThrow(() -> new IllegalStateException("Département introuvable."));
        if (!Boolean.TRUE.equals(dep.getActif())) throw new IllegalStateException("Département inactif.");
        if (dep.getChefId() == null || dep.getDirecteurId() == null)
            throw new IllegalStateException("Le département doit avoir un chef et un directeur actifs avant la transmission.");
        Utilisateur chefAffecte = utilisateurActif(dep.getChefId());
        Utilisateur directeurAffecte = utilisateurActif(dep.getDirecteurId());
        if (chefAffecte.getRole() != Role.CHEF_DEPARTEMENT)
            throw new IllegalStateException("Le responsable du département n'a pas le rôle CHEF_DEPARTEMENT.");
        if (directeurAffecte.getRole() != Role.DIRECTEUR)
            throw new IllegalStateException("Le responsable de la Direction n'a pas le rôle DIRECTEUR.");
        d.setDepartementId(dep.getId()); d.setChefId(dep.getChefId()); d.setDirecteurId(dep.getDirecteurId());
        d.setDateSoumissionChef(LocalDateTime.now()); d.setStatut("CHEF_EN_ATTENTE");
        CircuitDemandeConge saved = circuits.save(d);
        notifications.creerNotification(d.getChefId(), "Un dossier de congé (#" + d.getId() + ") attend votre examen.", "CIRCUIT_CONGE", "/dashboard/chef-departement");
        return saved;
    }

    @Transactional
    public CircuitDemandeConge transmettreAuDirecteur(Long id, String avis, String commentaire, Utilisateur chef) {
        exiger(chef, Role.CHEF_DEPARTEMENT);
        CircuitDemandeConge d = dossierVerrouille(id);
        if (!chef.getId().equals(d.getChefId())) throw new AccessDeniedException("Ce dossier ne relève pas de votre département.");
        if (!"CHEF_EN_ATTENTE".equals(d.getStatut())) throw new IllegalStateException("Ce dossier n'attend pas l'avis du chef.");
        String avisNormalise = avis == null ? "" : avis.trim().toUpperCase();
        if (!List.of("FAVORABLE", "DEFAVORABLE").contains(avisNormalise))
            throw new IllegalArgumentException("L'avis du chef doit être FAVORABLE ou DEFAVORABLE.");
        String observation = commentaire == null ? "" : commentaire.trim();
        if ("DEFAVORABLE".equals(avisNormalise) && observation.isBlank())
            throw new IllegalArgumentException("Une observation est obligatoire pour un avis défavorable.");
        d.setAvisChef(avisNormalise);
        d.setCommentaireChef(observation);
        d.setDateTransmissionDirecteur(LocalDateTime.now()); d.setStatut("DIRECTEUR_EN_ATTENTE");
        CircuitDemandeConge saved = circuits.save(d);
        notifications.creerNotification(d.getDirecteurId(), "Un dossier de congé (#" + d.getId() + ") attend votre décision.", "CIRCUIT_CONGE", "/dashboard/directeur");
        return saved;
    }

    @Transactional
    public CircuitDemandeConge decider(Long id, String decision, String motif, Utilisateur directeur) {
        exiger(directeur, Role.DIRECTEUR);
        CircuitDemandeConge d = dossierVerrouille(id);
        if (!directeur.getId().equals(d.getDirecteurId())) throw new AccessDeniedException("Ce dossier ne relève pas de votre département.");
        if (!"DIRECTEUR_EN_ATTENTE".equals(d.getStatut())) throw new IllegalStateException("Ce dossier n'attend pas la décision du directeur.");
        String choix = decision == null ? "" : decision.trim().toUpperCase();
        if (!List.of("VALIDEE", "REFUSEE").contains(choix)) throw new IllegalArgumentException("Décision attendue : VALIDEE ou REFUSEE.");
        String raison = motif == null ? "" : motif.trim();
        if ("REFUSEE".equals(choix) && raison.isBlank()) throw new IllegalArgumentException("Le motif de refus est obligatoire.");

        if ("VALIDEE".equals(choix)) {
            int joursVerifies = demandesHistoriques.verifierEligibiliteWorkflow(
                    d.getUtilisateurId(), d.getTypeCongeId(), d.getDateDebut(), d.getDateFin());
            if (joursVerifies != d.getNombreJours())
                throw new IllegalStateException("Le nombre de jours ouvrés a changé depuis la création du dossier. La décision est suspendue pour éviter un débit incorrect.");
            // Actualise le relevé final après recontrôle transactionnel des soldes.
            d.setReliquatJours(demandesHistoriques.calculerReliquatDisponiblePourPeriode(
                    d.getUtilisateurId(), d.getTypeCongeId(), d.getDateDebut(), d.getDateFin()));
        }
        DemandeConge legacy = DemandeConge.builder().utilisateurId(d.getUtilisateurId()).typeCongeId(d.getTypeCongeId())
                .dateDebut(d.getDateDebut()).dateFin(d.getDateFin()).nombreJours(d.getNombreJours())
                .commentaire(d.getCommentaire()).statut("EN_ATTENTE").dateCreation(java.time.LocalDate.now()).build();
        DemandeConge saved = demandesHistoriques.creerDemandePourCircuit(legacy);
        DemandeConge decisionResult = demandesHistoriques.traiterDemande(saved.getId(), null, choix, raison, directeur);
        d.setLegacyDemandeId(decisionResult.getId()); d.setMotifRefus("REFUSEE".equals(choix) ? raison : null);
        d.setDateDecision(LocalDateTime.now()); d.setStatut(choix);
        CircuitDemandeConge savedCircuit = circuits.save(d);
        notifications.creerNotification(d.getAssistantId(), "La décision du directeur pour le dossier #" + d.getId() + " est " + choix + ". Le PDF officiel est disponible dans votre boîte de réception.", "DECISION_CONGE", "/dashboard/circuit-conge");
        return savedCircuit;
    }

    @Transactional
    public CircuitDemandeConge demanderCorrectionReliquat(Long id, String commentaire, Utilisateur assistant) {
        exiger(assistant, Role.ASSISTANT_DIRECTION);
        CircuitDemandeConge d = dossierVerrouille(id);
        verifierAssistantProprietaire(d, assistant);
        if (!"RELIQUAT_RETOURNE".equals(d.getStatut()))
            throw new IllegalStateException("Seul un dossier dont le reliquat a été retourné peut être signalé.");
        String motif = commentaire == null ? "" : commentaire.trim();
        if (motif.isBlank()) throw new IllegalArgumentException("La discordance constatée doit être décrite.");
        d.setAssistantCorrection(motif);
        d.setStatut("RELIQUAT_A_CORRIGER");
        CircuitDemandeConge saved = circuits.save(d);
        for (Utilisateur agent : utilisateurs.findByRoleAndActifTrue(Role.ADMINISTRATION_PERSONNEL)) {
            notifications.creerNotification(agent.getId(), "Le dossier #" + d.getId()
                    + " a été retourné pour correction du relevé de reliquat.", "CIRCUIT_CONGE", "/dashboard/reliquats");
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CircuitDemandeConge> dossiersEmploye(Utilisateur user) {
        exiger(user, Role.EMPLOYE);
        return circuits.findByUtilisateurId(user.getId()).stream()
                .sorted(java.util.Comparator.comparing(CircuitDemandeConge::getDateCreation).reversed())
                .map(this::enrichir).toList();
    }

    private CircuitDemandeConge enrichir(CircuitDemandeConge dossier) {
        utilisateurs.findById(dossier.getUtilisateurId()).ifPresent(u -> {
            dossier.setUtilisateurNom(u.getNom());
            dossier.setUtilisateurPrenom(u.getPrenom());
            dossier.setUtilisateurMatricule(u.getMatricule());
        });
        return dossier;
    }

    private void verifierChevauchementCircuit(Long employeId, java.time.LocalDate debut,
            java.time.LocalDate fin, Long dossierIgnore) {
        for (CircuitDemandeConge autre : circuits.findByUtilisateurId(employeId)) {
            if (dossierIgnore != null && dossierIgnore.equals(autre.getId())) continue;
            if ("REFUSEE".equals(autre.getStatut())) continue;
            if (!(fin.isBefore(autre.getDateDebut()) || debut.isAfter(autre.getDateFin()))) {
                throw new IllegalStateException("Cette période chevauche un autre dossier de congé déjà enregistré.");
            }
        }
    }

    private CircuitDemandeConge dossierVerrouille(Long id) {
        return circuits.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Dossier introuvable."));
    }
    private Utilisateur utilisateurActif(Long id) {
        Utilisateur u = utilisateurs.findById(id).orElseThrow(() -> new IllegalArgumentException("Collaborateur introuvable."));
        if (!u.isEnabled()) throw new IllegalArgumentException("Le collaborateur est inactif ou archivé.");
        return u;
    }
    private void verifierAssistantProprietaire(CircuitDemandeConge d, Utilisateur assistant) {
        if (!assistant.getId().equals(d.getAssistantId())) throw new AccessDeniedException("Dossier créé par un autre assistant.");
    }
    private void exiger(Utilisateur u, Role role) {
        if (u == null || u.getRole() != role || !u.isEnabled()) throw new AccessDeniedException("Accès réservé au rôle " + role + ".");
    }
}
