package com.fruvio.GestionConge.jour_ferie.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fruvio.GestionConge.jour_ferie.entity.JourFerie;
import com.fruvio.GestionConge.jour_ferie.repository.JourFerieRepository;

@Service
public class JourFerieService {

    private final JourFerieRepository jourFerieRepository;

    public JourFerieService(JourFerieRepository jourFerieRepository) {
        this.jourFerieRepository = jourFerieRepository;
    }

    public boolean estJourOuvre(LocalDate date) {
        if (date == null) {
            return false;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    public boolean estJourFerie(LocalDate date) {
        if (date == null) {
            return false;
        }
        return jourFerieRepository.findByDateAndActifTrue(date).isPresent();
    }

    public List<JourFerie> getJoursFeriesActifs() {
        return jourFerieRepository.findByActifTrueOrderByDateAsc();
    }

    public List<JourFerie> getJoursFeriesParAnnee(int annee) {
        return jourFerieRepository.findByAnneeAndActifTrueOrderByDateAsc(annee);
    }

    public int compterJoursOuvres(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null || dateFin.isBefore(dateDebut)) {
            return 0;
        }

        List<JourFerie> joursFeries = jourFerieRepository.findByActifTrueAndDateBetweenOrderByDateAsc(dateDebut,
                dateFin);
        Set<LocalDate> datesFeriees = joursFeries.stream().map(JourFerie::getDate).collect(Collectors.toSet());

        int count = 0;
        LocalDate current = dateDebut;
        while (!current.isAfter(dateFin)) {
            if (estJourOuvre(current) && !datesFeriees.contains(current)) {
                count++;
            }
            current = current.plusDays(1);
        }

        return count;
    }
}
