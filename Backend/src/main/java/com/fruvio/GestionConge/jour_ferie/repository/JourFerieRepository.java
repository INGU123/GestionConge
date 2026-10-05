package com.fruvio.GestionConge.jour_ferie.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fruvio.GestionConge.jour_ferie.entity.JourFerie;

@Repository
public interface JourFerieRepository extends JpaRepository<JourFerie, Long> {

    Optional<JourFerie> findByDateAndActifTrue(LocalDate date);

    List<JourFerie> findByActifTrueAndDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);

    List<JourFerie> findByAnneeAndActifTrueOrderByDateAsc(Integer annee);

    List<JourFerie> findByActifTrueOrderByDateAsc();
}
