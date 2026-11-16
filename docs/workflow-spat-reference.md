# Référentiel fonctionnel — Workflow SPAT de gestion des congés

> Statut : proposition de référence technique à valider avec la procédure administrative interne SPAT.
>
> Ce document fixe les choix de conception prudents retenus pour rendre le logiciel cohérent. Il ne prétend pas remplacer une note de service, une convention collective ou une instruction officielle de la SPAT. Toute règle locale différente devra être ajustée ici avant modification du code.

## 1. Invariant métier non négociable

L'ordre du circuit est strictement :

1. Assistant
2. Administration du personnel
3. Assistant
4. Chef du département
5. Directeur

Aucun profil technique ne peut sauter, inverser ou remplacer une étape. Le passage d'une étape à la suivante est une transition métier explicite, contrôlée côté serveur et enregistrée dans l'historique.

La décision finale appartient à l'étape Directeur dans le circuit décrit. Les rôles techniques ADMIN, MANAGER et EMPLOYE ne confèrent pas, à eux seuls, le droit de valider ou refuser un congé.

## 2. Séparer les trois notions

- **Rôle technique** : ADMIN, MANAGER, EMPLOYE. Il définit l'accès général à l'application et les opérations d'administration technique.
- **Fonction professionnelle** : ASSISTANT, ADMINISTRATION_PERSONNEL, CHEF, DIRECTEUR. Elle détermine la place d'un agent dans le circuit.
- **Permission métier** : consulter, préparer, contrôler le reliquat, transmettre, examiner, décider, consulter les historiques.

Un utilisateur possède un rôle technique et une fonction professionnelle distincts. Un rôle technique ne doit jamais être converti implicitement en permission de décision. La fonction professionnelle doit être historisée au moment des actions sensibles.

Les permissions sont évaluées par le backend à partir de l'utilisateur authentifié, de sa fonction active, de son périmètre organisationnel, de l'état du dossier et de l'étape attendue. Le frontend ne fait que présenter les actions autorisées.

## 3. Répartition des responsabilités

| Fonction | Responsabilité dans le circuit | Périmètre |
|---|---|---|
| Assistant | Constituer le dossier, recevoir le retour de reliquat, contrôler la cohérence du dossier puis le transmettre au Chef | Dossiers qu'il est habilité à gérer |
| Administration du personnel | Vérifier les droits et le reliquat disponible, enregistrer le reliquat contrôlé et retourner le dossier à l'Assistant | Tous les dossiers relevant de son périmètre administratif |
| Chef | Examiner le dossier et le transmettre au Directeur | Département affecté |
| Directeur | Prendre la décision finale et motiver tout refus | Département affecté |

ADMIN conserve la gestion des comptes, fonctions, départements, services et paramètres administratifs ainsi que les consultations autorisées. ADMIN ne peut pas agir à la place d'une étape du circuit, ni modifier directement un dossier pour le faire avancer.

MANAGER conserve un accès de consultation strictement limité aux données autorisées par son périmètre. Il ne valide et ne refuse pas par le seul fait de son rôle.

EMPLOYE consulte ses informations, ses soldes et l'état de ses dossiers selon les droits attribués. Le dépôt d'une demande par formulaire employé reste désactivé dans le parcours actuel : le dossier est constitué par l'Assistant conformément au processus retenu.

## 4. États métier canoniques

Les libellés techniques existants peuvent être conservés temporairement pour compatibilité, mais ils doivent correspondre sans ambiguïté aux états ci-dessous.

| État canonique | Étape / signification |
|---|---|
| BROUILLON_ASSISTANT | Dossier préparé mais pas encore envoyé |
| ATTENTE_ADMINISTRATION | En attente du contrôle du reliquat |
| RETOUR_ASSISTANT | Reliquat contrôlé, dossier retourné à l'Assistant |
| ATTENTE_CHEF | Dossier transmis au Chef |
| ATTENTE_DIRECTEUR | Dossier transmis au Directeur |
| APPROUVEE | Décision finale favorable |
| REFUSEE | Décision finale défavorable avec motif |
| ANNULEE | Annulation autorisée et tracée avant clôture selon la règle ci-dessous |

Un état technique inconnu ou incohérent doit bloquer la transition et générer une erreur exploitable ; il ne doit jamais être interprété comme une approbation.

## 5. Transitions et contrôles

| État source | Action | Acteur | État cible |
|---|---|---|---|
| BROUILLON_ASSISTANT | Envoyer au contrôle du reliquat | Assistant | ATTENTE_ADMINISTRATION |
| ATTENTE_ADMINISTRATION | Enregistrer le reliquat vérifié et retourner le dossier | Administration du personnel | RETOUR_ASSISTANT |
| RETOUR_ASSISTANT | Contrôler le dossier et transmettre | Assistant | ATTENTE_CHEF |
| ATTENTE_CHEF | Examiner et transmettre | Chef du département concerné | ATTENTE_DIRECTEUR |
| ATTENTE_DIRECTEUR | Approuver | Directeur du département concerné | APPROUVEE |
| ATTENTE_DIRECTEUR | Refuser avec motif | Directeur du département concerné | REFUSEE |

