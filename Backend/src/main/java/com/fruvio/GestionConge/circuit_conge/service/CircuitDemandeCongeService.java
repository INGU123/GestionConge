package com.fruvio.GestionConge.circuit_conge.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fruvio.GestionConge.circuit_conge.entity.CircuitDemandeConge;
import com.fruvio.GestionConge.circuit_conge.repository.CircuitDemandeCongeRepository;
import com.fruvio.GestionConge.confirmation_reliquat.entity.ConfirmationReliquat;
import com.fruvio.GestionConge.confirmation_reliquat.repository.ConfirmationReliquatRepository;
import com.fruvio.GestionConge.demande_conge.entity.DemandeConge;
import com.fruvio.GestionConge.demande_conge.service.DemandeCongeService;
import com.fruvio.GestionConge.employe.entity.Employe;
import com.fruvio.GestionConge.employe.repository.EmployeRepository;
import com.fruvio.GestionConge.departement.entity.Departement;
import com.fruvio.GestionConge.departement.repository.DepartementRepository;
import com.fruvio.GestionConge.jour_ferie.service.JourFerieService;
import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.historique_mouvement.service.Historique_mouvementService;
import com.fruvio.GestionConge.notifications_conge.service.Notification_congeService;
import com.fruvio.GestionConge.service_conge.entity.Services;
import com.fruvio.GestionConge.service_conge.repository.Service_congeRepository;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@Service
public class CircuitDemandeCongeService {
    private final CircuitDemandeCongeRepository circuits;
    private final UtilisateurRepository utilisateurs;
    private final EmployeRepository employes;
    private final Type_congeRepository typesConge;
    private final Service_congeRepository services;
    private final DepartementRepository departements;
    private final JourFerieService joursFeries;
    private final DemandeCongeService demandesHistoriques;
    private final Notification_congeService notifications;
    private final Solde_congeRepository soldes;
    private final Historique_mouvementService mouvements;
    private final ConfirmationReliquatRepository confirmations;

    @Autowired
    public CircuitDemandeCongeService(CircuitDemandeCongeRepository circuits, UtilisateurRepository utilisateurs,
            EmployeRepository employes, Type_congeRepository typesConge, Service_congeRepository services,
            DepartementRepository departements, JourFerieService joursFeries,
            DemandeCongeService demandesHistoriques, Notification_congeService notifications,
            Solde_congeRepository soldes, Historique_mouvementService mouvements,
            ConfirmationReliquatRepository confirmations) {
        this.circuits = circuits; this.utilisateurs = utilisateurs; this.employes = employes;
        this.typesConge = typesConge; this.services = services; this.departements = departements;
        this.joursFeries = joursFeries; this.demandesHistoriques = demandesHistoriques;
        this.notifications = notifications; this.soldes = soldes; this.mouvements = mouvements;
        this.confirmations = confirmations;
    }

    // Constructeur de compatibilité pour les appels et tests utilisant l'ancienne signature.
    public CircuitDemandeCongeService(CircuitDemandeCongeRepository circuits, UtilisateurRepository utilisateurs,
            EmployeRepository employes, Type_congeRepository typesConge, Service_congeRepository services,
            DepartementRepository departements, JourFerieService joursFeries,
            DemandeCongeService demandesHistoriques, Notification_congeService notifications,
            Solde_congeRepository soldes, Historique_mouvementService mouvements) {
        this(circuits, utilisateurs, employes, typesConge, services, departements, joursFeries,
                demandesHistoriques, notifications, soldes, mouvements, null);
    }

    // Constructeur de compatibilité pour les tests unitaires historiques.
    public CircuitDemandeCongeService(CircuitDemandeCongeRepository circuits, UtilisateurRepository utilisateurs,
            EmployeRepository employes, Type_congeRepository typesConge, Service_congeRepository services,
            DepartementRepository departements, JourFerieService joursFeries,
            DemandeCongeService demandesHistoriques, Notification_congeService notifications) {
        this(circuits, utilisateurs, employes, typesConge, services, departements, joursFeries,
                demandesHistoriques, notifications, null, null, null);
    }

