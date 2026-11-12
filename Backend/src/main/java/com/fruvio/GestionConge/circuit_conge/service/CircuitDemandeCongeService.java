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
        Utilisateur employe = utilisateurActif(employeId);
        if (debut == null || fin == null || fin.isBefore(debut)) throw new IllegalArgumentException("Période de congé invalide.");
        if (typeId == null) throw new IllegalArgumentException("Le type de congé est obligatoire.");
        int nombre = joursFeries.compterJoursOuvres(debut, fin);
        if (nombre <= 0) throw new IllegalArgumentException("La période ne contient aucun jour ouvré.");
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
        return circuits.findByAssistantIdOrderByDateCreationDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public List<CircuitDemandeConge> consultationAdmin(Utilisateur user) {
        exiger(user, Role.ADMIN);
        return circuits.findAll().stream()
                .sorted(java.util.Comparator.comparing(CircuitDemandeConge::getDateCreation).reversed()).toList();
    }

    @Transactional(readOnly = true)
    public List<CircuitDemandeConge> enAttenteAdministration(Utilisateur user) {
        exiger(user, Role.ADMINISTRATION_PERSONNEL);
        return circuits.findByStatutOrderByDateCreationAsc("RELIQUAT_EN_ATTENTE");
    }

    @Transactional(readOnly = true)
    public List<CircuitDemandeConge> enAttenteChef(Utilisateur user) {
        exiger(user, Role.CHEF_DEPARTEMENT);
        return circuits.findByChefIdAndStatutOrderByDateCreationAsc(user.getId(), "CHEF_EN_ATTENTE");
    }

    @Transactional(readOnly = true)
    public List<CircuitDemandeConge> enAttenteDirecteur(Utilisateur user) {
        exiger(user, Role.DIRECTEUR);
        return circuits.findByDirecteurIdAndStatutOrderByDateCreationAsc(user.getId(), "DIRECTEUR_EN_ATTENTE");
    }

    @Transactional
    public CircuitDemandeConge retournerReliquat(Long id, Integer reliquat, String commentaire, Utilisateur actor) {
        exiger(actor, Role.ADMINISTRATION_PERSONNEL);
        if (reliquat == null || reliquat < 0) throw new IllegalArgumentException("Le reliquat doit être positif ou nul.");
        CircuitDemandeConge d = dossierVerrouille(id);
        if (!"RELIQUAT_EN_ATTENTE".equals(d.getStatut())) throw new IllegalStateException("Ce dossier n'attend pas le reliquat.");
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
        if (d.getReliquatJours() == null || d.getReliquatJours() < d.getNombreJours())
            throw new IllegalStateException("Reliquat insuffisant : " + d.getNombreJours() + " jour(s) demandés, "
                    + (d.getReliquatJours() == null ? 0 : d.getReliquatJours()) + " jour(s) retournés par l'administration.");
        int joursVerifies = demandesHistoriques.verifierEligibiliteWorkflow(d.getUtilisateurId(), d.getTypeCongeId(), d.getDateDebut(), d.getDateFin());
        if (joursVerifies != d.getNombreJours()) throw new IllegalStateException("Le nombre de jours ouvrés a changé. Recréez le dossier après vérification.");
        Utilisateur employe = utilisateurActif(d.getUtilisateurId());
        if (employe.getService_id() == null) throw new IllegalStateException("Le collaborateur n'est affecté à aucun service.");
        Services service = services.findById(employe.getService_id()).orElseThrow(() -> new IllegalStateException("Service du collaborateur introuvable."));
        if (service.getDepartementId() == null) throw new IllegalStateException("Le service du collaborateur n'est rattaché à aucun département.");
        Departement dep = departements.findById(service.getDepartementId()).orElseThrow(() -> new IllegalStateException("Département introuvable."));
        if (!Boolean.TRUE.equals(dep.getActif())) throw new IllegalStateException("Département inactif.");
        d.setDepartementId(dep.getId()); d.setChefId(dep.getChefId()); d.setDirecteurId(dep.getDirecteurId());
        d.setDateSoumissionChef(LocalDateTime.now()); d.setStatut("CHEF_EN_ATTENTE");
        CircuitDemandeConge saved = circuits.save(d);
        notifications.creerNotification(d.getChefId(), "Un dossier de congé (#" + d.getId() + ") attend votre examen.", "CIRCUIT_CONGE", "/dashboard/chef-departement");
        return saved;
    }

    @Transactional
    public CircuitDemandeConge transmettreAuDirecteur(Long id, Utilisateur chef) {
        exiger(chef, Role.CHEF_DEPARTEMENT);
        CircuitDemandeConge d = dossierVerrouille(id);
        if (!chef.getId().equals(d.getChefId())) throw new AccessDeniedException("Ce dossier ne relève pas de votre département.");
        if (!"CHEF_EN_ATTENTE".equals(d.getStatut())) throw new IllegalStateException("Ce dossier n'attend pas l'avis du chef.");
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
            demandesHistoriques.verifierEligibiliteWorkflow(d.getUtilisateurId(), d.getTypeCongeId(), d.getDateDebut(), d.getDateFin());
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
