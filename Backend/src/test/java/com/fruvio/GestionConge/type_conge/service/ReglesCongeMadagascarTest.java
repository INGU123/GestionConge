package com.fruvio.GestionConge.type_conge.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

class ReglesCongeMadagascarTest {

    @Test
    void identifiesAnnualLeaveByCodeAndLabel() {
        assertTrue(ReglesCongeMadagascar.estCongeAnnuel(
                Type_conge.builder().code("CA").libelle("Congé annuel payé").build()));
        assertTrue(ReglesCongeMadagascar.estCongeAnnuel(
                Type_conge.builder().code("AUTRE").libelle("Congé annuel").build()));
        assertFalse(ReglesCongeMadagascar.estCongeAnnuel(
                Type_conge.builder().code("MAL").libelle("Congé maladie").build()));
    }

    @Test
    void annualLeaveCannotStartBeforeTwelveMonthAnniversary() {
        Utilisateur employee = Utilisateur.builder()
                .date_embauche(LocalDate.of(2025, 10, 23))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> ReglesCongeMadagascar.verifierOuvertureDroit(employee, LocalDate.of(2026, 10, 22)));
        assertDoesNotThrow(
                () -> ReglesCongeMadagascar.verifierOuvertureDroit(employee, LocalDate.of(2026, 10, 23)));
    }

    @Test
    void firstFractionWindowLastsThreeMonthsFromOpeningDate() {
        Utilisateur employee = Utilisateur.builder()
                .date_embauche(LocalDate.of(2025, 10, 23))
                .build();

        assertTrue(ReglesCongeMadagascar.estDansFenetrePremiereFraction(employee, LocalDate.of(2026, 10, 23)));
        assertTrue(ReglesCongeMadagascar.estDansFenetrePremiereFraction(employee, LocalDate.of(2027, 1, 22)));
        assertFalse(ReglesCongeMadagascar.estDansFenetrePremiereFraction(employee, LocalDate.of(2027, 1, 23)));
    }
}
