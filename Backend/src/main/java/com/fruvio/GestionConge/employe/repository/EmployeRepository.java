package com.fruvio.GestionConge.employe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fruvio.GestionConge.employe.entity.Employe;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Long> {
    Optional<Employe> findByMatricule(String matricule);
    boolean existsByMatricule(String matricule);
    boolean existsByMatriculeAndIdNot(String matricule, Long id);
    List<Employe> findAllByOrderByNomAscPrenomAsc();
}
