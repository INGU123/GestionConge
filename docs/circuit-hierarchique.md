# Circuit hiérarchique des congés

## Déploiement de la base

Avant de démarrer le backend sur une base existante :

1. Faire une sauvegarde PostgreSQL.
2. Exécuter, si cela n'a pas déjà été fait, `Backend/src/main/resources/db/manual/2026-11-12-circuit-hierarchique.sql`.
3. Exécuter une seule fois `Backend/src/main/resources/db/manual/2026-11-19-referentiel-employes-circuit.sql`.
4. Démarrer le backend avec la configuration habituelle (`ddl-auto=validate`).

La migration du 12 novembre crée les départements et le circuit hiérarchique. La migration du 19 novembre ajoute le référentiel des employés, les liens employé aux dossiers, soldes et mouvements, ainsi que l'historique des confirmations de reliquat. Elle conserve les références historiques `utilisateur_id`.

## Référentiel des employés

- Les employés opérationnels sont stockés dans la table `employe`, séparément des comptes `utilisateur`.
- Aucun écran CRUD ou annuaire des employés n'est proposé.
- L'assistant recherche uniquement un matricule numérique exact ; l'application affiche les informations minimales et lie le dossier à `employe_id`.
- Les nouveaux dossiers ne nécessitent pas de compte de connexion pour l'employé.
- Les anciens dossiers et historiques liés à `utilisateur_id` restent consultables.
- Les 22 employés fictifs déjà présents dans la base locale ne doivent pas être réinsérés.

## Configuration initiale

1. L'ADMIN crée les comptes de connexion nécessaires : `ASSISTANT_DIRECTION`, `ADMINISTRATION_PERSONNEL`, `CHEF_DEPARTEMENT` et `DIRECTEUR`.
2. L'ADMIN crée chaque département en affectant un chef et un directeur.
3. L'ADMIN rattache les services existants à leurs départements.
4. Lors de la création du dossier, l'assistant choisit le service. Le département associé est enregistré comme instantané sur le dossier ; le routage Chef/Directeur se fait à partir de ce département enregistré.

## Circuit

- `RELIQUAT_EN_ATTENTE` : dossier créé par l'assistant, en attente de contrôle RH.
- `RELIQUAT_RETOURNE` : reliquat confirmé et retourné à l'assistant.
- `CHEF_EN_ATTENTE` : dossier soumis au chef du département enregistré.
- `DIRECTEUR_EN_ATTENTE` : dossier transmis au directeur enregistré.
- `VALIDEE` ou `REFUSEE` : décision finale enregistrée. Un refus exige un motif.

Pour un employé du référentiel, l'administration confirme le nombre de jours, la période de référence et la référence justificative via l'API `/reliquats/dossier/{id}/confirmer`. Chaque confirmation est conservée dans `confirmation_reliquat` ; l'API ne permet ni modification ni suppression de ces preuves. L'ancien endpoint de retour simple reste réservé aux dossiers historiques liés à un compte utilisateur.

## Soldes et mouvements des employés

- Les soldes employés sont rattachés par `employe_id`, avec une ligne par employé, type de congé et exercice.
- L'administration du personnel enregistre explicitement les jours acquis à partir des données RH validées via `POST /solde/employe/{id}/initialiser` avec `typeCongeId`, `periode` et `joursAcquis`.
- Aucun quota ni règle d'acquisition SPAT n'est calculé ou inventé automatiquement pour un employé.
- À la décision finale du directeur, une demande validée est débitée sur chaque exercice concerné. Le débit est refusé si un solde correspondant n'existe pas ou est insuffisant.
- Chaque débit ou ajustement est inscrit dans `historique_mouvement` avec `employe_id`. Les anciennes lignes utilisateur restent intactes.

## Permissions principales

- ADMIN : gestion des comptes et départements, consultation globale. Pas de saisie manuelle des soldes employés.
- MANAGER : consultation des demandes de son équipe selon le circuit historique.
- ASSISTANT_DIRECTION : recherche par matricule exact, création des dossiers et soumission au chef après confirmation RH.
- ADMINISTRATION_PERSONNEL : consultation des soldes, saisie explicite des soldes RH et confirmation du reliquat.
- CHEF_DEPARTEMENT : examen et transmission au directeur du département enregistré.
- DIRECTEUR : décision finale pour les dossiers de son département, avec motif obligatoire en cas de refus.
- EMPLOYE : rôle historique conservé ; les nouveaux dossiers du référentiel ne requièrent pas ce rôle.

## Hors périmètre temporaire

L'envoi des identifiants par SMS pour la récupération par matricule est différé. Aucun SMS n'est simulé.
