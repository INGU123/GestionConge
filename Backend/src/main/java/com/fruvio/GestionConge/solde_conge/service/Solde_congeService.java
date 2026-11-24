package com.fruvio.GestionConge.solde_conge.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fruvio.GestionConge.employe.entity.Employe;
import com.fruvio.GestionConge.demande_conge.entity.DemandeConge;
import com.fruvio.GestionConge.demande_conge.repository.DemandeCongeRepository;
import com.fruvio.GestionConge.employe.repository.EmployeRepository;
import com.fruvio.GestionConge.historique_mouvement.service.Historique_mouvementService;
import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@Service
public class Solde_congeService {
    private final Solde_congeRepository soldeCongeRepository;
    private final Type_congeRepository typeCongeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmployeRepository employeRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final Historique_mouvementService historiqueMouvementService;

    @Autowired
    public Solde_congeService(Solde_congeRepository soldeCongeRepository, Type_congeRepository typeCongeRepository,
            UtilisateurRepository utilisateurRepository, EmployeRepository employeRepository,
            DemandeCongeRepository demandeCongeRepository, Historique_mouvementService historiqueMouvementService) {
        this.soldeCongeRepository = soldeCongeRepository;
        this.typeCongeRepository = typeCongeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.employeRepository = employeRepository;
        this.demandeCongeRepository = demandeCongeRepository;
        this.historiqueMouvementService = historiqueMouvementService;
    }

    // Constructeurs conservés pour les tests et appels historiques.
    public Solde_congeService(Solde_congeRepository soldeCongeRepository, Type_congeRepository typeCongeRepository,
            UtilisateurRepository utilisateurRepository, EmployeRepository employeRepository,
            Historique_mouvementService historiqueMouvementService) {
        this(soldeCongeRepository, typeCongeRepository, utilisateurRepository, employeRepository, null,
                historiqueMouvementService);
    }

    public Solde_congeService(Solde_congeRepository soldeCongeRepository, Type_congeRepository typeCongeRepository,
            UtilisateurRepository utilisateurRepository, Historique_mouvementService historiqueMouvementService) {
        this(soldeCongeRepository, typeCongeRepository, utilisateurRepository, null, null,
                historiqueMouvementService);
    }

    public List<Solde_conge> getSoldesParUtilisateur(Long utilisateurId, Utilisateur currentUser) {
        if (currentUser == null) throw new AccessDeniedException("Authentification requise.");
        if (currentUser.getRole() == Role.EMPLOYE && !Objects.equals(currentUser.getId(), utilisateurId))
            throw new AccessDeniedException("Vous ne pouvez consulter que vos propres soldes.");
        if (currentUser.getRole() == Role.MANAGER && !Objects.equals(currentUser.getId(), utilisateurId)) {
            Utilisateur target = utilisateurRepository.findById(utilisateurId).orElse(null);
            if (target == null || !Objects.equals(target.getManager_id(), currentUser.getId()))
                throw new AccessDeniedException("Accès non autorisé : cet employé n'appartient pas à votre équipe.");
        }
        return filtrerSoldesAvecTypeExistant(soldeCongeRepository.findByUtilisateurId(utilisateurId));
    }

    public List<Solde_conge> getSoldesParEmploye(Long employeId, Utilisateur currentUser) {
        exigerRoles(currentUser, Role.ADMIN, Role.ADMINISTRATION_PERSONNEL);
        employeRepository.findById(employeId)
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable."));
        return filtrerSoldesAvecTypeExistant(soldeCongeRepository.findByEmployeId(employeId)).stream()
                .peek(Solde_conge::exposerSoldesEmployeEnJours).toList();
    }

    @Transactional
    public Solde_conge enregistrerSoldeEmploye(Long employeId, Long typeCongeId, int periode,
            int joursAcquis, Utilisateur acteur) {
        exigerRoles(acteur, Role.ADMINISTRATION_PERSONNEL);
        if (joursAcquis < 0) throw new IllegalArgumentException("Le nombre de jours acquis ne peut pas être négatif.");
        Employe employe = employeRepository.findById(employeId).filter(Employe::isActif)
                .orElseThrow(() -> new IllegalArgumentException("Employé actif introuvable."));
        Type_conge type = typeCongeRepository.findById(typeCongeId).filter(t -> Boolean.TRUE.equals(t.getActif()))
                .orElseThrow(() -> new IllegalArgumentException("Type de congé actif introuvable."));
        if (soldeCongeRepository.existsByEmployeIdAndTypeCongeIdAndPeriode(employe.getId(), type.getId(), periode))
            throw new IllegalStateException("Un solde existe déjà pour cet employé, ce type et cette période.");
        Solde_conge solde = new Solde_conge();
        solde.setEmployeId(employe.getId());
        solde.setTypeCongeId(type.getId());
        solde.setPeriode(periode);
        // Pour un employé du référentiel, 1 jour = 2 unités de demi-journée.
        int acquisDemiJournees = Math.multiplyExact(joursAcquis, 2);
        solde.setEnDemiJournees(true);
        solde.setSoldeAquis(acquisDemiJournees);
        solde.setSoldePris(0);
        solde.setSoldeRestant(acquisDemiJournees);
        solde.setDate_maj(new Timestamp(System.currentTimeMillis()));
        return soldeCongeRepository.save(solde);
    }

