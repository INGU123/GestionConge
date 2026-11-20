-- Stockage des soldes du référentiel employé en demi-journées.
-- Les lignes existantes restent en jours entiers (FALSE) pour préserver les données historiques.
ALTER TABLE solde_conge
    ADD COLUMN IF NOT EXISTS en_demi_journees BOOLEAN NOT NULL DEFAULT FALSE;