    public CircuitDemandeCongeService(CircuitDemandeCongeRepository circuits, UtilisateurRepository utilisateurs,
            Service_congeRepository services, DepartementRepository departements, JourFerieService joursFeries,
            DemandeCongeService demandesHistoriques, Notification_congeService notifications) {
        this.circuits = circuits; this.utilisateurs = utilisateurs; this.employes = null; this.typesConge = null;
        this.services = services; this.departements = departements; this.joursFeries = joursFeries;
        this.demandesHistoriques = demandesHistoriques; this.notifications = notifications;
        this.soldes = null; this.mouvements = null; this.confirmations = null;
    }

    @Transactional
    public CircuitDemandeConge creerPourEmploye(Long employeId, Long typeId,
            java.time.LocalDate debut, java.time.LocalDate fin, String commentaire, Utilisateur assistant) {
        exiger(assistant, Role.ASSISTANT_DIRECTION);
        Employe employe = employes.findById(employeId).filter(Employe::isActif)
                .orElseThrow(() -> new IllegalArgumentException("Employé actif introuvable. Recherchez son matricule exact."));
        if (employe.getServiceId() == null)
            throw new IllegalStateException("Aucun service n'est associé à ce collaborateur dans le référentiel.");
        Services serviceChoisi = services.findById(employe.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Le service du collaborateur est introuvable."));
        if (serviceChoisi.getDepartementId() == null) throw new IllegalStateException("Service sans département.");
        Departement dep = departements.findById(serviceChoisi.getDepartementId())
                .orElseThrow(() -> new IllegalStateException("Département introuvable."));
        if (!Boolean.TRUE.equals(dep.getActif()) || dep.getChefId() == null || dep.getDirecteurId() == null)
            throw new IllegalStateException("Le département doit être actif et avoir un chef et un directeur.");
        if (debut == null || fin == null || fin.isBefore(debut)) throw new IllegalArgumentException("Période invalide.");
        var typeConge = typeId == null ? null : typesConge.findById(typeId)
                .filter(t -> Boolean.TRUE.equals(t.getActif()))
                .orElseThrow(() -> new IllegalArgumentException("Type de congé indisponible."));
        if (typeConge == null) throw new IllegalArgumentException("Type de congé indisponible.");
        String codeType = typeConge.getCode() == null ? "" : typeConge.getCode().toUpperCase();
        String libelleType = typeConge.getLibelle() == null ? "" : typeConge.getLibelle().toUpperCase();
        if (codeType.contains("ANNUEL") || libelleType.contains("ANNUEL")) {
            if (employe.getDateEmbauche() == null)
                throw new IllegalStateException("La date d'embauche doit être renseignée avant toute demande de congé annuel.");
            if (employe.getDateEmbauche().plusMonths(12).isAfter(debut))
                throw new IllegalStateException("Le droit de jouissance du congé annuel s'ouvre après 12 mois de service effectif.");
        }
        int nombre = joursFeries.compterJoursCalendaires(debut, fin);
        if (nombre <= 0) throw new IllegalArgumentException("La période ne contient aucun jour ouvré.");
        verifierChevauchementEmploye(employeId, debut, fin, null);
        CircuitDemandeConge saved = circuits.save(CircuitDemandeConge.builder()
                .employeId(employe.getId()).typeCongeId(typeId).assistantId(assistant.getId())
                .departementId(dep.getId()).dateDebut(debut).dateFin(fin).nombreJours(nombre)
                .commentaire(commentaire).statut("RELIQUAT_EN_ATTENTE").dateCreation(LocalDateTime.now()).build());
        for (Utilisateur agent : utilisateurs.findByRoleAndActifTrue(Role.ADMINISTRATION_PERSONNEL))
            notifications.creerNotification(agent.getId(), "Nouvelle demande de reliquat (dossier #" + saved.getId() + ").",
                    "CIRCUIT_CONGE", "/dashboard/reliquats");
        return enrichir(saved);
    }

    private void verifierChevauchementEmploye(Long employeId, java.time.LocalDate debut,
            java.time.LocalDate fin, Long dossierIgnore) {
        for (CircuitDemandeConge autre : circuits.findByEmployeId(employeId)) {
            if (dossierIgnore != null && dossierIgnore.equals(autre.getId())) continue;
            if ("REFUSEE".equals(autre.getStatut())) continue;
            if (!(fin.isBefore(autre.getDateDebut()) || debut.isAfter(autre.getDateFin())))
                throw new IllegalStateException("Cette période chevauche un autre dossier de congé déjà enregistré.");
        }
    }

    @Transactional
    public CircuitDemandeConge creer(Long employeId, Long typeId, Long serviceId,
            java.time.LocalDate debut, java.time.LocalDate fin, String commentaire, Utilisateur assistant) {
        exiger(assistant, Role.ASSISTANT_DIRECTION);
        utilisateurActif(employeId);
        if (serviceId == null) throw new IllegalArgumentException("Le service est obligatoire.");
        Services serviceChoisi = services.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service sélectionné introuvable."));
        if (serviceChoisi.getDepartementId() == null)
            throw new IllegalStateException("Le service choisi n'est rattaché à aucun département.");
        Departement departementChoisi = departements.findById(serviceChoisi.getDepartementId())
                .orElseThrow(() -> new IllegalStateException("Département du service introuvable."));
        if (!Boolean.TRUE.equals(departementChoisi.getActif()))
            throw new IllegalStateException("Le département du service est inactif.");
        if (departementChoisi.getChefId() == null || departementChoisi.getDirecteurId() == null)
            throw new IllegalStateException("Le département doit avoir un chef et un directeur actifs.");
        if (debut == null || fin == null || fin.isBefore(debut)) throw new IllegalArgumentException("Période de congé invalide.");
        if (typeId == null) throw new IllegalArgumentException("Le type de congé est obligatoire.");
        int nombre = joursFeries.compterJoursCalendaires(debut, fin);
        if (nombre <= 0) throw new IllegalArgumentException("La période ne contient aucun jour ouvré.");
        verifierChevauchementCircuit(employeId, debut, fin, null);
        CircuitDemandeConge saved = circuits.save(CircuitDemandeConge.builder().utilisateurId(employeId).typeCongeId(typeId)
                .assistantId(assistant.getId()).departementId(departementChoisi.getId())
                .dateDebut(debut).dateFin(fin).nombreJours(nombre)
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
        return circuits.findByStatutOrderByDateCreationAsc("RELIQUAT_EN_ATTENTE").stream().map(this::enrichir).toList();
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
        if (d.getEmployeId() != null)
            throw new IllegalStateException("Utilisez la confirmation RH avec période et référence pour un employé du référentiel.");
        if (d.getEmployeId() == null) {
            int disponible = demandesHistoriques.calculerReliquatDisponible(d.getUtilisateurId(), d.getTypeCongeId(), d.getDateDebut(), d.getDateFin());
            if (reliquat > disponible) throw new IllegalArgumentException("Le reliquat saisi (" + reliquat + " j) dépasse le solde disponible enregistré (" + disponible + " j).");
        }
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
        if (d.getEmployeId() != null) verifierChevauchementEmploye(d.getEmployeId(), d.getDateDebut(), d.getDateFin(), d.getId());
        else verifierChevauchementCircuit(d.getUtilisateurId(), d.getDateDebut(), d.getDateFin(), d.getId());
        if (d.getEmployeId() != null) {
            int reliquat = d.getReliquatJours() == null ? 0 : d.getReliquatJours();
            int disponibleExercices = soldes.findByEmployeIdAndTypeCongeId(d.getEmployeId(), d.getTypeCongeId()).stream()
                    .filter(s -> s.getPeriode() >= d.getDateDebut().getYear()
                            && s.getPeriode() <= d.getDateFin().getYear())
                    .mapToInt(s -> Math.max(0, s.getSoldeAquisEnDemiJournees()
                            - s.getSoldePrisEnDemiJournees()) / 2).sum();
            if (reliquat + disponibleExercices < d.getNombreJours())
                throw new IllegalStateException("Couverture insuffisante : " + reliquat + " j de reliquat + "
                        + disponibleExercices + " j disponibles, pour " + d.getNombreJours() + " j demandés.");
        } else if (d.getReliquatJours() == null || d.getReliquatJours() < d.getNombreJours()) {
            throw new IllegalStateException("Reliquat insuffisant : " + d.getNombreJours() + " jour(s) demandés, "
                    + (d.getReliquatJours() == null ? 0 : d.getReliquatJours()) + " jour(s) retournés par l'administration.");
        }
        int joursVerifies = d.getEmployeId() != null
                ? joursFeries.compterJoursCalendaires(d.getDateDebut(), d.getDateFin())
                : demandesHistoriques.verifierEligibiliteWorkflow(d.getUtilisateurId(), d.getTypeCongeId(), d.getDateDebut(), d.getDateFin());
        if (joursVerifies != d.getNombreJours()) throw new IllegalStateException("Le nombre de jours calendaires a changé. Recréez le dossier après vérification.");
        if (d.getEmployeId() == null) utilisateurActif(d.getUtilisateurId());
        else employes.findById(d.getEmployeId()).filter(Employe::isActif).orElseThrow(() -> new IllegalStateException("Employé inactif ou introuvable."));
        if (d.getDepartementId() == null)
            throw new IllegalStateException("Le département associé au dossier est introuvable.");
        Departement dep = departements.findById(d.getDepartementId())
                .orElseThrow(() -> new IllegalStateException("Département introuvable."));
        if (!Boolean.TRUE.equals(dep.getActif()))
            throw new IllegalStateException("Département inactif.");
        if (dep.getChefId() == null || dep.getDirecteurId() == null)
            throw new IllegalStateException("Le département doit avoir un chef et un directeur.");
        d.setChefId(dep.getChefId());
        d.setDirecteurId(dep.getDirecteurId());
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

        if (d.getEmployeId() == null) {
            if ("VALIDEE".equals(choix))
                demandesHistoriques.verifierEligibiliteWorkflow(d.getUtilisateurId(), d.getTypeCongeId(), d.getDateDebut(), d.getDateFin());
            DemandeConge legacy = DemandeConge.builder().utilisateurId(d.getUtilisateurId()).typeCongeId(d.getTypeCongeId())
                    .dateDebut(d.getDateDebut()).dateFin(d.getDateFin()).nombreJours(d.getNombreJours())
                    .commentaire(d.getCommentaire()).statut("EN_ATTENTE").dateCreation(java.time.LocalDate.now()).build();
            DemandeConge saved = demandesHistoriques.creerDemandePourCircuit(legacy);
            DemandeConge decisionResult = demandesHistoriques.traiterDemande(saved.getId(), null, choix, raison, directeur);
            d.setLegacyDemandeId(decisionResult.getId());
        } else {
            DemandeConge employeeHistory = DemandeConge.builder().employeId(d.getEmployeId())
                    .typeCongeId(d.getTypeCongeId()).dateDebut(d.getDateDebut()).dateFin(d.getDateFin())
                    .nombreJours(d.getNombreJours()).commentaire(d.getCommentaire()).statut(choix)
                    .motifRefus("REFUSEE".equals(choix) ? raison : null).valideePar(directeur.getId())
                    .dateValidation(java.time.LocalDate.now()).dateCreation(java.time.LocalDate.now()).build();
            DemandeConge saved = demandesHistoriques.creerDemandePourCircuit(employeeHistory);
            d.setLegacyDemandeId(saved.getId());
            if ("VALIDEE".equals(choix)) debiterSoldesEmploye(d, directeur, saved.getId());
        }
        d.setMotifRefus("REFUSEE".equals(choix) ? raison : null);
        d.setDateDecision(LocalDateTime.now()); d.setStatut(choix);
        CircuitDemandeConge savedCircuit = circuits.save(d);
        String contenu = "Votre demande de congé (dossier #" + d.getId() + ") a été "
                + ("VALIDEE".equals(choix) ? "VALIDÉE." : "REFUSÉE.");
        if ("REFUSEE".equals(choix)) contenu += " Motif du refus : " + raison;
        notifications.creerNotification(d.getEmployeId() == null ? d.getUtilisateurId() : d.getAssistantId(), contenu,
                "DECISION_CONGE", "/circuit-conge/" + d.getId() + "/pdf");
        return savedCircuit;
    }

    /**
     * Débite uniquement après la décision finale du directeur.
     * Les soldes doivent avoir été saisis explicitement par l'administration ; aucun quota SPAT n'est inventé.
     */
    private void debiterSoldesEmploye(CircuitDemandeConge dossier, Utilisateur directeur, Long demandeId) {
        if (soldes == null || mouvements == null || confirmations == null)
            throw new IllegalStateException("Le service de soldes employés n'est pas configuré.");

        ConfirmationReliquat confirmation = confirmations
                .findByCircuitDemandeCongeIdOrderByDateConfirmationDesc(dossier.getId()).stream()
                .findFirst().orElseThrow(() -> new IllegalStateException("Confirmation RH du reliquat introuvable."));
        int reliquat = Math.min(dossier.getNombreJours(), confirmation.getNombreJoursConfirme() == null ? 0 : confirmation.getNombreJoursConfirme());
        int anneeReference = extraireAnneeReference(confirmation.getPeriodeReference());
        if (anneeReference >= dossier.getDateDebut().getYear())
            throw new IllegalStateException("L'exercice du reliquat doit précéder celui du congé.");

        Map<Integer, Integer> joursParAnnee = new TreeMap<>();
        LocalDate cursor = dossier.getDateDebut();
        while (!cursor.isAfter(dossier.getDateFin())) {
            int annee = cursor.getYear();
            LocalDate finAnnee = LocalDate.of(annee, 12, 31);
            LocalDate finSegment = dossier.getDateFin().isBefore(finAnnee) ? dossier.getDateFin() : finAnnee;
            int jours = joursFeries.compterJoursCalendaires(cursor, finSegment);
            if (jours > 0) joursParAnnee.put(annee, jours);
            cursor = finSegment.plusDays(1);
        }
        int total = joursParAnnee.values().stream().mapToInt(Integer::intValue).sum();
        if (total != dossier.getNombreJours())
            throw new IllegalStateException("Le nombre de jours calendaires ne correspond plus au dossier.");

        Solde_conge soldeReference = soldes.findByEmployeIdAndTypeCongeIdAndPeriode(
                dossier.getEmployeId(), dossier.getTypeCongeId(), anneeReference)
                .orElseThrow(() -> new IllegalStateException("Solde RH de l'exercice de référence introuvable."));
        int acquisReference = soldeReference.getSoldeAquisEnDemiJournees();
        int prisReference = soldeReference.getSoldePrisEnDemiJournees();
        if ((acquisReference - prisReference) / 2 < reliquat)
            throw new IllegalStateException("Le reliquat de l'exercice " + anneeReference + " n'est plus disponible.");

        int aDebiterSurExercicesConges = total - reliquat;
        Map<Integer, Solde_conge> soldesVerifies = new LinkedHashMap<>();
        Map<Integer, Integer> debitsParAnnee = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : joursParAnnee.entrySet()) {
            Solde_conge solde = soldes.findByEmployeIdAndTypeCongeIdAndPeriode(
                    dossier.getEmployeId(), dossier.getTypeCongeId(), entry.getKey())
                    .orElseThrow(() -> new IllegalStateException("Aucun solde employé saisi pour l'exercice "
                            + entry.getKey() + "."));
            int acquis = solde.getSoldeAquisEnDemiJournees();
            int pris = solde.getSoldePrisEnDemiJournees();
            int debit = Math.min(entry.getValue(), aDebiterSurExercicesConges);
            if ((acquis - pris) / 2 < debit)
                throw new IllegalStateException("Solde insuffisant pour l'exercice " + entry.getKey()
                        + " : " + debit + " jour(s) requis, " + (Math.max(0, acquis - pris) / 2) + " disponible(s).");
            soldesVerifies.put(entry.getKey(), solde);
            debitsParAnnee.put(entry.getKey(), debit);
            aDebiterSurExercicesConges -= debit;
        }
        if (aDebiterSurExercicesConges > 0)
            throw new IllegalStateException("Les soldes des exercices du congé ne couvrent pas le complément de "
                    + aDebiterSurExercicesConges + " jour(s).");

        soldeReference.setEnDemiJournees(true);
        soldeReference.setSoldePris(prisReference + reliquat * 2);
        soldeReference.setSoldeRestant(Math.max(0, acquisReference - soldeReference.getSoldePris()));
        soldeReference.setDate_maj(new java.sql.Timestamp(System.currentTimeMillis()));
        soldes.save(soldeReference);
        if (reliquat > 0) mouvements.enregistrerMouvementPourEmploye(dossier.getEmployeId(), "DEBIT_RELIQUAT",
                reliquat, dossier.getTypeCongeId(), demandeId,
                "Consommation du reliquat confirmé de l'exercice " + anneeReference, directeur.getId());

        for (Map.Entry<Integer, Integer> entry : debitsParAnnee.entrySet()) {
            int debit = entry.getValue();
            if (debit <= 0) continue;
            Solde_conge solde = soldesVerifies.get(entry.getKey());
            int pris = solde.getSoldePrisEnDemiJournees();
            int acquis = solde.getSoldeAquisEnDemiJournees();
            solde.setEnDemiJournees(true);
            solde.setSoldePris(pris + debit * 2);
            solde.setSoldeRestant(Math.max(0, acquis - solde.getSoldePris()));
            solde.setDate_maj(new java.sql.Timestamp(System.currentTimeMillis()));
            soldes.save(solde);
            mouvements.enregistrerMouvementPourEmploye(dossier.getEmployeId(), "DEBIT_CONGE",
                    debit, dossier.getTypeCongeId(), demandeId,
                    "Congé validé par le directeur #" + directeur.getId()
                            + " (exercice " + entry.getKey() + ")", directeur.getId());
        }
    }

    private int extraireAnneeReference(String periode) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?:19|20)\\d{2}").matcher(periode == null ? "" : periode);
        if (!matcher.find())
            throw new IllegalStateException("La période de référence RH doit contenir une année explicite.");
        return Integer.parseInt(matcher.group());
    }

    public CircuitDemandeConge dossierPourPdf(Long id, Utilisateur user) {
        if (user == null || user.getId() == null || !user.isEnabled()) {
            throw new AccessDeniedException("Authentification requise.");
        }

        CircuitDemandeConge dossier = circuits.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable."));

        boolean demandeur = dossier.getUtilisateurId() != null && user.getRole() != Role.DIRECTEUR
                && user.getId().equals(dossier.getUtilisateurId());
        boolean assistantCreateur = user.getRole() == Role.ASSISTANT_DIRECTION
                && user.getId().equals(dossier.getAssistantId());

        if (!demandeur && !assistantCreateur) {
            throw new AccessDeniedException("Téléchargement non autorisé.");
        }
        if (!List.of("VALIDEE", "REFUSEE").contains(dossier.getStatut())) {
            throw new IllegalStateException("Le PDF sera disponible après la décision du directeur.");
        }
        return dossier;
    }

    private CircuitDemandeConge enrichir(CircuitDemandeConge dossier) {
        if (dossier.getEmployeId() != null) {
            employes.findById(dossier.getEmployeId()).ifPresent(e -> {
                dossier.setUtilisateurNom(e.getNom()); dossier.setUtilisateurPrenom(e.getPrenom());
                dossier.setUtilisateurMatricule(e.getMatricule()); dossier.setUtilisateurFonction(e.getFonction());
            });
        } else if (dossier.getUtilisateurId() != null) {
            utilisateurs.findById(dossier.getUtilisateurId()).ifPresent(u -> {
                dossier.setUtilisateurNom(u.getNom()); dossier.setUtilisateurPrenom(u.getPrenom());
                dossier.setUtilisateurMatricule(u.getMatricule());
            });
        }
        if (confirmations != null && dossier.getId() != null) {
            confirmations.findByCircuitDemandeCongeIdOrderByDateConfirmationDesc(dossier.getId())
                    .stream().findFirst().ifPresent(c -> {
                        dossier.setReliquatPeriodeReference(c.getPeriodeReference());
                        dossier.setReliquatReferenceConfirmation(c.getReferenceConfirmation());
                        dossier.setReliquatJustificatif(c.getJustificatif());
                        dossier.setReliquatObservation(c.getObservation());
                        dossier.setReliquatDateConfirmation(c.getDateConfirmation());
                    });
        }
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
