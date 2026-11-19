package com.fruvio.GestionConge.confirmation_reliquat.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fruvio.GestionConge.circuit_conge.entity.CircuitDemandeConge;
import com.fruvio.GestionConge.circuit_conge.repository.CircuitDemandeCongeRepository;
import com.fruvio.GestionConge.confirmation_reliquat.entity.ConfirmationReliquat;
import com.fruvio.GestionConge.confirmation_reliquat.repository.ConfirmationReliquatRepository;
import com.fruvio.GestionConge.employe.entity.Employe;
import com.fruvio.GestionConge.employe.repository.EmployeRepository;
import com.fruvio.GestionConge.notifications_conge.service.Notification_congeService;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

@Service
public class ConfirmationReliquatService {
    private final ConfirmationReliquatRepository confirmations;
    private final CircuitDemandeCongeRepository circuits;
    private final EmployeRepository employes;
    private final Notification_congeService notifications;

    public ConfirmationReliquatService(ConfirmationReliquatRepository confirmations,
            CircuitDemandeCongeRepository circuits, EmployeRepository employes,
            Notification_congeService notifications) {
        this.confirmations = confirmations;
        this.circuits = circuits;
        this.employes = employes;
        this.notifications = notifications;
    }

    @Transactional
    public ConfirmationReliquat confirmer(Long dossierId, Integer jours, String periode,
            String reference, String justificatif, String observation, Utilisateur acteur) {
        exigerAdministration(acteur);
        if (jours == null || jours < 0) throw new IllegalArgumentException("Le nombre de jours confirmé doit être positif ou nul.");
        if (periode == null || periode.isBlank()) throw new IllegalArgumentException("La période de référence est obligatoire.");
        if (reference == null || reference.isBlank()) throw new IllegalArgumentException("La référence de confirmation est obligatoire.");

        CircuitDemandeConge dossier = circuits.findById(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable."));
        if (!"RELIQUAT_EN_ATTENTE".equals(dossier.getStatut()))
            throw new IllegalStateException("Ce dossier n'attend pas une confirmation de reliquat.");
        if (dossier.getEmployeId() == null)
            throw new IllegalStateException("Ce dossier historique est lié à un compte utilisateur ; utilisez son circuit historique.");
        Employe employe = employes.findById(dossier.getEmployeId()).filter(Employe::isActif)
                .orElseThrow(() -> new IllegalStateException("Employé actif introuvable."));
        if (confirmations.existsByReferenceConfirmation(reference.trim()))
            throw new IllegalArgumentException("Cette référence de confirmation existe déjà.");

        ConfirmationReliquat confirmation = confirmations.save(ConfirmationReliquat.builder()
                .circuitDemandeCongeId(dossier.getId())
                .employeId(employe.getId())
                .nombreJoursConfirme(jours)
                .periodeReference(periode.trim())
                .confirmePar(acteur.getId())
                .dateConfirmation(LocalDateTime.now())
                .referenceConfirmation(reference.trim())
                .justificatif(justificatif == null || justificatif.isBlank() ? null : justificatif.trim())
                .observation(observation == null || observation.isBlank() ? null : observation.trim())
                .build());

        dossier.setReliquatJours(jours);
        dossier.setReliquatCommentaire(observation);
        dossier.setAdministrationPersonnelId(acteur.getId());
        dossier.setDateReliquat(LocalDateTime.now());
        dossier.setStatut("RELIQUAT_RETOURNE");
        circuits.save(dossier);

        notifications.creerNotification(dossier.getAssistantId(),
                "Le reliquat du dossier #" + dossier.getId() + " a été confirmé (" + jours + " jour(s)).",
                "CIRCUIT_CONGE", "/dashboard/circuit-conge");
        return confirmation;
    }

    @Transactional(readOnly = true)
    public List<ConfirmationReliquat> historiqueDossier(Long dossierId, Utilisateur acteur) {
        exigerAdministration(acteur);
        return confirmations.findByCircuitDemandeCongeIdOrderByDateConfirmationDesc(dossierId);
    }

    private void exigerAdministration(Utilisateur acteur) {
        if (acteur == null || (acteur.getRole() != Role.ADMIN
                && acteur.getRole() != Role.ADMINISTRATION_PERSONNEL))
            throw new AccessDeniedException("Action réservée à l'administration du personnel.");
    }
}
