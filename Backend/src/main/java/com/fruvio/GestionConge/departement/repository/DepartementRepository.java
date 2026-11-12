package com.fruvio.GestionConge.departement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fruvio.GestionConge.departement.entity.Departement;

public interface DepartementRepository extends JpaRepository<Departement, Long> {
    Optional<Departement> findByChefIdAndActifTrue(Long chefId);
    Optional<Departement> findByDirecteurIdAndActifTrue(Long directeurId);
    List<Departement> findByActifTrueOrderByNomAsc();
    boolean existsByNomIgnoreCase(String nom);
}
