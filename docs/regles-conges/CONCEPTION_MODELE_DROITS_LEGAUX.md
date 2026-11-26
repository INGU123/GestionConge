# Conception cible — droits au congé annuel (loi malgache n° 2024-014)

> État : conception préparatoire. Ce document ne constitue ni une implémentation, ni une validation juridique/RH. Ne pas fusionner la PR #6 telle quelle.

## 1. État technique confirmé

Le modèle actuel repose sur :
- `solde_conge.solde_aquis`, `solde_pris` et `solde_restant` en `Integer` ;
- une période annuelle entière (`periode`) ;
- `type_conge.nombreJoursParAn` en `Integer` ;
- des calculs historiques par jours ouvrés et par année civile ;
- un débit/restauration des soldes dans le service de demandes.

Ce modèle ne peut pas représenter proprement une acquisition de 2,5 jours par mois ni conserver l'origine exacte des droits consommés.

## 2. Règles de référence à couvrir

Sous réserve de validation par les RH et de lecture de la version officielle du texte :
- Article 125 : acquisition de 2,5 jours calendaires par mois de service effectif, sauf disposition plus favorable.
- Articles 126 et 127 : distinguer les périodes assimilées au service effectif et les absences qui ne sont pas comptées.
- Article 128 : planification des congés en début d'année après consultation.
- Article 129 : prescription triennale, sous réserve des exceptions prévues par le texte.
- Article 130 : ouverture du droit après 12 mois de service effectif ; première fraction de 15 jours prise de manière ininterrompue dans les 3 mois suivant l'ouverture ; deuxième fraction prise ou cumulée sur 3 ans.

Le décompte du congé annuel doit être séparé du calcul des autres catégories de congé. Les samedis et dimanches ne doivent pas être supprimés du décompte calendaire du congé annuel ; les jours fériés suivent la règle légale applicable.

## 3. Modèle cible proposé

### A. Historique des périodes de service

Table proposée : `periode_service_effectif`

- `id`
- `utilisateur_id`
- `date_debut`, `date_fin`
- `nature` : période travaillée, absence assimilée, absence non assimilée
- `reference_legale` ou code de motif
- `justificatif_reference`
- `validee_par`, `validee_le`
- `commentaire`

Contraintes : dates cohérentes, périodes non contradictoires, traçabilité des corrections et validation RH. La date d'embauche reste le point de départ, mais ne suffit pas à elle seule à calculer tout le service effectif.

### B. Journal d'acquisition mensuelle

Table proposée : `acquisition_conge_annuel`

- `id`, `utilisateur_id`
- `mois_service` (mois de référence)
- `jours_acquis` en `NUMERIC(6,2)`
- `statut_validation`, `validee_par`, `validee_le`
- `origine_calcul`, `commentaire`

Une contrainte unique sur salarié + mois évite les doubles acquisitions. Le journal conserve chaque attribution de 2,50 jours au lieu de remplacer les acquisitions par un quota annuel entier.

### C. Lots de droits et consommations

Table proposée : `affectation_droit_conge`

- `id`, `demande_conge_id`, `acquisition_conge_annuel_id`
- `jours_imputes` en `NUMERIC(6,2)`
- `date_imputation`, `statut`

Chaque demande approuvée est imputée sur un ou plusieurs lots, selon l'ordre de consommation validé par les RH. L'annulation restaure exactement les lots d'origine. La suppression logique ou la correction d'une demande ne doit pas effacer l'historique.

### D. Ouverture du droit et fractions

Le système doit pouvoir conserver :
- la date réelle d'ouverture après vérification des 12 mois de service effectif ;
- la fenêtre de trois mois de la première fraction ;
- la prise ininterrompue des 15 jours ;
- les dates de début et de fin des fractions ;
- les échéances de cumul et de prescription sur trois ans.

Ces événements doivent être historisés, pas déduits uniquement du solde courant.

## 4. Intégration au code

Les deux parcours de demande doivent appeler le même service métier :
1. demande classique ;
2. circuit hiérarchique SPAT.

Le service commun doit :
- déterminer le nombre de jours calendaires imputables au congé annuel ;
- vérifier l'ouverture du droit à la date demandée ;
- contrôler les droits disponibles et les échéances ;
- réserver/imputer les lots de droits de manière transactionnelle ;
- restituer les mêmes lots en cas d'annulation ;
- empêcher les doubles débits en cas de concurrence ;
- conserver les calculs en jours ouvrés pour les autres types de congé.

Les soldes historiques entiers ne doivent pas être convertis automatiquement en lots de 2,50 jours. Une reprise doit être calculée et validée par les RH à partir des données historiques disponibles.

## 5. Décisions métier indispensables avant codage final

Ces points ne doivent pas être inventés par le développeur :

1. Comment la SPAT enregistre-t-elle les périodes assimilées et non assimilées au service effectif ? Quels justificatifs et qui les valide ?
2. Comment traiter un mois de service partiel ou interrompu par une absence non assimilée : acquisition entière, prorata, ou aucune acquisition pour le mois ? La réponse doit être confirmée par les RH ou un avis juridique.
3. À quelle date exacte le droit est-il réputé ouvert lorsque les périodes de service effectif ont été corrigées rétroactivement ?
4. Comment la SPAT planifie-t-elle et prouve-t-elle la première fraction de 15 jours consécutifs dans les trois mois ?
5. Quelle règle interne plus favorable existe éventuellement, et comment gérer les reliquats déjà enregistrés ?

## 6. Plan de migration sans risque

1. Créer les nouvelles tables en ajout additif, sans supprimer ni renommer les colonnes existantes.
2. Exécuter la migration sur une base de test isolée.
3. Charger les périodes de service validées et les acquisitions historiques vérifiées.
4. Comparer les nouveaux droits avec les soldes actuels, salarié par salarié.
5. Faire valider les écarts par les RH.
6. Activer le nouveau calcul uniquement après rapprochement et tests automatisés.
7. Conserver une procédure de retour arrière et l'ancien historique en lecture seule pendant la transition.

## 7. Tests d'acceptation requis

- acquisition mensuelle de 2,50 jours sans doublon ;
- service effectif interrompu par une absence non assimilée ;
- période assimilée qui ne réduit pas le service effectif ;
- ouverture du droit à 12 mois effectifs ;
- première fraction de 15 jours consécutifs dans la fenêtre légale ;
- seconde fraction et cumul dans la période de trois ans ;
- congé annuel traversant deux années ;
- consommation de plusieurs lots et restauration exacte après annulation ;
- deux demandes concurrentes sur le même reliquat ;
- coexistence avec les types de congé calculés en jours ouvrés ;
- vérification du parcours classique et du circuit hiérarchique.

## 8. Décision actuelle

La PR #6 ne doit pas être fusionnée telle quelle. Elle améliore certains points mais s'appuie encore sur la date d'embauche comme approximation du service effectif et sur des soldes entiers par année civile. La présente conception prépare une refonte traçable, mais l'implémentation finale dépend des décisions métier ci-dessus.