Toute autre transition est interdite par défaut. En particulier, aucune API historique ne doit permettre à ADMIN ou MANAGER de traiter directement une demande du circuit en contournant les étapes ci-dessus.

### 5.1 Retours et corrections

Le retour normal prévu par le circuit est Administration du personnel → Assistant. Le commentaire de contrôle est obligatoire lorsqu'une correction est demandée.

Après réception du retour, l'Assistant peut corriger les champs autorisés et retransmettre au Chef seulement lorsque le contrôle de reliquat et les vérifications de cohérence sont satisfaits.

Après transmission au Chef, une correction ne doit pas faire disparaître les étapes déjà réalisées. Tant qu'aucune procédure de retour Chef/Directeur n'est formellement définie, ces étapes ne disposent pas d'une transition de retour dans l'application. Le dossier doit être bloqué et orienté vers l'administrateur habilité pour traitement documenté, sans modification silencieuse.

Un refus final est définitif dans le dossier courant. Une nouvelle demande doit être créée pour une nouvelle instruction. Une réouverture administrative exceptionnelle, si elle devient nécessaire, devra être une opération distincte, autorisée, motivée et auditée.

### 5.2 Annulation

Choix conservateur : une demande en cours ne peut pas être supprimée physiquement. Une annulation avant décision finale doit être demandée et enregistrée comme événement ; elle ne doit être effective qu'après autorisation de l'Assistant responsable du dossier et contrôle qu'aucune décision finale n'a été rendue. Après APPROUVEE ou REFUSEE, pas d'annulation ordinaire : une correction administrative distincte est nécessaire.

L'annulation d'une demande approuvée ne doit jamais effacer la décision ni modifier le solde sans mouvement compensatoire traçable. Les autorisations et modalités de cette annulation approuvée sont à confirmer dans la procédure SPAT avant activation.

## 6. Règles de sécurité et d'intégrité

- Un utilisateur ne peut jamais instruire sa propre demande.
- Le backend contrôle chaque transition ; le masquage d'un bouton frontend n'est pas une mesure de sécurité.
- Le dossier est verrouillé pendant une transition concurrente ; l'état est revérifié dans la transaction.
- Une action répétée ne doit pas créer deux mouvements de solde, deux décisions ou deux notifications métier.
- Les contrôles de département sont obligatoires pour Chef et Directeur.
- Le Chef et le Directeur affectés au dossier sont figés au moment de son entrée dans l'étape départementale ; un changement ultérieur d'affectation ne réécrit pas l'historique du dossier.
- Une fonction désactivée, un compte archivé ou un département inactif ne peut pas agir.
- Un compte sans fonction professionnelle ne peut pas exécuter d'action de circuit.
- Les opérations critiques sont transactionnelles : décision, création de la demande historique, mouvements de solde et audit doivent rester cohérents.
- Les API de l'ancien workflow ne doivent pas rester un chemin parallèle de validation.

## 7. Soldes, reliquats et mouvements

Le reliquat communiqué par l'Administration du personnel est une donnée contrôlée du dossier ; il ne doit pas être confondu avec le solde calculé automatiquement.

Avant transmission au Chef, le backend recalcule les jours ouvrés et vérifie que le reliquat saisi couvre le nombre de jours demandé selon les règles de congé applicables. Toute différence de calendrier, de jours fériés ou de règles légales bloque la transmission et demande une nouvelle vérification.

Choix comptable : le solde n'est débité qu'après la décision finale APPROUVEE. Le débit est exécuté une seule fois dans la même transaction que la décision. Un refus ne débite rien. Toute restitution ultérieure est un mouvement compensatoire lié au mouvement initial ; aucun solde historique n'est écrasé.

Chaque mouvement conserve au minimum : collaborateur, type de congé, quantité, sens, date, référence du dossier, acteur, motif, solde avant et après si disponible. Les valeurs négatives et les doubles consommations sont interdites.

Le calcul du reliquat disponible doit avoir une seule source métier faisant autorité. Les écrans et exports ne doivent pas recalculer chacun leur propre formule.

## 8. Interfaces par fonction

Les espaces de travail sont spécifiques aux responsabilités, sans dupliquer la logique métier :

- **Assistant** : dossiers à préparer, retours reçus, dossiers transmis, décisions finales et téléchargement du document de décision.
- **Administration du personnel** : file de contrôle, détail des droits, reliquat calculé, reliquat enregistré, commentaire et retour à l'Assistant.
- **Chef** : file des dossiers de son département, détail complet utile à l'examen, transmission au Directeur.
- **Directeur** : file des dossiers de son département, synthèse de contrôle et actions de décision finale.
- **ADMIN** : collaborateurs, fonctions, départements, services, paramètres et consultations administratives.
- **MANAGER** : consultation autorisée uniquement.
- **EMPLOYE** : espace personnel, soldes, suivi et historique de ses dossiers.

