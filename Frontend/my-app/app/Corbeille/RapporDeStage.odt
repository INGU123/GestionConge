"use client";

import React from "react";

const chapters = [
  {
    id: "chapitre-1",
    title: "Chapitre I — Présentation de l’organisme d’accueil et du cadre du stage",
    sections: [
      ["1.1. Présentation générale de l’organisme", "La Société du Port à gestion Autonome de Toamasina (SPAT) constitue le cadre professionnel de ce stage. Le port de Toamasina occupe une place importante dans les échanges commerciaux de Madagascar : il assure une partie essentielle des opérations d’importation et d’exportation et met en relation différents acteurs du transport, de la logistique et de l’administration. Pour la version définitive, cette présentation doit être complétée à partir des documents officiels de la SPAT : historique, statut juridique, missions officielles, organigramme et chiffres institutionnels à jour."],
      ["1.2. Environnement administratif", "Comme dans toute organisation qui emploie des collaborateurs répartis entre plusieurs services, la gestion des ressources humaines doit disposer d’informations fiables sur les agents, leurs affectations, leurs droits et leurs absences. La circulation de ces informations doit rester compréhensible, traçable et cohérente avec les règles internes. Les procédures exactes appliquées par le service d’accueil doivent être décrites après validation auprès du maître de stage."],
      ["1.3. Cadre du stage", "Le stage s’inscrit dans le parcours de Licence en Économie Administrative et Sociale et associe découverte du milieu professionnel et mise en pratique de compétences informatiques. Le travail présenté porte sur la conception et le développement d’une application web de gestion des congés appelée GestionConge.", "Période du stage : [à compléter avec les dates exactes]. Service d’accueil : [à compléter]. Encadreur professionnel : [nom et fonction à compléter]. Encadreur pédagogique : [nom à compléter]."],
      ["1.4. Missions et objectifs", "L’objectif technique est de contribuer à une application permettant de centraliser les demandes de congé, de suivre leur statut et de consulter les informations liées aux utilisateurs, aux types de congé et aux soldes. Les tâches réellement réalisées pendant le stage doivent être distinguées des fonctionnalités existantes dans le dépôt : cette distinction garantit que le rapport décrit fidèlement le travail personnel accompli."]
    ]
  },
  {
    id: "chapitre-2",
    title: "Chapitre II — Étude de l’existant, problématique et analyse des besoins",
    sections: [
      ["2.1. Contexte et étude de l’existant", "La gestion des congés nécessite de réunir plusieurs informations : identité du demandeur, type de congé, période souhaitée, nombre de jours, statut de la demande et décision du responsable. Lorsque ces informations sont dispersées entre des documents ou des échanges, le suivi peut devenir difficile et l’historique des décisions moins facile à consulter. Ces risques constituent des constats généraux à confronter à la procédure réellement utilisée par la SPAT avant de les présenter comme des difficultés observées sur le terrain."],
      ["2.2. Problématique", "Comment concevoir une application web sécurisée qui centralise les informations relatives aux congés, facilite le traitement des demandes et améliore la traçabilité des opérations, tout en respectant les règles de gestion définies par l’organisation ?"],
      ["2.3. Objectif général et objectifs spécifiques", "L’objectif général est de mettre en place un outil de gestion des congés accessible par interface web. Les objectifs spécifiques sont : gérer les comptes et les rôles ; enregistrer et consulter les demandes ; gérer les types de congé ; suivre les soldes ; conserver un historique des mouvements ; transmettre des notifications ; sécuriser l’accès aux données ; et faciliter l’exploitation des informations disponibles."],
      ["2.4. Acteurs du système", "Les rôles métier envisagés dans l’application sont le collaborateur, qui consulte ses informations et soumet une demande ; le responsable ou valideur, qui examine les demandes selon les habilitations attribuées ; et l’administrateur, qui gère les données de référence et les comptes autorisés. Les droits exacts doivent être confirmés à partir de la configuration de sécurité et des écrans effectivement livrés. Le rapport ne doit pas supposer qu’un rôle dispose d’une action si le code ne l’autorise pas."],
      ["2.5. Besoins fonctionnels", "Les besoins fonctionnels identifiés dans le périmètre du dépôt comprennent la gestion des utilisateurs, des services, des types de congé, des demandes, des soldes, de l’historique et des notifications. Le projet comprend également une fonctionnalité d’export PDF dans le backend. Chaque besoin doit être relié à un écran, un endpoint ou un test dans la version finale."],
      ["2.6. Besoins non fonctionnels et limites", "L’application doit protéger les données personnelles, contrôler les accès, fournir des réponses compréhensibles, maintenir la cohérence des données et rester suffisamment simple à utiliser. La sécurité, la disponibilité, la performance et l’ergonomie doivent être évaluées par des tests et non déclarées acquises sans preuve. Le présent rapport ne présente aucun résultat de test comme réussi tant qu’il n’a pas été exécuté et consigné."]
    ]
  },
  {
    id: "chapitre-3",
    title: "Chapitre III — Conception et modélisation de la solution",
    sections: [
      ["3.1. Architecture générale", "Le projet adopte une architecture web séparant l’interface utilisateur, l’API métier et le stockage relationnel. Le frontend est développé avec Next.js et React. Le backend repose sur Java et Spring Boot ; il expose des contrôleurs, s’appuie sur des services et des repositories, et utilise Spring Data JPA/Hibernate pour la persistance. PostgreSQL assure le stockage des données. L’authentification repose notamment sur des composants JWT et une configuration Spring Security présents dans le dépôt."],
      ["3.2. Modélisation des données", "Les entités Java examinées comprennent Utilisateur, DemandeConge, Type_conge, Solde_conge, Historique_mouvement, Notification et Services. Plusieurs liens sont représentés par des champs identifiants tels que utilisateurId, typeCongeId, demande_id ou service_id, plutôt que par des associations JPA objet-à-objet. Le schéma ci-dessous est donc une vue conceptuelle des références fonctionnelles, et non une affirmation que toutes ces références sont des contraintes de clé étrangère dans la base."],
      ["3.3. Processus de demande", "Le parcours métier peut être représenté de façon conceptuelle : le collaborateur prépare une demande, l’application vérifie les données transmises, la demande est enregistrée, puis un acteur habilité l’examine et une décision est conservée. Le statut, la date de validation, l’identifiant du valideur et le motif de refus figurent dans l’entité DemandeConge. Les règles précises de transition des statuts doivent être vérifiées dans DemandeCongeService et Demande_CongeController avant de figer le diagramme de processus."],
      ["3.4. Sécurité et contrôle d’accès", "Le backend contient SecurityConfig, JwtAuthFilter, JwtUtil, CustomUserDetails et CustomUserDetailsService. Ces composants participent à la mise en œuvre de l’authentification et de l’autorisation. L’entité Utilisateur implémente UserDetails et son rôle est stocké sous forme d’énumération. Les mots de passe et les informations de réinitialisation sont marqués pour ne pas être exposés dans les réponses JSON. Une revue complémentaire des règles HTTP et des contrôleurs reste nécessaire pour documenter précisément les autorisations de chaque endpoint."],
      ["3.5. Modélisation de l’interface", "L’interface Next.js regroupe des pages de tableau de bord, de demandes, d’historique, de soldes, de types de congé, de services, d’utilisateurs, de validation, de calendrier et de notifications. Les captures d’écran à insérer dans la version imprimée doivent provenir de la version réellement exécutée. Pour chaque capture, indiquer le rôle utilisé, l’objectif de l’écran et l’action illustrée."]
    ]
  },
  {
    id: "chapitre-4",
    title: "Chapitre IV — Environnement technique et réalisation",
    sections: [
      ["4.1. Outils et technologies", "Java 17 et Spring Boot forment la base du backend. Maven assure la gestion de la construction et des dépendances. Spring Web sert à exposer les contrôleurs HTTP ; Spring Data JPA et Hibernate gèrent la persistance. Spring Security et les composants JWT participent à la sécurité. PostgreSQL est la base relationnelle. Le frontend repose sur Next.js et React, avec JavaScript/TypeScript selon les fichiers et des composants d’interface web. Les versions exactes à mentionner doivent être relevées dans les fichiers de configuration utilisés pour la livraison finale."],
      ["4.2. Organisation du backend", "Le code backend est organisé par domaines fonctionnels, notamment demande_conge, utilisateur, type_conge, solde_conge, historique_mouvement, notifications_conge et service_conge. Plusieurs domaines disposent de contrôleurs, services, repositories et entités. Cette organisation sépare les responsabilités : les contrôleurs reçoivent les requêtes, les services portent la logique métier et les repositories accèdent aux données."],
      ["4.3. Gestion des demandes et des soldes", "L’entité DemandeConge stocke notamment l’identifiant du demandeur, le type de congé, les dates de début et de fin, le nombre de jours, le commentaire, le statut, le motif de refus, le valideur et les dates de création et de validation. Solde_conge contient les soldes acquis, pris et restants, ainsi que la période et la date de mise à jour. Ces champs donnent une base à la description technique ; les règles de calcul doivent être expliquées à partir du code du service et des tests correspondants."],
      ["4.4. Historique et notifications", "Historique_mouvement conserve des informations telles que l’utilisateur concerné, le type de mouvement, la quantité, le type de congé, la demande associée, le commentaire, l’auteur de l’opération et la date. Notification contient notamment l’utilisateur destinataire, le contenu, le type, l’état de lecture, les dates et un lien. Ces structures permettent de décrire le suivi prévu par l’application sans présumer que tous les scénarios de notification sont opérationnels."],
      ["4.5. Export PDF", "Le dépôt contient un service nommé PdfExportService dans le package export. La présence du service confirme l’existence d’un composant dédié à l’export PDF côté backend, mais ne suffit pas à démontrer que tous les écrans disposent d’un export fonctionnel. Le rapport final doit préciser les données réellement exportées, le contrôleur appelé, le format obtenu et le résultat d’un essai manuel."],
      ["4.6. Difficultés et solutions", "Cette section doit relater des difficultés effectivement rencontrées durant le stage : problème, cause identifiée, démarche de résolution et résultat constaté. Les exemples doivent être complétés à partir du journal de travail personnel : configuration de l’environnement, connexion PostgreSQL, authentification, affichage des données ou génération de PDF, selon les difficultés réellement rencontrées. Éviter d’attribuer au stage une correction qui a été réalisée en dehors de la période ou par une autre personne."]
    ]
  },
  {
    id: "chapitre-5",
    title: "Chapitre V — Tests, résultats et validation",
    sections: [
      ["5.1. Démarche de test", "Les tests doivent vérifier les comportements attendus à partir de scénarios reproductibles. Pour chaque test, consigner les préconditions, les données utilisées, les étapes, le résultat attendu, le résultat observé et le statut final. Les données personnelles réelles doivent être remplacées par des données fictives dans les captures et comptes rendus."],
      ["5.2. Matrice de tests à compléter", "La matrice proposée ci-dessous est un plan de vérification, pas un compte rendu de tests déjà réussis. Exécuter chaque scénario sur la version de livraison et remplacer « À exécuter » par le résultat réel accompagné, si possible, d’une preuve."],
      ["5.3. Analyse des résultats", "Après exécution, présenter les fonctionnalités validées, les anomalies restantes, leur gravité et les corrections envisagées. Les tests unitaires présents dans le dépôt constituent un point de départ, mais leur seule présence ne prouve pas que la suite complète passe. Joindre la commande utilisée et le résumé réel des résultats Maven/JUnit."],
      ["5.4. Limites connues", "Les limites doivent être établies après revue de la version finale : scénarios métier non couverts, règles de calcul à confirmer, couverture des tests, gestion des erreurs, accessibilité, performance, sauvegarde et restauration, ou fonctions encore incomplètes. Ne conserver dans cette section que les limites confirmées."]
    ]
  },
  {
    id: "chapitre-6",
    title: "Chapitre VI — Bilan du stage et perspectives",
    sections: [
      ["6.1. Bilan technique", "Le projet permet de mobiliser plusieurs notions complémentaires : structuration d’une API REST, séparation contrôleur-service-repository, persistance relationnelle, authentification, gestion d’états métier et développement d’interfaces web. Le bilan personnel doit préciser les parties effectivement conçues, codées, testées ou documentées par le stagiaire, sans confondre l’ensemble du dépôt et sa contribution individuelle."],
      ["6.2. Bilan professionnel et personnel", "Le stage offre l’occasion de mieux comprendre les attentes d’un environnement professionnel : organisation du travail, communication avec l’encadreur, prise en compte des besoins utilisateurs, documentation et vérification des livrables. Cette partie doit être personnalisée avec des exemples concrets, les compétences acquises et les difficultés qui ont réellement fait progresser le stagiaire."],
      ["6.3. Perspectives d’amélioration", "Les pistes possibles sont l’amélioration de la couverture des tests, la vérification systématique des droits par rôle, la documentation de l’API, la journalisation des actions sensibles, la robustesse de l’envoi des courriels, l’amélioration des rapports RH et des exports, ainsi que la mise en place d’une procédure de sauvegarde et de restauration. Chaque proposition doit être présentée comme une perspective tant qu’elle n’est pas réalisée et validée."]
    ]
  }
];

