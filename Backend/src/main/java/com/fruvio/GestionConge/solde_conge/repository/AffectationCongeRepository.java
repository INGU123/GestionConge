package com.fruvio.GestionConge.solde_conge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fruvio.GestionConge.solde_conge.entity.AffectationConge;

@Repository
public interface AffectationCongeRepository extends JpaRepository<AffectationConge, Long> {
    List<AffectationConge> findByDemandeCongeId(Long demandeCongeId);
    void deleteByDemandeCongeId(Long demandeCongeId);
}
