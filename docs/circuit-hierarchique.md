# Circuit hiérarchique des congés

## Déploiement de la base

Avant de démarrer le backend sur une base existante :

1. Faire une sauvegarde PostgreSQL.
2. Exécuter une seule fois `Backend/src/main/resources/db/manual/2026-11-12-circuit-hierarchique.sql`.
3. Démarrer le backend avec la configuration habituelle (`ddl-auto=validate`).

La migration autorise les comptes sans e-mail, crée `departement` et `circuit_demande_conge`, puis ajoute `departement_id` à `service`. Elle ne devine aucune affectation existante : les services restent sans département jusqu'à leur rattachement explicite.

## Configuration initiale

1. L'ADMIN crée les comptes `ASSISTANT_DIRECTION`, `ADMINISTRATION_PERSONNEL`, `CHEF_DEPARTEMENT` et `DIRECTEUR` dans Gestion des collaborateurs.
2. L'ADMIN crée chaque département en affectant un chef et un directeur distincts.
3. L'ADMIN rattache les services existants à leurs départements.
4. Les collaborateurs doivent avoir un service affecté et ce service doit être rattaché à un département avant la création d'un dossier.

## Circuit

- `RELIQUAT_EN_ATTENTE` : dossier verbal créé par l'assistant, en attente de l'administration du personnel.
- `RELIQUAT_RETOURNE` : reliquat contrôlé et retourné à l'assistant.
- `CHEF_EN_ATTENTE` : règles de congé vérifiées, dossier transmis au chef du département.
- `DIRECTEUR_EN_ATTENTE` : dossier transmis au directeur.
- `VALIDEE` ou `REFUSEE` : décision enregistrée. Un refus exige un motif.

À la décision, le backend réutilise le traitement historique pour débiter les soldes en cas d'accord et conserver les mouvements d'historique. Le PDF est généré à partir des informations de décision et devient disponible dans l'espace de l'assistant, avec notification.

## Permissions principales

- ADMIN : gestion des collaborateurs et des départements, consultation des demandes, historiques et soldes, traitement des demandes de réinitialisation par matricule. Aucun traitement de congé ni ajustement/initialisation manuelle des soldes.
- MANAGER : consultation des demandes de son équipe uniquement.
- ASSISTANT_DIRECTION : création des dossiers verbaux, transmission au chef après contrôle et réception du PDF.
- ADMINISTRATION_PERSONNEL : consultation du solde d'un collaborateur et retour du reliquat.
- CHEF_DEPARTEMENT : examen et transmission au directeur de son département.
- DIRECTEUR : décision finale pour son département, avec motif obligatoire en cas de refus.
- EMPLOYE : consultation de ses demandes, sans formulaire de dépôt.

## Hors périmètre temporaire

L'envoi des identifiants par SMS pour la récupération par matricule est volontairement différé. Aucun SMS n'est simulé.

## Référentiel de consolidation

Le référentiel métier détaillé, les transitions autorisées, les responsabilités, les règles de sécurité, les choix conservateurs et les tests d'acceptation sont centralisés dans [workflow-spat-reference.md](workflow-spat-reference.md).

Attention : le code actuel utilise encore les fonctions professionnelles comme valeurs de l'enum `Role`. La séparation durable entre rôle technique, fonction professionnelle et permission métier est une évolution de modèle de données à réaliser avec une migration additive et des tests de compatibilité ; elle n'est pas réputée terminée par la seule documentation.
