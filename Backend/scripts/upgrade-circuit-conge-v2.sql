-- Migration additive et idempotente du circuit hiérarchique des congés.
-- À exécuter sur une sauvegarde de la base avant de démarrer l'application
-- avec DDL_AUTO=validate. Aucune table métier existante ni donnée n'est supprimée.
--
-- La création conditionnelle couvre aussi les bases qui n'ont jamais démarré
-- avec DDL_AUTO=update depuis l'ajout de l'entité CircuitDemandeConge.

CREATE TABLE IF NOT EXISTS circuit_demande_conge (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL,
    type_conge_id BIGINT NOT NULL,
    departement_id BIGINT,
    assistant_id BIGINT NOT NULL,
    administration_personnel_id BIGINT,
    chef_id BIGINT,
    directeur_id BIGINT,
    legacy_demande_id BIGINT,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    nombre_jours INTEGER NOT NULL,
    commentaire VARCHAR(1500),
    statut VARCHAR(40) NOT NULL,
    reliquat_jours INTEGER,
    reliquat_commentaire VARCHAR(1500),
    motif_refus VARCHAR(1500),
    assistant_correction VARCHAR(1500),
    avis_chef VARCHAR(20),
    commentaire_chef VARCHAR(1500),
    date_creation TIMESTAMP NOT NULL,
    date_reliquat TIMESTAMP,
    date_soumission_chef TIMESTAMP,
    date_transmission_directeur TIMESTAMP,
    date_decision TIMESTAMP
);

-- Complète une table déjà créée par une version antérieure du circuit.
ALTER TABLE circuit_demande_conge
    ADD COLUMN IF NOT EXISTS assistant_correction VARCHAR(1500),
    ADD COLUMN IF NOT EXISTS avis_chef VARCHAR(20),
    ADD COLUMN IF NOT EXISTS commentaire_chef VARCHAR(1500);

-- Contrôle non destructif : cette migration ne remet aucun statut ou solde à zéro.
