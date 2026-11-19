-- Référentiel employés et rattachement des nouveaux dossiers de congé.
-- Migration additive : les anciens dossiers utilisateur_id sont conservés.
CREATE TABLE IF NOT EXISTS employe (
    id BIGSERIAL PRIMARY KEY,
    matricule VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    numero_cin VARCHAR(30) UNIQUE,
    date_naissance DATE,
    telephone VARCHAR(30),
    adresse VARCHAR(500),
    fonction VARCHAR(150),
    type_contrat VARCHAR(80),
    email VARCHAR(254),
    date_embauche DATE,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
DO $
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_employe_matricule_digits') THEN
        ALTER TABLE employe ADD CONSTRAINT ck_employe_matricule_digits CHECK (matricule ~ '^[0-9]+
ALTER TABLE circuit_demande_conge ADD COLUMN IF NOT EXISTS employe_id BIGINT;
ALTER TABLE circuit_demande_conge ALTER COLUMN utilisateur_id DROP NOT NULL;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_circuit_demande_employe') THEN
        ALTER TABLE circuit_demande_conge ADD CONSTRAINT fk_circuit_demande_employe
            FOREIGN KEY (employe_id) REFERENCES employe(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_circuit_demande_employe ON circuit_demande_conge(employe_id);

-- Historique des décisions : l'employé est référencé indépendamment d'un compte utilisateur.
ALTER TABLE demande_conge ADD COLUMN IF NOT EXISTS employe_id BIGINT;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_demande_conge_employe') THEN
        ALTER TABLE demande_conge ADD CONSTRAINT fk_demande_conge_employe
            FOREIGN KEY (employe_id) REFERENCES employe(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_demande_conge_employe ON demande_conge(employe_id);
);
    END IF;
END $;
CREATE INDEX IF NOT EXISTS idx_employe_matricule ON employe(matricule);
ALTER TABLE circuit_demande_conge ADD COLUMN IF NOT EXISTS employe_id BIGINT;
ALTER TABLE circuit_demande_conge ALTER COLUMN utilisateur_id DROP NOT NULL;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_circuit_demande_employe') THEN
        ALTER TABLE circuit_demande_conge ADD CONSTRAINT fk_circuit_demande_employe
            FOREIGN KEY (employe_id) REFERENCES employe(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_circuit_demande_employe ON circuit_demande_conge(employe_id);

-- Historique des décisions : l'employé est référencé indépendamment d'un compte utilisateur.
ALTER TABLE demande_conge ADD COLUMN IF NOT EXISTS employe_id BIGINT;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_demande_conge_employe') THEN
        ALTER TABLE demande_conge ADD CONSTRAINT fk_demande_conge_employe
            FOREIGN KEY (employe_id) REFERENCES employe(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_demande_conge_employe ON demande_conge(employe_id);