    /**
     * Calcule automatiquement le droit annuel légal : 2,5 jours par mois complet de service effectif.
     * Les colonnes entières des soldes employés sont stockées en demi-journées (5 unités par mois).
     * Les périodes déjà enregistrées ne sont jamais écrasées.
     */
    @Transactional
    public Solde_conge calculerSoldeAnnuelEmploye(Long employeId, Long typeCongeId, int periode, Utilisateur acteur) {
        exigerRoles(acteur, Role.ADMIN, Role.ADMINISTRATION_PERSONNEL);
        Employe employe = employeRepository.findById(employeId).filter(Employe::isActif)
                .orElseThrow(() -> new IllegalArgumentException("Employé actif introuvable."));
        if (employe.getDateEmbauche() == null)
            throw new IllegalStateException("La date d'embauche doit être renseignée pour calculer les droits.");
        Type_conge type = typeCongeRepository.findById(typeCongeId).filter(t -> Boolean.TRUE.equals(t.getActif()))
                .orElseThrow(() -> new IllegalArgumentException("Type de congé actif introuvable."));
        String code = type.getCode() == null ? "" : type.getCode().toUpperCase();
        String libelle = type.getLibelle() == null ? "" : type.getLibelle().toUpperCase();
        if (!code.contains("ANNUEL") && !libelle.contains("ANNUEL"))
            throw new IllegalArgumentException("Le calcul légal automatique s'applique uniquement au congé annuel.");
        LocalDate debutExercice = LocalDate.of(periode, 1, 1);
        LocalDate finExercice = LocalDate.of(periode, 12, 31);
        var existing = soldeCongeRepository.findByEmployeIdAndTypeCongeIdAndPeriode(employeId, typeCongeId, periode);
        if (existing.isPresent()) {
            Solde_conge saved = existing.get();
            saved.exposerSoldesEmployeEnJours();
            return saved;
        }
        if (demandeCongeRepository == null)
            throw new IllegalStateException("L'historique des congés est indisponible : calcul automatique refusé.");
        // L'historique validé est la source des jours déjà consommés :
        // aucun solde RH ne doit être saisi manuellement pour le reconstituer.
        LocalDate limite = LocalDate.now().isBefore(finExercice) ? LocalDate.now() : finExercice;
        int moisComplets = 0;
        int indexMois = 1;
        LocalDate prochaineEcheance = employe.getDateEmbauche().plusMonths(indexMois).minusDays(1);
        while (!prochaineEcheance.isAfter(limite)) {
            if (!prochaineEcheance.isBefore(debutExercice)) moisComplets++;
            indexMois++;
            prochaineEcheance = employe.getDateEmbauche().plusMonths(indexMois).minusDays(1);
        }
        int acquisDemiJournees = moisComplets * 5;
        int joursPris = demandeCongeRepository.findByEmployeIdAndTypeCongeId(employeId, typeCongeId).stream()
                .filter(d -> "VALIDEE".equalsIgnoreCase(d.getStatut()))
                .filter(d -> d.getDateDebut() != null && d.getDateFin() != null && d.getNombreJours() != null)
                .mapToInt(d -> joursConsommesPourExercice(d.getDateDebut(), d.getDateFin(),
                        d.getNombreJours(), periode))
                .sum();
        int prisDemiJournees = Math.multiplyExact(joursPris, 2);
        int restantDemiJournees = Math.max(0, acquisDemiJournees - prisDemiJournees);
        Solde_conge solde = new Solde_conge();
        solde.setEmployeId(employeId);
        solde.setTypeCongeId(typeCongeId);
        solde.setPeriode(periode);
        solde.setEnDemiJournees(true);
        solde.setSoldeAquis(acquisDemiJournees);
        solde.setSoldePris(prisDemiJournees);
        solde.setSoldeRestant(restantDemiJournees);
        solde.setDate_maj(new Timestamp(System.currentTimeMillis()));
        Solde_conge saved = soldeCongeRepository.save(solde);
        saved.exposerSoldesEmployeEnJours();
        return saved;
    }