const tests = [
  ["Authentification", "Connexion avec identifiants valides", "Session/token obtenu et accès autorisé", "À exécuter"],
  ["Autorisation", "Accès à une action avec un rôle non habilité", "Accès refusé", "À exécuter"],
  ["Demande", "Création d’une demande avec dates valides", "Demande enregistrée avec statut initial cohérent", "À exécuter"],
  ["Validation", "Décision d’un utilisateur habilité", "Statut et informations de validation enregistrés", "À exécuter"],
  ["Solde", "Consultation du solde d’un utilisateur", "Valeurs cohérentes avec les mouvements", "À exécuter"],
  ["Historique", "Consultation des mouvements", "Mouvements pertinents et dates cohérentes", "À exécuter"],
  ["Notification", "Génération puis lecture d’une notification", "État de lecture mis à jour", "À exécuter"],
  ["Export PDF", "Déclenchement de l’export prévu", "PDF lisible et données conformes", "À exécuter"]
];

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return <section className="subsection"><h3>{title}</h3>{children}</section>;
}

export default function RapportDeStage() {
  return (
    <main className="report">
      <div className="toolbar no-print">
        <span>Rapport de stage — GestionConge</span>
        <button onClick={() => window.print()}>Imprimer / Enregistrer en PDF</button>
      </div>

      <section className="cover page-break">
        <div className="cover-top">RÉPUBLIQUE DE MADAGASCAR<br />Fitiavana · Tanindrazana · Fandrosoana</div>
        <div className="cover-rule" />
        <p className="institution">ÉTABLISSEMENT DE FORMATION : [À COMPLÉTER]</p>
        <p className="degree">Licence en Économie Administrative et Sociale</p>
        <div className="cover-spacer" />
        <p className="label">RAPPORT DE STAGE</p>
        <h1>Conception et développement d’une application web de gestion des congés</h1>
        <p className="project-name">Projet : GestionConge</p>
        <div className="cover-spacer" />
        <div className="cover-info">
          <p><strong>Organisme d’accueil :</strong> Société du Port à gestion Autonome de Toamasina (SPAT)</p>
          <p><strong>Présenté par :</strong> Fruvio [nom complet à confirmer]</p>
          <p><strong>Service d’accueil :</strong> [À compléter]</p>
          <p><strong>Encadreur professionnel :</strong> [À compléter]</p>
          <p><strong>Encadreur pédagogique :</strong> [À compléter]</p>
          <p><strong>Période du stage :</strong> [Dates exactes à compléter]</p>
        </div>
        <p className="academic-year">Année universitaire : [À compléter]</p>
      </section>

      <section className="front-page page-break">
        <h1>Remerciements</h1>
        <p>Je tiens à exprimer ma reconnaissance à toutes les personnes qui ont contribué au bon déroulement de mon stage et à mon apprentissage professionnel. Je remercie l’organisme d’accueil pour l’opportunité de découvrir son environnement de travail, ainsi que mon encadreur professionnel pour son accompagnement et ses orientations. J’adresse également mes remerciements à l’équipe pédagogique pour les connaissances acquises au cours de ma formation.</p>
        <p><em>À personnaliser en citant les personnes et fonctions concernées, avec leur accord.</em></p>
      </section>

      <section className="front-page page-break">
        <h1>Résumé</h1>
        <p>Ce rapport présente le travail réalisé autour de GestionConge, une application web destinée à organiser les informations et les opérations liées à la gestion des congés. Le projet repose sur une architecture séparant une interface Next.js/React, un backend Java Spring Boot et une base de données PostgreSQL. Les fonctionnalités structurantes du domaine couvrent les utilisateurs, les demandes, les types de congé, les soldes, l’historique et les notifications. Le backend comprend également des composants d’authentification et de contrôle d’accès ainsi qu’un service d’export PDF.</p>
        <p>Le rapport expose le contexte, la problématique, les besoins, la conception, les technologies utilisées et une démarche de validation. Les dates du stage, les tâches personnelles, les résultats de tests et les observations propres à l’organisme doivent être confirmés et complétés avant la remise officielle.</p>
        <p><strong>Mots-clés :</strong> gestion des congés, application web, Spring Boot, Next.js, PostgreSQL, sécurité, modélisation.</p>
        <h2>Abstract</h2>
        <p>This report describes the GestionConge web application, designed to support leave-management information and workflows. The project separates a Next.js/React user interface, a Java Spring Boot backend and a PostgreSQL database. Its domain includes users, leave requests, leave types, balances, movement history and notifications. The report outlines the context, requirements, architecture, implementation approach and a test plan. Internship details, personal contributions and actual test results must be confirmed before the final submission.</p>
        <p><strong>Keywords:</strong> leave management, web application, Spring Boot, Next.js, PostgreSQL, security, modelling.</p>
      </section>

      <section className="front-page page-break">
        <h1>Liste des abréviations</h1>
        <table><tbody>
          <tr><th>API</th><td>Application Programming Interface</td></tr>
          <tr><th>CSS</th><td>Cascading Style Sheets</td></tr>
          <tr><th>HTTP</th><td>Hypertext Transfer Protocol</td></tr>
          <tr><th>JPA</th><td>Java Persistence API</td></tr>
          <tr><th>JWT</th><td>JSON Web Token</td></tr>
          <tr><th>ORM</th><td>Object-Relational Mapping</td></tr>
          <tr><th>PDF</th><td>Portable Document Format</td></tr>
          <tr><th>RH</th><td>Ressources humaines</td></tr>
          <tr><th>SQL</th><td>Structured Query Language</td></tr>
          <tr><th>UML</th><td>Unified Modeling Language</td></tr>
        </tbody></table>
        <h2>Sommaire</h2>
        <ol className="toc">
          <li>Introduction générale</li>
          {chapters.map(c => <li key={c.id}>{c.title}</li>)}
          <li>Conclusion générale</li>
          <li>Bibliographie et webographie</li>
          <li>Annexes</li>
        </ol>
        <p className="note">Les numéros de page sont volontairement laissés à générer après mise en page et impression.</p>
      </section>

      <section className="chapter page-break">
        <h1>Introduction générale</h1>
        <p>La gestion des congés constitue une activité administrative qui exige la disponibilité d’informations fiables sur les collaborateurs, les périodes d’absence, les droits acquis et les décisions prises. Un outil numérique adapté peut centraliser ces informations, rendre leur consultation plus structurée et améliorer la traçabilité du traitement des demandes.</p>
        <p>Dans ce contexte, le projet GestionConge consiste à développer une application web organisée autour d’une interface utilisateur, d’une API backend et d’une base de données relationnelle. Le dépôt consulté contient des modules de gestion des demandes, des utilisateurs, des types de congé, des soldes, de l’historique et des notifications, ainsi que des composants liés à la sécurité et à l’export PDF.</p>
        <p>La problématique retenue est la suivante : comment concevoir une application web sécurisée permettant de centraliser les données de congés et de faciliter le suivi des demandes tout en conservant une trace des opérations ?</p>
        <p>Le rapport est organisé en six chapitres. Le premier présente l’organisme d’accueil et le cadre du stage. Le deuxième analyse l’existant et les besoins. Le troisième décrit la conception et la modélisation. Le quatrième présente l’environnement technique et la réalisation. Le cinquième définit la démarche de test et l’analyse des résultats. Le sixième dresse le bilan et propose des perspectives d’amélioration.</p>
      </section>

      {chapters.map((chapter) => (
        <section className="chapter page-break" id={chapter.id} key={chapter.id}>
          <h1>{chapter.title}</h1>
          {chapter.sections.map(([title, body, extra]) => (
            <Section title={title} key={title}>
              <p>{body}</p>
              {extra ? <p className="highlight">{extra}</p> : null}
              {title.startsWith("3.1.") ? (
                <div className="diagram">
                  <div className="diagram-node"><strong>Frontend</strong><span>Next.js · React</span><span>Pages et composants UI</span></div>
                  <div className="diagram-arrow">↕ Requêtes HTTP / réponses JSON</div>
                  <div className="diagram-node"><strong>Backend</strong><span>Spring Boot · Java</span><span>Contrôleurs → Services → Repositories</span><span>Spring Security · JWT</span></div>
                  <div className="diagram-arrow">↕ JPA / Hibernate</div>
                  <div className="diagram-node"><strong>Base de données</strong><span>PostgreSQL</span><span>Utilisateurs, demandes, soldes, historique…</span></div>
                  <p className="figure-caption">Figure 1 — Architecture logique déduite de la structure du dépôt.</p>
                </div>
              ) : null}
              {title.startsWith("3.2.") ? (
                <div className="diagram">
                  <div className="entity-grid">
                    <div className="entity"><strong>Utilisateur</strong><span>id, matricule, nom, email, rôle</span></div>
                    <div className="entity"><strong>DemandeConge</strong><span>utilisateurId, typeCongeId, dates, statut</span></div>
                    <div className="entity"><strong>Type_conge</strong><span>code, libellé, règles, jours/an</span></div>
                    <div className="entity"><strong>Solde_conge</strong><span>utilisateurId, typeCongeId, acquis, pris, restant</span></div>
                    <div className="entity"><strong>Historique_mouvement</strong><span>utilisateurId, demande_id, quantité, date</span></div>
                    <div className="entity"><strong>Notification</strong><span>utilisateurId, contenu, type, lue</span></div>
                    <div className="entity"><strong>Services</strong><span>nom, responsable_id, effectifMinimum</span></div>
                  </div>
                  <p className="figure-caption">Figure 2 — Principales entités du code. Les identifiants indiquent des références fonctionnelles ; les contraintes relationnelles doivent être vérifiées dans le schéma PostgreSQL.</p>
                </div>
              ) : null}
              {title.startsWith("3.3.") ? (
                <div className="process">
                  <div>1. Saisie de la demande</div><b>↓</b><div>2. Contrôles des données</div><b>↓</b><div>3. Enregistrement et statut initial</div><b>↓</b><div>4. Examen par un acteur habilité</div><b>↓</b><div>5. Décision et traçabilité</div>
                  <p className="figure-caption">Figure 3 — Flux métier conceptuel à confirmer avec les règles du service et les contrôleurs.</p>
                </div>
              ) : null}
            </Section>
          ))}
          {chapter.id === "chapitre-5" ? (
            <table className="test-table">
              <thead><tr><th>Domaine</th><th>Scénario</th><th>Résultat attendu</th><th>État</th></tr></thead>
              <tbody>{tests.map((t) => <tr key={t[0]}>{t.map((v, i) => <td key={i}>{v}</td>)}</tr>)}</tbody>
            </table>
          ) : null}
        </section>
      ))}

      <section className="chapter page-break">
        <h1>Conclusion générale</h1>
        <p>Le projet GestionConge illustre la mise en place d’une application web organisée en plusieurs couches : interface, logique métier, sécurité et persistance. L’examen du dépôt met en évidence des modules consacrés aux demandes de congé, aux utilisateurs, aux types de congé, aux soldes, à l’historique et aux notifications. La présence de composants d’authentification et d’un service d’export PDF montre également que le projet couvre des préoccupations qui dépassent la simple saisie de données.</p>
        <p>La valeur finale de la solution dépend toutefois de la conformité des règles métier, de la validation des droits d’accès, de la cohérence des soldes et des résultats des tests. Le stage permet de relier la formation aux exigences d’un projet informatique et de développer une démarche structurée d’analyse, de conception, de réalisation et de vérification.</p>
        <p>Avant la remise, cette conclusion doit être enrichie par les résultats effectivement obtenus, les contributions personnelles précises et les recommandations validées avec l’encadreur.</p>
      </section>

      <section className="chapter page-break">
        <h1>Bibliographie et webographie</h1>
        <ol>
          <li>Documentation officielle de Spring Boot, Spring Framework et Spring Security — <span>https://spring.io/projects</span></li>
          <li>Documentation officielle de Next.js — <span>https://nextjs.org/docs</span></li>
          <li>Documentation officielle de React — <span>https://react.dev</span></li>
          <li>Documentation officielle de PostgreSQL — <span>https://www.postgresql.org/docs/</span></li>
          <li>Dépôt source du projet GestionConge, branche consultée : <span>https://github.com/INGU123/GestionConge</span></li>
          <li>Documents institutionnels officiels de la SPAT : [références exactes à ajouter].</li>
          <li>Consignes et documents pédagogiques de l’établissement : [références à ajouter].</li>
        </ol>
      </section>

      <section className="chapter page-break">
        <h1>Annexes</h1>
        <h2>Annexe A — Captures d’écran de l’application</h2>
        <p>Insérer des captures authentiques de la page de connexion, du tableau de bord, de la demande de congé, de la validation, du solde, de l’historique et de l’export PDF. Masquer les noms, adresses électroniques, matricules et toute donnée réelle non destinée à la diffusion.</p>
        <h2>Annexe B — Éléments techniques</h2>
        <p>Joindre les extraits de code courts et commentés qui démontrent les mécanismes décrits : contrôleur de demande, service métier, repository, configuration de sécurité et service PDF. Éviter de reproduire des fichiers entiers ou des secrets de configuration.</p>
        <h2>Annexe C — Preuves de tests</h2>
        <p>Ajouter les comptes rendus des tests réellement exécutés, les résultats Maven/JUnit, les anomalies connues et les corrections apportées. Aucun résultat ne doit être déclaré positif sans preuve reproductible.</p>
        <h2>Annexe D — Informations à compléter avant dépôt</h2>
        <ul>
          <li>Nom officiel de l’établissement et année universitaire.</li>
          <li>Dates exactes, service d’accueil et noms/fonctions des encadreurs.</li>
          <li>Présentation de la SPAT vérifiée avec une source officielle.</li>
          <li>Missions réellement effectuées par le stagiaire et calendrier de réalisation.</li>
          <li>Captures de la version livrée et résultats de tests réellement observés.</li>
          <li>Pagination, table des matières définitive et relecture orthographique.</li>
        </ul>
      </section>

      <footer className="report-footer">Fin du rapport — GestionConge</footer>

      <style jsx global>{`
        * { box-sizing: border-box; }
        body { margin: 0; background: #edf1f5; color: #202a36; font-family: Arial, Helvetica, sans-serif; }
        .report { max-width: 980px; margin: 28px auto; padding: 0 22px 50px; }
        .toolbar { display:flex; justify-content:space-between; align-items:center; gap:16px; padding:14px 18px; background:#173b63; color:white; border-radius:10px; margin-bottom:22px; }
        .toolbar button { border:0; border-radius:7px; background:#fff; color:#173b63; padding:10px 15px; font-weight:700; cursor:pointer; }
        .cover, .front-page, .chapter { background:#fff; padding:60px 68px; margin:0 0 22px; box-shadow:0 5px 22px #10203012; border-radius:4px; }
        .cover { min-height:1050px; display:flex; flex-direction:column; text-align:center; border-top:12px solid #173b63; }
        .cover-top { font-size:13px; line-height:1.8; font-weight:700; letter-spacing:.5px; }
        .cover-rule { height:3px; background:#b28b45; margin:22px 15%; }
        .institution { margin-top:26px; font-weight:700; font-size:14px; }
        .degree { color:#586779; }
        .cover-spacer { flex:1; min-height:36px; }
        .label { letter-spacing:4px; font-weight:700; color:#a57a31; }
        .cover h1 { font-size:29px; line-height:1.35; color:#173b63; margin:10px 0; }
        .project-name { font-size:18px; font-weight:700; }
        .cover-info { text-align:left; max-width:570px; margin:0 auto; line-height:1.55; font-size:14px; }
        .cover-info p { margin:7px 0; }
        .academic-year { font-weight:700; margin-top:34px; }
        h1 { color:#173b63; font-size:27px; line-height:1.35; border-bottom:2px solid #b28b45; padding-bottom:13px; margin:0 0 26px; }
        h2 { color:#214d78; font-size:20px; margin:26px 0 12px; }
        h3 { color:#214d78; font-size:17px; margin:25px 0 10px; }
        p, li { font-size:14px; line-height:1.8; text-align:justify; }
        p { margin:0 0 13px; }
        .subsection { margin:0 0 23px; }
        .highlight, .note { padding:12px 14px; background:#f2f6fa; border-left:3px solid #b28b45; }
        table { width:100%; border-collapse:collapse; margin:18px 0 24px; font-size:13px; }
        th, td { border:1px solid #cbd4de; padding:9px 10px; text-align:left; vertical-align:top; line-height:1.5; }
        th { background:#eaf0f6; color:#173b63; }
        .toc li { margin:7px 0; }
        .diagram, .process { border:1px solid #d4dde6; border-radius:10px; padding:18px; margin:22px 0; background:#fbfcfe; }
        .diagram-node { max-width:520px; margin:0 auto; padding:13px; border:2px solid #34638c; background:#eef5fb; border-radius:9px; display:flex; flex-direction:column; text-align:center; gap:4px; font-size:13px; }
        .diagram-node strong { font-size:15px; color:#173b63; }
        .diagram-arrow { text-align:center; color:#526a80; font-size:13px; font-weight:700; padding:10px; }
        .entity-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }
        .entity { display:flex; flex-direction:column; gap:6px; border:1px solid #9db4c8; border-radius:7px; padding:12px; background:#fff; font-size:12px; }
        .entity strong { color:#173b63; font-size:14px; }
        .figure-caption { font-size:11px; text-align:center; color:#536273; margin:15px 0 0; font-style:italic; }
        .process { display:flex; flex-direction:column; align-items:stretch; gap:8px; }
        .process div { padding:10px; border-radius:6px; border:1px solid #9db4c8; background:#f3f7fb; text-align:center; font-size:13px; }
        .process b { text-align:center; color:#9b762f; }
        .test-table { font-size:11px; }
        .test-table td, .test-table th { padding:7px; }
        .report-footer { text-align:center; color:#657486; font-size:12px; padding:20px; }
        @media (max-width:700px) {
          .report { padding:0 8px 25px; margin:8px auto; }
          .cover, .front-page, .chapter { padding:28px 22px; }
          .cover { min-height:850px; }
          .cover h1 { font-size:23px; }
          h1 { font-size:23px; }
          .entity-grid { grid-template-columns:1fr; }
          .toolbar { align-items:flex-start; flex-direction:column; }
          table { font-size:11px; }
        }
        @media print {
          @page { size:A4; margin:18mm 17mm 18mm; }
          body { background:#fff; color:#111; }
          .report { max-width:none; margin:0; padding:0; }
          .no-print { display:none !important; }
          .cover, .front-page, .chapter { box-shadow:none; border-radius:0; margin:0; padding:0; }
          .cover { min-height:260mm; page-break-after:always; }
          .page-break { break-before:page; page-break-before:always; }
          .chapter, .front-page { break-inside:auto; }
          h1, h2, h3 { break-after:avoid; }
          p, li, table, .diagram, .process { break-inside:avoid; }
          .subsection { break-inside:auto; }
          .diagram, .process { print-color-adjust:exact; -webkit-print-color-adjust:exact; }
          a, span { overflow-wrap:anywhere; }
        }
      `}</style>
    </main>
  );
}
