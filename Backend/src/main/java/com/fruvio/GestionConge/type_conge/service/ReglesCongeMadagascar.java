package com.fruvio.GestionConge.type_conge.service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;

import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

/**
 * Règles communes au congé annuel payé prévues par les articles 125 à 130
 * de la loi malgache n° 2024-014 portant Code du travail.
 *
 * La date d'embauche est utilisée comme approximation de l'ancienneté effective.
 * Les périodes assimilées au service effectif doivent être intégrées si elles
 * sont suivies par l'application.
 */
public final class ReglesCongeMadagascar {

    public static final int JOURS_ANNUELS_PAR_AN = 30;
    public static final int PREMIERE_FRACTION_JOURS = 15;

    private ReglesCongeMadagascar() {
    }

    public static boolean estCongeAnnuel(Type_conge type) {
        if (type == null) {
            return false;
        }
        String code = normaliser(type.getCode());
        String libelle = normaliser(type.getLibelle());
        String regle = normaliser(type.getRegle_acquisition());

        return "CA".equals(code)
                || "CONGE_ANNUEL".equals(code)
                || "CONGE_ANNUEL_PAYE".equals(code)
                || "ANNUEL".equals(code)
                || libelle.contains("conge annuel")
                || libelle.contains("conge paye annuel")
                || "CONGE_ANNUEL_MG".equals(regle);
    }

    public static LocalDate dateOuvertureDroit(Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getDate_embauche() == null) {
            throw new IllegalArgumentException(
                    "La date d'embauche du salarié est obligatoire pour vérifier le droit au congé annuel.");
        }
        return utilisateur.getDate_embauche().plusYears(1);
    }

    public static void verifierOuvertureDroit(Utilisateur utilisateur, LocalDate dateDebut) {
        LocalDate ouverture = dateOuvertureDroit(utilisateur);
        if (dateDebut.isBefore(ouverture)) {
            throw new IllegalArgumentException(
                    "Le congé annuel payé ne peut être pris qu'après 12 mois de service effectif. "
                    + "Date d'ouverture du droit estimée : " + ouverture + ".");
        }
    }

    public static boolean estDansFenetrePremiereFraction(Utilisateur utilisateur, LocalDate dateDebut) {
        LocalDate ouverture = dateOuvertureDroit(utilisateur);
        LocalDate finFenetre = ouverture.plusMonths(3).minusDays(1);
        return !dateDebut.isBefore(ouverture) && !dateDebut.isAfter(finFenetre);
    }

    private static String normaliser(String valeur) {
        if (valeur == null) {
            return "";
        }
        String sansAccents = Normalizer.normalize(valeur, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sansAccents.trim().toLowerCase(Locale.ROOT).replace('-', '_').replaceAll("\\s+", " ");
    }
}
