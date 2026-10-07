package com.fruvio.GestionConge.utilisateur.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fruvio.GestionConge.demande_conge.repository.DemandeCongeRepository;
import com.fruvio.GestionConge.historique_mouvement.repository.Historique_mouvementRepository;
import com.fruvio.GestionConge.notifications_conge.repository.NotificationRepository;
import com.fruvio.GestionConge.service_conge.repository.Service_congeRepository;
import com.fruvio.GestionConge.solde_conge.repository.Solde_congeRepository;
import com.fruvio.GestionConge.solde_conge.service.Solde_congeService;
import com.fruvio.GestionConge.utilisateur.dto.ChangePasswordRequest;
import com.fruvio.GestionConge.utilisateur.dto.UtilisateurCreateRequest;
import com.fruvio.GestionConge.utilisateur.dto.UtilisateurUpdateRequest;
import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;
import com.fruvio.GestionConge.utilisateur.repository.UtilisateurRepository;

@Service
public class UtilisateurService {

        private static final Logger log = LoggerFactory.getLogger(UtilisateurService.class);

        private final UtilisateurRepository utilisateurRepository;

        private final PasswordEncoder passwordEncoder;

        private final Solde_congeService soldeCongeService;

        private final EmailService emailService;

        private final DemandeCongeRepository demandeCongeRepository;

        private final Solde_congeRepository soldeCongeRepository;

        private final Historique_mouvementRepository historiqueRepository;

        private final NotificationRepository notificationRepository;

        private final Service_congeRepository serviceRepository;

        private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";

        private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();

        private static final String NUMBER = "0123456789";

        private static final String SPECIAL = "!@#$%^&*";

        private static final String PASSWORD_ALLOW_BASE = CHAR_LOWER + CHAR_UPPER + NUMBER + SPECIAL;

        private static final SecureRandom random = new SecureRandom();