    /**
     * Permissions exceptionnelles pour événements familiaux : plafond légal global
     * de 10 jours par année civile (Code du travail, loi 2024-014, art. 126).
     * Le congé de paternité est traité séparément : 3 jours (art. 134).
     * Seules les demandes VALIDEE consomment le plafond.
     */
    @Transactional(readOnly = true)
    public int calculerPermissionFamilialeDisponible(Long employeId, Long typeCongeId, int annee) {
        if (demandeCongeRepository == null)
            throw new IllegalStateException("L'historique des congés est indisponible : calcul refusé.");
        Type_conge typeCible = typeCongeRepository.findById(typeCongeId)
                .orElseThrow(() -> new IllegalArgumentException("Type de congé introuvable."));
        String codeCible = typeCible.getCode() == null ? "" : typeCible.getCode().trim().toUpperCase();

        if ("NAISS_PAT".equals(codeCible)) {
            int pris = demandeCongeRepository.findByEmployeIdAndTypeCongeId(employeId, typeCongeId).stream()
                    .filter(d -> "VALIDEE".equalsIgnoreCase(d.getStatut()))
                    .filter(d -> d.getDateDebut() != null && d.getDateFin() != null && d.getNombreJours() != null)
                    .mapToInt(d -> joursConsommesPourExercice(d.getDateDebut(), d.getDateFin(), d.getNombreJours(), annee))
                    .sum();
            return Math.max(0, 3 - pris);
        }

        Set<String> permissionsFamiliales = Set.of(
                "MAR_SAL", "MAR_ENF", "DEC_CONJ", "CIRC_ENF", "DEM_LOG", "SINISTRE", "URGENCE", "AUT_FAM");
        List<Type_conge> typesFamiliaux = typeCongeRepository.findAll().stream()
                .filter(t -> t.getCode() != null && permissionsFamiliales.contains(t.getCode().trim().toUpperCase()))
                .toList();
        int consommes = typesFamiliaux.stream()
                .flatMap(t -> demandeCongeRepository.findByEmployeIdAndTypeCongeId(employeId, t.getId()).stream())
                .filter(d -> "VALIDEE".equalsIgnoreCase(d.getStatut()))
                .filter(d -> d.getDateDebut() != null && d.getDateFin() != null && d.getNombreJours() != null)
                .mapToInt(d -> joursConsommesPourExercice(d.getDateDebut(), d.getDateFin(), d.getNombreJours(), annee))
                .sum();
        return Math.max(0, 10 - consommes);
    }

    private int joursConsommesPourExercice(LocalDate debut, LocalDate fin, int nombreJours, int periode) {
        LocalDate debutAnnee = LocalDate.of(periode, 1, 1);
        LocalDate finAnnee = LocalDate.of(periode, 12, 31);
        LocalDate debutSegment = debut.isAfter(debutAnnee) ? debut : debutAnnee;
        LocalDate finSegment = fin.isBefore(finAnnee) ? fin : finAnnee;
        if (finSegment.isBefore(debutSegment)) return 0;
        long totalCalendrier = java.time.temporal.ChronoUnit.DAYS.between(debut, fin) + 1;
        long joursCalendrierSegment = java.time.temporal.ChronoUnit.DAYS.between(debutSegment, finSegment) + 1;
        if (totalCalendrier <= 0) return 0;
        if (totalCalendrier == joursCalendrierSegment) return nombreJours;
        return (int) Math.round((double) nombreJours * joursCalendrierSegment / totalCalendrier);
    }

    public List<Solde_conge> getAllSoldes(Utilisateur currentUser) {
        if (currentUser == null) throw new AccessDeniedException("Authentification requise.");
        if (currentUser.getRole() == Role.ADMIN)
            return filtrerSoldesAvecTypeExistant(soldeCongeRepository.findAll());
        if (currentUser.getRole() == Role.MANAGER) {
            List<Utilisateur> subordinates = utilisateurRepository.findByManagerId(currentUser.getId());
            List<Long> ids = new ArrayList<>(subordinates.stream().map(Utilisateur::getId).toList());
            if (!ids.contains(currentUser.getId())) ids.add(currentUser.getId());
            if (ids.isEmpty()) return Collections.emptyList();
            return filtrerSoldesAvecTypeExistant(soldeCongeRepository.findByUtilisateurIdIn(ids));
        }
        throw new AccessDeniedException("Accès refusé : consultation globale réservée aux managers et administrateurs.");
    }

