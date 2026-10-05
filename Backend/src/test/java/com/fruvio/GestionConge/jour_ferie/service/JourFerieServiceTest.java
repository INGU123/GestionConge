package com.fruvio.GestionConge.jour_ferie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fruvio.GestionConge.jour_ferie.entity.JourFerie;
import com.fruvio.GestionConge.jour_ferie.repository.JourFerieRepository;

@ExtendWith(MockitoExtension.class)
class JourFerieServiceTest {

    @Mock
    private JourFerieRepository jourFerieRepository;

    @InjectMocks
    private JourFerieService jourFerieService;

    @Test
    void shouldCountFiveWorkdaysFromMondayToFriday() {
        when(jourFerieRepository.findByActifTrueAndDateBetweenOrderByDateAsc(LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 9))).thenReturn(List.of());

        assertEquals(5, jourFerieService.compterJoursOuvres(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 9)));
    }

    @Test
    void shouldCountTwoWorkdaysFromFridayToMonday() {
        when(jourFerieRepository.findByActifTrueAndDateBetweenOrderByDateAsc(LocalDate.of(2026, 1, 9),
                LocalDate.of(2026, 1, 12))).thenReturn(List.of());

        assertEquals(2, jourFerieService.compterJoursOuvres(LocalDate.of(2026, 1, 9), LocalDate.of(2026, 1, 12)));
    }

    @Test
    void shouldExcludeHolidayDates() {
        when(jourFerieRepository.findByActifTrueAndDateBetweenOrderByDateAsc(LocalDate.of(2026, 12, 24),
                LocalDate.of(2026, 12, 28))).thenReturn(List
                        .of(JourFerie.builder().date(LocalDate.of(2026, 12, 25)).libelle("Noël").actif(true).build()));

        assertEquals(2, jourFerieService.compterJoursOuvres(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 28)));
    }

    @Test
    void shouldExcludeMultipleHolidayDates() {
        when(jourFerieRepository.findByActifTrueAndDateBetweenOrderByDateAsc(LocalDate.of(2026, 12, 24),
                LocalDate.of(2026, 12, 31)))
                        .thenReturn(List.of(
                                JourFerie.builder().date(LocalDate.of(2026, 12, 25)).libelle("Noël").actif(true)
                                        .build(),
                                JourFerie.builder().date(LocalDate.of(2026, 12, 31)).libelle("Jour de l'An").actif(true)
                                        .build()));

        assertEquals(4, jourFerieService.compterJoursOuvres(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 31)));
    }
}
