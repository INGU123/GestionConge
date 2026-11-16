-- Mise à niveau non destructive du circuit hiérarchique des congés.
-- À exécuter une seule fois sur la base gestionconge avant de relancer avec DDL_AUTO=validate.
ALTER TABLE circuit_demande_conge
    ADD COLUMN IF NOT EXISTS assistant_correction VARCHAR(1500),
    ADD COLUMN IF NOT EXISTS avis_chef VARCHAR(20),
    ADD COLUMN IF NOT EXISTS commentaire_chef VARCHAR(1500);

-- Les valeurs existantes sont conservées. Aucune suppression ni remise à zéro.