        public UtilisateurService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder,
                        @Lazy Solde_congeService soldeCongeService, EmailService emailService,
                        DemandeCongeRepository demandeCongeRepository, Solde_congeRepository soldeCongeRepository,
                        Historique_mouvementRepository historiqueRepository,
                        NotificationRepository notificationRepository, Service_congeRepository serviceRepository) {

                this.utilisateurRepository = utilisateurRepository;
                this.passwordEncoder = passwordEncoder;
                this.soldeCongeService = soldeCongeService;
                this.emailService = emailService;
                this.demandeCongeRepository = demandeCongeRepository;
                this.soldeCongeRepository = soldeCongeRepository;
                this.historiqueRepository = historiqueRepository;
                this.notificationRepository = notificationRepository;
                this.serviceRepository = serviceRepository;
        }

        // =========================================================
        // GENERATION DE MOT DE PASSE TEMPORAIRE SECURISE
        // =========================================================

        public String generateRandomPassword(int length) {

                int pwdLength = Math.max(10, length);

                StringBuilder sb = new StringBuilder(pwdLength);

                // Garantir au moins un caractère de chaque type
                sb.append(CHAR_LOWER.charAt(random.nextInt(CHAR_LOWER.length())));

                sb.append(CHAR_UPPER.charAt(random.nextInt(CHAR_UPPER.length())));

                sb.append(NUMBER.charAt(random.nextInt(NUMBER.length())));

                sb.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

                for (int i = 4; i < pwdLength; i++) {

                        int index = random.nextInt(PASSWORD_ALLOW_BASE.length());

                        sb.append(PASSWORD_ALLOW_BASE.charAt(index));
                }

                // Mélange
                char[] chars = sb.toString().toCharArray();

                for (int i = chars.length - 1; i > 0; i--) {

                        int j = random.nextInt(i + 1);

                        char temp = chars[i];

                        chars[i] = chars[j];

                        chars[j] = temp;
                }

                return new String(chars);
        }

        // =========================================================
        // AUTHENTIFICATION / LOGIN
        // =========================================================

        public Utilisateur login(String identifier, String rawPassword) {

                if (identifier == null || identifier.trim().isEmpty() || rawPassword == null || rawPassword.isEmpty()) {

                        return null;
                }

                String search = identifier.trim();

                // Recherche prioritaire par matricule,
                // puis par email
                Optional<Utilisateur> userOpt = utilisateurRepository.findByMatricule(search);

                if (userOpt.isEmpty()) {

                        userOpt = utilisateurRepository.findByEmail(search);
                }

                if (userOpt.isEmpty()) {

                        log.warn("Tentative de connexion échouée : identifiant inexistant");

                        return null;
                }

                Utilisateur user = userOpt.get();

                // Vérifier que le compte est actif
                if (!user.isActif()) {

                        log.warn("Tentative de connexion rejetée : compte inactif [{}]", user.getMatricule());

                        return null;
                }

                // Vérifier le mot de passe avec BCrypt uniquement
                String storedHash = user.getPassword();

                if (storedHash == null || (!storedHash.startsWith("$2a$") && !storedHash.startsWith("$2b$")
                                && !storedHash.startsWith("$2y$"))) {

                        log.error("Compte avec mot de passe non haché BCrypt détecté pour [{}]. Connexion refusée.",
                                        user.getMatricule());

                        return null;
                }

                boolean matches = passwordEncoder.matches(rawPassword, storedHash);

                if (!matches) {

                        log.warn("Mot de passe invalide pour [{}]", user.getMatricule());

                        return null;
                }

                return user;
        }

        // =========================================================
        // CREATION UTILISATEUR
        // =========================================================

        @Transactional
        public Utilisateur createUtilisateur(UtilisateurCreateRequest request, Utilisateur currentUser) {

                if (request == null) {

                        throw new IllegalArgumentException("Données d'utilisateur obligatoires.");
                }

                String matricule = request.getMatricule() != null ? request.getMatricule().trim() : null;

                String email = request.getEmail() != null ? request.getEmail().trim() : null;

                if (matricule == null || matricule.isEmpty()) {

                        throw new IllegalArgumentException("Le matricule est obligatoire.");
                }

                if (email == null || email.isEmpty()) {

                        throw new IllegalArgumentException("L'adresse e-mail est obligatoire.");
                }

                if (utilisateurRepository.existsByMatricule(matricule)) {

                        throw new IllegalArgumentException("Le matricule " + matricule + " existe déjà.");
                }

                if (utilisateurRepository.existsByEmail(email)) {

                        throw new IllegalArgumentException("L'adresse e-mail " + email + " existe déjà.");
                }

                Role assignedRole = request.getRole() != null ? request.getRole() : Role.EMPLOYE;

                if (assignedRole == Role.ADMIN) {

                        throw new IllegalArgumentException(
                                        "La création d'un compte ADMIN n'est pas autorisée depuis cet endpoint.");
                }

                // Seul un ADMIN peut attribuer un rôle
                if (currentUser == null || currentUser.getRole() != Role.ADMIN) {

                        throw new AccessDeniedException("Seul un administrateur peut créer des utilisateurs.");
                }

                String rawPassword;

                if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {

                        rawPassword = request.getPassword().trim();

                } else {

                        rawPassword = generateRandomPassword(12);
                }

                Utilisateur newUser = Utilisateur.builder().matricule(matricule)
                                .nom(request.getNom() != null ? request.getNom().trim() : "")
                                .prenom(request.getPrenom() != null ? request.getPrenom().trim() : "").email(email)
                                .password(passwordEncoder.encode(rawPassword)).role(assignedRole)
                                .service_id(request.getService_id()).manager_id(request.getManager_id())
                                .date_embauche(request.getDate_embauche() != null ? request.getDate_embauche()
                                                : LocalDate.now())
                                .date_creation(LocalDate.now())
                                .actif(request.getActif() != null ? request.getActif() : true).build();

                Utilisateur saved = utilisateurRepository.save(newUser);

                // Initialisation cohérente des soldes
                try {

                        int currentYear = LocalDate.now().getYear();

                        soldeCongeService.initialiserSoldesPourUtilisateur(saved.getId(), currentYear);

                } catch (Exception e) {

                        log.error("Erreur lors de l'initialisation des soldes pour l'utilisateur #{}: {}",
                                        saved.getId(), e.getMessage(), e);
                }

                // Notification par e-mail des identifiants
                // Le mot de passe en clair est uniquement transmis par e-mail.
                try {

                        emailService.sendWelcomeEmail(saved.getEmail(), saved.getMatricule(), rawPassword);

                        log.info("E-mail de bienvenue envoyé avec succès pour l'utilisateur #{} [{}]", saved.getId(),
                                        saved.getEmail());

                } catch (IllegalStateException e) {

                        log.error("Échec de l'envoi de l'e-mail lors de la création de l'utilisateur #{} [{}]. Type: {}. Message: {}",
                                        saved.getId(), saved.getEmail(), e.getClass().getSimpleName(), e.getMessage());

                        /*
                         * L'échec de l'envoi est volontairement propagé.
                         *
                         * Comme createUtilisateur() est transactionnel, l'échec empêche la validation
                         * de la création du compte.
                         *
                         * Le compte ne doit donc pas être considéré comme créé si l'envoi obligatoire
                         * du mail de bienvenue échoue.
                         */
                        throw e;
                }

                /*
                 * Retourner l'entité enregistrée avec son hash BCrypt intact. Le mot de passe
                 * en clair n'est JAMAIS stocké dans l'entité.
                 */
                return saved;
        }

        // =========================================================
        // MISE A JOUR UTILISATEUR
        // =========================================================

        @Transactional
        public Utilisateur updateUtilisateur(UtilisateurUpdateRequest request, Utilisateur currentUser) {

                if (request == null || request.getId() == null) {

                        throw new IllegalArgumentException("L'identifiant de l'utilisateur est obligatoire.");
                }

                Utilisateur targetUser = utilisateurRepository.findById(request.getId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Utilisateur non trouvé avec l'identifiant " + request.getId()));

                if (currentUser == null) {

                        throw new AccessDeniedException("Opération non autorisée : utilisateur non authentifié.");
                }

                Role currentRole = currentUser.getRole();

                boolean isSelf = Objects.equals(currentUser.getId(), targetUser.getId());

                boolean isAdmin = currentRole == Role.ADMIN;

                // Contrôle des autorisations
                if (!isAdmin && !isSelf) {

                        throw new AccessDeniedException(
                                        "Vous ne disposez pas des privilèges nécessaires pour modifier ce compte.");
                }

                if (!isAdmin && (request.getRole() != null || request.getActif() != null
                                || request.getMatricule() != null || request.getService_id() != null
                                || request.getManager_id() != null)) {

                        throw new AccessDeniedException(
                                        "Seul un administrateur peut modifier les informations administratives d'un compte.");
                }

                // 1. Informations personnelles
                if (request.getNom() != null && !request.getNom().trim().isEmpty()) {

                        targetUser.setNom(request.getNom().trim());
                }

                if (request.getPrenom() != null && !request.getPrenom().trim().isEmpty()) {

                        targetUser.setPrenom(request.getPrenom().trim());
                }

                if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {

                        String newEmail = request.getEmail().trim();

                        if (!newEmail.equalsIgnoreCase(targetUser.getEmail())) {

                                if (utilisateurRepository.existsByEmail(newEmail)) {

                                        throw new IllegalArgumentException(
                                                        "L'adresse e-mail " + newEmail + " est déjà utilisée.");
                                }

                                targetUser.setEmail(newEmail);
                        }
                }

                // 2. Modification du mot de passe
                if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {

                        targetUser.setPassword(passwordEncoder.encode(request.getPassword().trim()));
                }

                // 3. Champs administratifs protégés

                // Rôle
                if (request.getRole() != null && request.getRole() != targetUser.getRole()) {

                        if (!isAdmin) {

                                throw new AccessDeniedException(
                                                "Seul un administrateur est autorisé à modifier le rôle.");
                        }

                        targetUser.setRole(request.getRole());
                }

                // Actif
                if (request.getActif() != null && request.getActif() != targetUser.isActif()) {

                        if (!isAdmin) {

                                throw new AccessDeniedException(
                                                "Seul un administrateur est autorisé à activer ou désactiver un compte.");
                        }

                        targetUser.setActif(request.getActif());
                }

                // Matricule
                if (request.getMatricule() != null && !request.getMatricule().trim().isEmpty()
                                && !request.getMatricule().trim().equals(targetUser.getMatricule())) {

                        if (!isAdmin) {

                                throw new AccessDeniedException(
                                                "Seul un administrateur est autorisé à modifier le matricule.");
                        }

                        String newMatricule = request.getMatricule().trim();

                        if (utilisateurRepository.existsByMatricule(newMatricule)) {

                                throw new IllegalArgumentException(
                                                "Le matricule " + newMatricule + " est déjà utilisé.");
                        }

                        targetUser.setMatricule(newMatricule);
                }

                // Service
                if (request.getService_id() != null && !request.getService_id().equals(targetUser.getService_id())) {

                        if (!isAdmin) {

                                throw new AccessDeniedException(
                                                "Seul un administrateur est autorisé à changer l'affectation du service.");
                        }

                        targetUser.setService_id(request.getService_id());
                }

                // Manager
                if (request.getManager_id() != null && !request.getManager_id().equals(targetUser.getManager_id())) {

                        if (!isAdmin) {

                                throw new AccessDeniedException(
                                                "Seul un administrateur est autorisé à modifier le manager référent.");
                        }

                        targetUser.setManager_id(request.getManager_id());
                }

                return utilisateurRepository.save(targetUser);
        }

        // =========================================================
        // SUPPRESSION UTILISATEUR
        // =========================================================

        @Transactional
        public boolean deleteUtilisateur(Long id, Utilisateur currentUser) {

                if (currentUser == null || currentUser.getRole() != Role.ADMIN) {

                        throw new AccessDeniedException("Seul un administrateur peut supprimer un collaborateur.");
                }

                Utilisateur targetUser = utilisateurRepository.findById(id).orElse(null);

                if (targetUser == null) {
                        return false;
                }

                if (Objects.equals(currentUser.getId(), targetUser.getId())) {

                        throw new IllegalStateException(
                                        "Vous ne pouvez pas supprimer le compte ADMIN actuellement connecté.");
                }

                Long targetId = targetUser.getId();

                boolean hasAssociatedData = demandeCongeRepository.existsByUtilisateurId(targetId)
                                || demandeCongeRepository.existsByValideePar(targetId)
                                || soldeCongeRepository.existsByUtilisateurId(targetId)
                                || historiqueRepository.existsByUtilisateurId(targetId)
                                || historiqueRepository.existsByActorId(targetId)
                                || notificationRepository.existsByUtilisateurId(targetId)
                                || !utilisateurRepository.findByManagerId(targetId).isEmpty()
                                || (targetId <= Integer.MAX_VALUE
                                                && serviceRepository.existsByResponsableId(targetId.intValue()));

                if (hasAssociatedData) {

                        throw new IllegalStateException("Ce collaborateur possède des données métier associées. "
                                        + "Pour préserver l'historique, désactivez son compte "
                                        + "au lieu de le supprimer.");
                }

                String email = targetUser.getEmail();

                String matricule = targetUser.getMatricule();

                utilisateurRepository.delete(targetUser);

                Runnable sendDeletionEmail = () -> {

                        try {

                                emailService.sendAccountDeletedEmail(email, matricule);

                                log.info("E-mail de suppression envoyé pour l'utilisateur #{} [{}]", targetId, email);

                        } catch (Exception e) {

                                /*
                                 * La suppression est déjà validée. Une erreur d'envoi d'e-mail ne doit donc pas
                                 * annuler ou remettre en cause la suppression du compte.
                                 */

                                log.error("Échec de l'envoi de l'e-mail après suppression de l'utilisateur #{} [{}]. Type: {}. Message: {}",
                                                targetId, email, e.getClass().getSimpleName(), e.getMessage());
                        }
                };

                if (TransactionSynchronizationManager.isSynchronizationActive()) {

                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                                @Override
                                public void afterCommit() {

                                        sendDeletionEmail.run();
                                }
                        });

                } else {

                        sendDeletionEmail.run();
                }

                return true;
        }

        // =========================================================
        // CHANGEMENT DE MOT DE PASSE SECURISE
        // =========================================================

        @Transactional
        public void changePassword(Long userId, ChangePasswordRequest request, Utilisateur currentUser) {

                if (userId == null || request == null) {

                        throw new IllegalArgumentException("Données de changement de mot de passe invalides.");
                }

                Utilisateur targetUser = utilisateurRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé."));

                boolean isSelf = currentUser != null && Objects.equals(currentUser.getId(), targetUser.getId());

                boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;

                if (!isSelf && !isAdmin) {

                        throw new AccessDeniedException(
                                        "Vous ne pouvez pas modifier le mot de passe de cet utilisateur.");
                }

                // Si l'utilisateur change son propre mot de passe,
                // l'ancien est strictement exigé.
                if (isSelf) {

                        if (request.getAncienMotDePasse() == null || request.getAncienMotDePasse().isEmpty()) {

                                throw new IllegalArgumentException("L'ancien mot de passe est obligatoire.");
                        }

                        if (!passwordEncoder.matches(request.getAncienMotDePasse(), targetUser.getPassword())) {

                                throw new IllegalArgumentException("L'ancien mot de passe est incorrect.");
                        }
                }

                if (request.getNouveauMotDePasse() == null || request.getNouveauMotDePasse().length() < 6) {

                        throw new IllegalArgumentException(
                                        "Le nouveau mot de passe doit comporter au moins 6 caractères.");
                }

                targetUser.setPassword(passwordEncoder.encode(request.getNouveauMotDePasse()));

                utilisateurRepository.save(targetUser);
        }

        // =========================================================
        // CONSULTATION PAR ID
        // =========================================================

        public Utilisateur getUtilisateurById(Long id, Utilisateur currentUser) {

                if (id == null) {
                        return null;
                }

                Utilisateur target = utilisateurRepository.findById(id).orElse(null);

                if (target == null) {
                        return null;
                }

                if (currentUser == null) {

                        throw new AccessDeniedException("Authentification requise.");
                }

                if (currentUser.getRole() == Role.ADMIN) {

                        return target;
                }

                // Un employé ne peut voir que son profil
                if (currentUser.getRole() == Role.EMPLOYE) {

                        if (!Objects.equals(currentUser.getId(), target.getId())) {

                                throw new AccessDeniedException(
                                                "Accès non autorisé : vous ne pouvez consulter que votre propre profil.");
                        }

                        return target;
                }

                // Un manager peut voir son propre profil
                // ou les membres de son équipe
                if (currentUser.getRole() == Role.MANAGER) {

                        boolean isSelf = Objects.equals(currentUser.getId(), target.getId());

                        boolean isSubordinate = Objects.equals(currentUser.getId(), target.getManager_id());

                        if (!isSelf && !isSubordinate) {

                                throw new AccessDeniedException(
                                                "Accès non autorisé : cet employé n'appartient pas à votre équipe.");
                        }

                        return target;
                }

                throw new AccessDeniedException("Accès non autorisé.");
        }

        // =========================================================
        // LISTE DES UTILISATEURS
        // =========================================================

        public List<Utilisateur> getAllUtilisateurs(Utilisateur currentUser) {

                if (currentUser == null) {

                        throw new AccessDeniedException("Authentification requise.");
                }

                if (currentUser.getRole() == Role.ADMIN) {

                        return utilisateurRepository.findAll();
                }

                if (currentUser.getRole() == Role.MANAGER) {

                        List<Utilisateur> subordinates = new ArrayList<>(
                                        utilisateurRepository.findByManagerId(currentUser.getId()));

                        if (subordinates.stream().noneMatch(u -> Objects.equals(u.getId(), currentUser.getId()))) {

                                subordinates.add(currentUser);
                        }

                        return subordinates;
                }

                throw new AccessDeniedException(
                                "Accès refusé : la liste complète des collaborateurs est réservée à l'administration.");
        }

}