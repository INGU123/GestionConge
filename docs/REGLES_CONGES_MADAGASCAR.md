# Règles de congés — périmètre et décisions (Madagascar)

## Référence
Loi n° 2024-014, Code du travail, articles 125 à 130 : https://cnlegis.gov.mg/uploads/L2024-014-VF.pdf

## Corrections techniques de cette branche
- Rejeter les demandes incomplètes et les dates de début antérieures à la date courante.
- Répartir les débits/restitutions par année civile (sur le moteur historique de jours ouvrés).
- Ne pas valider une demande si l'écriture de l'historique échoue ; l'opération doit être transactionnelle.
- Refuser l'annulation ordinaire d'un congé validé ayant commencé ; régularisation RH requise.
- Configurer les origines CORS via app.cors.allowed-origins ; exemple : APP_CORS_ALLOWED_ORIGINS=http://192.168.1.10:3000 (Spring property app.cors.allowed-origins).
- Le frontend possède déjà un ToastProvider et affiche les erreurs de demande. Conserver l'UI.

## Attention — règles légales NON encore intégrées
Cette branche ne doit PAS être déployée comme solution juridiquement conforme :
1. Les droits légaux sont acquis à raison de 2,5 jours **calendaires** par mois de service effectif (art. 125). L'ancien moteur décompte des jours ouvrés et initialise le quota annuel complet : migration des unités et des données indispensable.
2. Les périodes assimilées au service effectif (art. 126) doivent être historisées ; date d'embauche seule insuffisante.
3. La première jouissance après 12 mois et la première fraction de 15 jours (art. 130) nécessitent le suivi des périodes de référence et des congés déjà pris, et une procédure RH pour nécessité de service.
4. Report/prescription de trois ans et exceptions (art. 129), validation RH, calendrier des équipes portuaires et conditions particulières des autres congés nécessitent un modèle de droits par millésime, un règlement validé et des tests.
5. Les validations concurrentes nécessitent un verrou en base (ou version optimiste) et des tests d'intégration.
6. L'export PDF est encore présent dans le backend ; ne pas supprimer OpenPDF sans vérification des consommateurs.

## Bonnes pratiques pour les prochains éditeurs
- Ne jamais changer silencieusement l'unité d'un solde déjà enregistré ; effectuer une migration explicite et réversible avec sauvegarde préalable.
- Une règle métier doit indiquer sa source juridique, son périmètre (type de congé), ses paramètres et les tests de non-régression.
- Ne jamais inventer un délai minimum entre deux demandes : le Code n'impose pas de délai général.
- Les conventions collectives/accords SPAT peuvent être plus favorables ; valider avec RH avant activation des restrictions.
- Tester sur une copie de PostgreSQL, jamais sur les données réelles en premier.
