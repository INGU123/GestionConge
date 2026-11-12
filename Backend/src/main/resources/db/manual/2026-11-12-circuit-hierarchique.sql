-- Déploiement du circuit hiérarchique de congés.
-- Les services et utilisateurs existants sont conservés sans affectation automatique.
CREATE TABLE IF NOT EXISTS departement (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(120) NOT NULL UNIQUE,
    chef_id BIGINT NOT NULL UNIQUE REFERENCES utilisateur(id),
    directeur_id BIGINT NOT NULL UNIQUE REFERENCES utilisateur(id),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_departement_chef_directeur_distincts CHECK (chef_id <> directeur_id)
);

ALTER TABLE service ADD COLUMN IF NOT EXISTS departement_id BIGINT;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_service_departement') THEN
        ALTER TABLE service ADD CONSTRAINT fk_service_departement
            FOREIGN KEY (departement_id) REFERENCES departement(id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS circuit_demande_conge (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id),
    type_conge_id BIGINT NOT NULL REFERENCES type_conge(id),
    departement_id BIGINT REFERENCES departement(id),
    assistant_id BIGINT NOT NULL REFERENCES utilisateur(id),
    administration_personnel_id BIGINT REFERENCES utilisateur(id),
    chef_id BIGINT REFERENCES utilisateur(id),
    directeur_id BIGINT REFERENCES utilisateur(id),
    legacy_demande_id BIGINT REFERENCES demande_conge(id),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    nombre_jours INTEGER NOT NULL CHECK (nombre_jours > 0),
    commentaire VARCHAR(1500),
    statut VARCHAR(40) NOT NULL,
    reliquat_jours INTEGER,
    reliquat_commentaire VARCHAR(1500),
    motif_refus VARCHAR(1500),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_reliquat TIMESTAMP,
    date_soumission_chef TIMESTAMP,
    date_transmission_directeur TIMESTAMP,
    date_decision TIMESTAMP,
    CONSTRAINT ck_circuit_dates CHECK (date_fin >= date_debut),
    CONSTRAINT ck_circuit_reliquat CHECK (reliquat_jours IS NULL OR reliquat_jours >= 0)
);
CREATE INDEX IF NOT EXISTS idx_circuit_statut ON circuit_demande_conge(statut);
CREATE INDEX IF NOT EXISTS idx_circuit_assistant ON circuit_demande_conge(assistant_id);
CREATE INDEX IF NOT EXISTS idx_circuit_chef ON circuit_demande_conge(chef_id);
CREATE INDEX IF NOT EXISTS idx_circuit_directeur ON circuit_demande_conge(directeur_id);