Fonctionnalités communes conservées pour tous : Accueil, Notifications, Historique, mode jour/nuit et Déconnexion. Les contenus et menus doivent être adaptés à la fonction, mais l'autorisation reste toujours appliquée au backend.

**Aide / Guide** : l'entrée de navigation est masquée pour tous les profils. Les pages, textes, captures et fichiers d'aide sont conservés dans le dépôt ; aucune suppression de contenu n'est demandée.

## 9. Historique et audit

Chaque événement important est append-only dans le journal métier : création, envoi, contrôle, retour, correction, transmission, décision, annulation, mouvement de solde et opération administrative exceptionnelle.

Chaque événement doit contenir : identifiant du dossier, type d'événement, état avant/après, acteur, rôle technique, fonction professionnelle au moment de l'action, horodatage serveur, commentaire/motif, et identifiant de corrélation de la requête si disponible.

Les journaux ne doivent pas contenir de mot de passe, jeton JWT, token de réinitialisation ou donnée secrète. Un utilisateur ordinaire ne peut ni modifier ni supprimer l'historique.

## 10. Notifications et délais

Une notification est émise après validation transactionnelle d'un événement métier, jamais avant son commit. Les événements répétés ne doivent pas produire de doublons.

Les notifications prioritaires sont : dossier à traiter, reliquat retourné, dossier transmis, décision finale, annulation et anomalie bloquante. Les rappels de retard sont des alertes, pas des transitions automatiques.

Les délais de traitement et seuils de relance doivent être configurables. Aucun délai légal ou délai SPAT n'est présumé dans ce document.

## 11. Données organisationnelles et affectations

- Un département possède un Chef et un Directeur actifs, distincts.
- Un service est rattaché explicitement à un département.
- Un collaborateur doit avoir un rattachement organisationnel valide pour être traité dans le circuit.
- Un changement de fonction, de service ou de département est daté et audité.
- Les dossiers déjà ouverts conservent les acteurs et le département enregistrés lors de leur affectation au circuit.
- La création d'un premier Chef ou Directeur ne doit pas exiger un responsable hiérarchique inexistant. Le responsable hiérarchique ne doit pas être rendu obligatoire pour les fonctions qui n'en ont pas besoin.

Les comptes de démonstration et personnages fictifs sont hors périmètre de cette phase.

## 12. Tests d'acceptation obligatoires

1. Le circuit complet passe exclusivement par Assistant → Administration du personnel → Assistant → Chef → Directeur.
2. Toute tentative de sauter une étape est rejetée côté backend.
3. ADMIN et MANAGER ne peuvent pas approuver/refuser par leurs anciens endpoints.
4. Aucun utilisateur ne peut agir sur sa propre demande.
5. Administration du personnel ne peut traiter que l'état ATTENTE_ADMINISTRATION.
6. L'Assistant ne peut retransmettre qu'après retour du reliquat et contrôles valides.
7. Chef et Directeur ne voient et ne traitent que leur département.
8. Un refus sans motif est rejeté.
9. Une demande approuvée ne débite le solde qu'une seule fois.
10. Un refus ne débite aucun solde.
11. Une transition concurrente ou rejouée ne double aucun effet.
12. Une correction conserve les valeurs et événements antérieurs.
13. Une fonction désactivée ou un compte archivé ne peut plus traiter de dossier.
14. Les notifications ne sont émises qu'après commit et sans doublon.
15. L'entrée Aide / Guide est absente du menu de tous les profils ; les fichiers d'aide restent présents.
16. Les fonctionnalités communes restent accessibles à tous les profils autorisés.
17. Les accès directs aux URL et endpoints sont testés, pas seulement les menus.
18. Les anciennes demandes et mouvements restent consultables après migration.

## 13. Ordre d'implémentation

1. Geler le référentiel métier et inventorier les chemins historiques de validation.
2. Ajouter la fonction professionnelle séparément du rôle technique, avec migration additive et compatibilité des comptes existants.
3. Centraliser les permissions et transitions dans un service métier unique.
4. Rendre les endpoints historiques de traitement incompatibles avec le circuit obligatoire, sans casser les consultations et historiques.
5. Ajouter le journal d'audit append-only et fiabiliser les transactions de solde.
6. Finaliser les vues propres à chaque fonction et conserver les cinq fonctionnalités communes.
7. Renforcer notifications, délais, calendrier, exports et délégations uniquement après validation des règles correspondantes.
8. Exécuter les tests d'acceptation et une répétition de migration sur une copie de base avant tout déploiement.

## 14. Choix prudents à réviser si la procédure SPAT l'exige

Les choix suivants sont des valeurs par défaut de conception, non des affirmations sur un règlement interne :
- décision finale exclusivement au Directeur ;
- refus final sans réouverture ordinaire ;
- retour de correction standard Administration du personnel → Assistant ;
- annulation en cours avec autorisation de l'Assistant responsable ;
- débit du solde uniquement après approbation finale ;
- absence de retour automatique depuis Chef ou Directeur ;
- absence de délais de traitement présumés.

Ils sont volontairement centralisés ici pour pouvoir être modifiés sans disperser des règles contradictoires dans le code.
