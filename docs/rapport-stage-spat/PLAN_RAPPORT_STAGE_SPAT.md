# Plan de rédaction — Rapport de stage SPAT

> Document de préparation. Les éléments entre crochets doivent être complétés à partir des documents SPAT, des observations réelles et des tests réalisés. Ne pas transformer une hypothèse en fait.

## Informations confirmées

- Organisme d'accueil : Société du Port à Gestion Autonome de Toamasina (SPAT).
- Période de stage : du 3 août au 3 octobre 2026.
- Sujet : conception et développement d'une application web de gestion des congés.
- Étudiant : Ass Van Fruvio VAZAHA.
- Formation : Licence en Économie Administratif et Social, Université de Toamasina.
- Orientation du projet : application de gestion des demandes et du circuit de traitement des congés.

## Répartition cible — environ 80 pages hors annexes

| Partie | Contenu | Pages cibles |
|---|---|---:|
| Pages liminaires | Couverture, attestations, dédicace, remerciements, résumés, sigles, sommaire | 8 |
| Introduction générale | Contexte, problématique, objectifs, démarche, annonce du plan | 5 |
| Partie I — Cadre du stage | Présentation SPAT, missions, service d'accueil, environnement de travail | 10 |
| Partie II — Étude préalable | Situation existante observée, difficultés confirmées, besoins, périmètre et contraintes | 11 |
| Partie III — Analyse et conception | Acteurs, exigences, processus, UML, architecture, modèle de données, sécurité | 17 |
| Partie IV — Réalisation | Environnement technique, backend, frontend, base de données, circuit hiérarchique, interfaces | 16 |
| Partie V — Vérification et résultats | Stratégie de tests, tests automatisés, tests manuels, résultats, limites | 8 |
| Conclusion générale | Bilan réel, limites, perspectives | 3 |
| Bibliographie et webographie | Textes légaux, documentation technique, sources consultées | 2 |
| **Total cible** |  | **80** |

Les annexes peuvent être ajoutées après les 80 pages : extraits de code utiles, scripts SQL, cas de test, diagrammes complets et captures autorisées.

## Contenu attendu par partie

### Introduction générale
- Présenter le contexte de la gestion des congés dans une organisation.
- Exposer uniquement le problème réellement observé pendant le stage.
- Distinguer l'objectif général des objectifs spécifiques.
- Délimiter ce que l'application couvre et ce qu'elle ne couvre pas.

### Partie I — Cadre du stage
1. Présentation générale de la SPAT à partir de sources institutionnelles.
2. Historique, missions et organisation : à confirmer avec des documents officiels.
3. Présentation du service d'accueil et de son rôle : à compléter avec les informations réelles.
4. Déroulement du stage, encadrement et activités effectivement réalisées.

### Partie II — Étude préalable
1. Description du processus de congé réellement pratiqué avant le projet.
2. Acteurs impliqués et circulation des informations, après validation par le personnel concerné.
3. Problèmes constatés et exemples anonymisés, sans inventer de cas.
4. Besoins fonctionnels et non fonctionnels.
5. Périmètre, contraintes, risques et critères de réussite.

### Partie III — Analyse et conception
1. Identification des acteurs et des rôles.
2. Cas d'utilisation et descriptions textuelles.
3. Diagrammes de séquence des scénarios réellement implémentés.
4. Diagramme de classes aligné sur le code final.
5. Modèle relationnel et dictionnaire des données alignés sur la base vérifiée.
6. Architecture logique et échanges frontend/backend.
7. Authentification, autorisations, protection des données et journalisation.

### Partie IV — Réalisation
1. Environnement matériel et logiciel réellement utilisé.
2. Technologies et versions vérifiées dans les fichiers du projet.
3. Organisation du backend Spring Boot.
4. Organisation du frontend Next.js/React.
5. Persistance PostgreSQL et scripts de migration.
6. Gestion des utilisateurs, rôles, demandes, soldes et notifications.
7. Circuit hiérarchique : Assistant de direction → Administration du personnel → Assistant → Chef de département → Directeur, sous réserve de validation du parcours final.
8. Présentation des interfaces avec captures réelles, anonymisées et lisibles.

### Partie V — Vérification et résultats
1. Environnement de test et jeux de données fictifs.
2. Tests unitaires : commande, date, résultat et nombre de tests réellement exécutés.
3. Tests d'intégration et CI : preuves issues des exécutions GitHub Actions.
4. Tests manuels du parcours complet : à remplir après exécution conjointe.
5. Résultats obtenus, anomalies corrigées et anomalies restantes.
6. Limites connues et écarts entre objectifs et réalisation.

### Conclusion générale
- Rappeler les objectifs.
- Présenter uniquement les réalisations démontrées.
- Faire le bilan des compétences acquises.
- Proposer des perspectives réalistes et priorisées.

## Éléments à collecter avant rédaction finale

- [ ] Organigramme SPAT officiel et présentation institutionnelle autorisée.
- [ ] Nom exact du service d'accueil et fonction de l'encadreur.
- [ ] Description validée du processus de congé avant le projet.
- [ ] Dates et activités réellement réalisées pendant le stage.
- [ ] Architecture et diagrammes extraits de la version finale du code.
- [ ] Schéma PostgreSQL réellement appliqué sur la base de test.
- [ ] Captures d'écran de chaque rôle, avec données fictives ou anonymisées.
- [ ] Résultats CI associés aux commits finaux.
- [ ] Résultats des tests manuels réalisés ensemble.
- [ ] Validation RH des règles de congés et de leur interprétation opérationnelle.
- [ ] Autorisation de citer les noms, documents ou captures internes SPAT.

## Règles de fiabilité rédactionnelle

- Ne pas inventer de statistiques, de procédures internes, de résultats, de réunions ou de validations.
- Ne pas présenter un test prévu comme un test exécuté.
- Ne pas présenter une migration préparée comme une migration appliquée.
- Ne pas affirmer que l'application est déployée ou utilisée en production sans preuve.
- Anonymiser les salariés, matricules, adresses, e-mails et données de congé.
- Faire correspondre chaque diagramme, capture et description à la version finale réellement testée.
- Séparer clairement les réalisations du stage, les améliorations ultérieures et les perspectives.

## Sources techniques à référencer

- Code source final du dépôt GestionConge et historique Git.
- Documentation officielle Spring Boot, Spring Security, Next.js, React et PostgreSQL, pour les versions effectivement utilisées.
- Code du travail malgache, loi n° 2024-014, articles 125 à 130, après vérification de la version officielle.
- Documents institutionnels SPAT autorisés à la consultation et à la citation.