    private List<Solde_conge> filtrerSoldesAvecTypeExistant(List<Solde_conge> soldes) {
        Set<Long> typesExistants = typeCongeRepository.findAll().stream().map(Type_conge::getId).collect(Collectors.toSet());
        return soldes.stream().filter(s -> typesExistants.contains(s.getTypeCongeId())).toList();
    }

    @Transactional
    public void initialiserSoldesPourUtilisateur(Long utilisateurId, int annee) {
        for (Type_conge type : typeCongeRepository.findAll()) {
            if (!soldeCongeRepository.existsByUtilisateurIdAndTypeCongeIdAndPeriode(utilisateurId, type.getId(), annee)) {
                int nbJours = type.getNombreJoursParAn() != null ? type.getNombreJoursParAn() : 0;
                Solde_conge solde = new Solde_conge();
                solde.setUtilisateurId(utilisateurId); solde.setTypeCongeId(type.getId()); solde.setPeriode(annee);
                solde.setSoldeAquis(nbJours); solde.setSoldePris(0); solde.setSoldeRestant(nbJours);
                solde.setDate_maj(new Timestamp(System.currentTimeMillis()));
                soldeCongeRepository.save(solde);
            }
        }
    }

    @Transactional
    public Solde_conge ajusterSolde(Long soldeId, int nouveauNombreJoursRestants) {
        return ajusterSolde(soldeId, nouveauNombreJoursRestants, null, null);
    }
    @Transactional
    public Solde_conge ajusterSolde(Long soldeId, int nouveauNombreJoursRestants, Long acteurId) {
        return ajusterSolde(soldeId, nouveauNombreJoursRestants, acteurId, null);
    }

    @Transactional
    public Solde_conge ajusterSolde(Long soldeId, int nouveauNombreJoursRestants, Long acteurId, String motif) {
        if (acteurId == null) throw new AccessDeniedException("Un administrateur authentifié est requis.");
        if (motif == null || motif.trim().length() < 10)
            throw new IllegalArgumentException("Le motif de correction doit contenir au moins 10 caractères.");
        if (nouveauNombreJoursRestants < 0) throw new IllegalArgumentException("Le solde ne peut pas être négatif.");
        Solde_conge solde = soldeCongeRepository.findById(soldeId)
                .orElseThrow(() -> new IllegalArgumentException("Solde introuvable"));
        if (Objects.equals(solde.getUtilisateurId(), acteurId))
            throw new AccessDeniedException("Vous ne pouvez pas ajuster votre propre solde.");
        int facteur = solde.getEmployeId() != null ? 2 : 1;
        int ancienRestantStocke = solde.getEmployeId() != null
                ? solde.getSoldeRestantEnDemiJournees()
                : (solde.getSoldeRestant() == null ? 0 : solde.getSoldeRestant());
        int joursDejaPris = solde.getEmployeId() != null
                ? solde.getSoldePrisEnDemiJournees()
                : (solde.getSoldePris() == null ? 0 : solde.getSoldePris());
        int ancienRestant = ancienRestantStocke / facteur;
        if (solde.getEmployeId() != null) solde.setEnDemiJournees(true);
        int nouveauRestantStocke = Math.multiplyExact(nouveauNombreJoursRestants, facteur);
        solde.setSoldeAquis(joursDejaPris + nouveauRestantStocke);
        solde.setSoldeRestant(nouveauRestantStocke);
        solde.setDate_maj(new Timestamp(System.currentTimeMillis()));
        Solde_conge saved = soldeCongeRepository.save(solde);
        int delta = Math.abs(nouveauRestantStocke - ancienRestantStocke) / facteur;
        String commentaire = "Ajustement manuel du solde : " + ancienRestant + " -> " + nouveauNombreJoursRestants
                + " | Motif : " + motif.trim();
        if (solde.getEmployeId() != null)
            historiqueMouvementService.enregistrerMouvementPourEmploye(solde.getEmployeId(), "AJUSTEMENT_SOLDE",
                    delta, solde.getTypeCongeId(), null, commentaire, acteurId);
        else if (solde.getUtilisateurId() != null)
            historiqueMouvementService.enregistrerMouvement(solde.getUtilisateurId(), "AJUSTEMENT_SOLDE",
                    delta, solde.getTypeCongeId(), null, commentaire, acteurId);
        return saved;
    }

    private void exigerRoles(Utilisateur user, Role... roles) {
        if (user == null) throw new AccessDeniedException("Authentification requise.");
        for (Role role : roles) if (user.getRole() == role) return;
        throw new AccessDeniedException("Accès non autorisé.");
    }
}
