-- Migration additive du référentiel employés et du circuit de congé.
-- À exécuter une seule fois après sauvegarde de la base.
-- Les anciens liens utilisateur_id et leurs historiques sont conservés.

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
    service_id BIGINT NOT NULL REFERENCES service(id) ON DELETE RESTRICT,
    donnees_fictives BOOLEAN NOT NULL DEFAULT FALSE,
    type_contrat VARCHAR(80),
    email VARCHAR(254),
    date_embauche DATE,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_employe_matricule_digits'
    ) THEN
        ALTER TABLE employe
            ADD CONSTRAINT ck_employe_matricule_digits
            CHECK (matricule ~ '^[0-9]+$');
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_employe_matricule ON employe(matricule);

-- Compléter la table employe si elle existait déjà avant cette migration.
ALTER TABLE employe ADD COLUMN IF NOT EXISTS numero_cin VARCHAR(30);
ALTER TABLE employe ADD COLUMN IF NOT EXISTS date_naissance DATE;
ALTER TABLE employe ADD COLUMN IF NOT EXISTS telephone VARCHAR(30);
ALTER TABLE employe ADD COLUMN IF NOT EXISTS adresse VARCHAR(500);
ALTER TABLE employe ADD COLUMN IF NOT EXISTS type_contrat VARCHAR(80);
ALTER TABLE employe ADD COLUMN IF NOT EXISTS email VARCHAR(254);
ALTER TABLE employe ADD COLUMN IF NOT EXISTS date_embauche DATE;

-- Garantir l'unicité du CIN sur les tables existantes et nouvelles.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_index i
        JOIN pg_class t ON t.oid = i.indrelid
        JOIN pg_attribute a ON a.attrelid = t.oid
        WHERE t.relname = 'employe'
          AND a.attname = 'numero_cin'
          AND i.indisunique
          AND i.indnkeyatts = 1
          AND i.indkey[0] = a.attnum
    ) THEN
        CREATE UNIQUE INDEX uq_employe_numero_cin
            ON employe(numero_cin)
            WHERE numero_cin IS NOT NULL;
    END IF;
END $$;


-- Nouveau circuit : un dossier peut viser un employé sans compte utilisateur.
ALTER TABLE circuit_demande_conge
    ADD COLUMN IF NOT EXISTS employe_id BIGINT;
ALTER TABLE circuit_demande_conge
    ALTER COLUMN utilisateur_id DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_circuit_demande_employe'
    ) THEN
        ALTER TABLE circuit_demande_conge
            ADD CONSTRAINT fk_circuit_demande_employe
            FOREIGN KEY (employe_id) REFERENCES employe(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_circuit_demande_employe
    ON circuit_demande_conge(employe_id);

-- Demandes historiques / décisions : conservation du lien utilisateur existant.
ALTER TABLE demande_conge
    ADD COLUMN IF NOT EXISTS employe_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_demande_conge_employe'
    ) THEN
        ALTER TABLE demande_conge
            ADD CONSTRAINT fk_demande_conge_employe
            FOREIGN KEY (employe_id) REFERENCES employe(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_demande_conge_employe
    ON demande_conge(employe_id);

-- Soldes séparés pour les employés sans compte utilisateur.
ALTER TABLE solde_conge
    ADD COLUMN IF NOT EXISTS employe_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_solde_conge_employe'
    ) THEN
        ALTER TABLE solde_conge
            ADD CONSTRAINT fk_solde_conge_employe
            FOREIGN KEY (employe_id) REFERENCES employe(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_solde_conge_employe
    ON solde_conge(employe_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_solde_conge_employe_type_periode
    ON solde_conge(employe_id, type_conge_id, periode)
    WHERE employe_id IS NOT NULL;

-- Mouvements : rattachement à l'employé sans réécrire les anciennes lignes.
ALTER TABLE historique_mouvement
    ADD COLUMN IF NOT EXISTS employe_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_historique_mouvement_employe'
    ) THEN
        ALTER TABLE historique_mouvement
            ADD CONSTRAINT fk_historique_mouvement_employe
            FOREIGN KEY (employe_id) REFERENCES employe(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_historique_mouvement_employe
    ON historique_mouvement(employe_id);

-- Confirmation administrative du reliquat.
-- Les corrections sont de nouvelles lignes reliées à la confirmation remplacée.
CREATE TABLE IF NOT EXISTS confirmation_reliquat (
    id BIGSERIAL PRIMARY KEY,
    circuit_demande_conge_id BIGINT NOT NULL
        REFERENCES circuit_demande_conge(id),
    employe_id BIGINT NOT NULL REFERENCES employe(id),
    nombre_jours_confirme INTEGER NOT NULL CHECK (nombre_jours_confirme >= 0),
    periode_reference VARCHAR(100) NOT NULL,
    confirme_par BIGINT NOT NULL REFERENCES utilisateur(id),
    date_confirmation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference_confirmation VARCHAR(100) NOT NULL,
    justificatif VARCHAR(500),
    observation VARCHAR(1500),
    remplace_confirmation_id BIGINT REFERENCES confirmation_reliquat(id),
    CONSTRAINT uq_confirmation_reliquat_reference
        UNIQUE (reference_confirmation)
);

CREATE INDEX IF NOT EXISTS idx_confirmation_reliquat_employe
    ON confirmation_reliquat(employe_id);
CREATE INDEX IF NOT EXISTS idx_confirmation_reliquat_dossier
    ON confirmation_reliquat(circuit_demande_conge_id);
