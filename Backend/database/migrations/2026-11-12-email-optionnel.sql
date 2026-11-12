-- Migration requise pour autoriser plusieurs comptes sans adresse e-mail.
-- PostgreSQL conserve la contrainte UNIQUE : les valeurs NULL restent autorisées en multiple.
ALTER TABLE utilisateur
    ALTER COLUMN email DROP NOT NULL;
