# Automatisations de GestionConge

Fuseau horaire : `Africa/Antananarivo`.

## Automatisations actives

| Tâche | Horaire | Comportement |
|---|---|---|
| Rappel des demandes en attente | Tous les jours à 08:00 | Regroupe les demandes dont la date de création remonte à au moins 3 jours et avertit le manager responsable. Si aucun manager actif n'est affecté, les administrateurs actifs sont avertis. Un rappel par destinataire et par jour. |
| Bilan RH mensuel | Le 1er de chaque mois à 08:30 | Envoie aux administrateurs actifs un récapitulatif des demandes créées le mois précédent et de leur statut actuel. |
| Notifications e-mail | Après validation de la transaction métier | Complète les notifications internes pour les soumissions, validations, refus, annulations, rappels et bilans. Une panne SMTP est journalisée et ne bloque pas l'opération métier. |

## Attribution annuelle des soldes : volontairement désactivée

La tâche de création des soldes annuels manquants existe, mais elle est désactivée par défaut avec :

`app.automation.annual-balances.enabled=false`

Ne l'activer qu'après migration du moteur de droits. Le champ historique `nombreJoursParAn` et l'initialisation annuelle complète ne représentent pas à eux seuls les droits légaux par période de référence. La documentation `docs/REGLES_CONGES_MADAGASCAR.md` signale notamment l'acquisition en jours calendaires, les périodes de service effectif, la première jouissance et les reports. Réactiver la tâche avant cette migration pourrait créditer des droits incorrects.

La tâche utilise uniquement les valeurs configurées par l'administration ; elle n'invente aucun taux légal ni prorata.

## Paramètres

- Délai de rappel : 3 jours calendaires, actuellement défini dans `AutomatisationCongeService`.
- Fuseau horaire : `Africa/Antananarivo`.
- Les e-mails nécessitent la configuration SMTP déjà utilisée par `EmailService`.

## Exploitation

- Garder une seule instance active du backend tant qu'un verrou distribué ou une contrainte d'unicité dédiée aux tâches planifiées n'est pas en place.
- Vérifier les journaux `AutomatisationCongeService` et `EmailService`.
- Tester les tâches sur une copie de PostgreSQL avant toute mise en production.
- Les bilans mensuels sont des récapitulatifs e-mail, pas des fichiers PDF joints.
