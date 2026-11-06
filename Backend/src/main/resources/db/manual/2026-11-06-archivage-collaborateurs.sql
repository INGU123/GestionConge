-- GestionConge - Archivage logique des collaborateurs
-- À exécuter une seule fois sur les bases déployées avec DDL_AUTO=validate.
-- Les bases locales en DDL_AUTO=update créent automatiquement ces colonnes via Hibernate.

ALTER TABLE utilisateur
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS deleted_by BIGINT NULL,
    ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(1000) NULL,
    ADD COLUMN IF NOT EXISTS actif_avant_archivage BOOLEAN NULL,
    ADD COLUMN IF NOT EXISTS archive_email_sent_at TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS historique_compte (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id) ON DELETE RESTRICT,
    acteur_id BIGINT NOT NULL REFERENCES utilisateur(id) ON DELETE RESTRICT,
    action VARCHAR(30) NOT NULL,
    motif VARCHAR(1000) NULL,
    date_action TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_historique_compte_utilisateur_date
    ON historique_compte (utilisateur_id, date_action DESC);
