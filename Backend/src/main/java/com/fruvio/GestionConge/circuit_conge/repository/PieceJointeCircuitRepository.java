package com.fruvio.GestionConge.circuit_conge.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fruvio.GestionConge.circuit_conge.entity.PieceJointeCircuit;

public interface PieceJointeCircuitRepository extends JpaRepository<PieceJointeCircuit, Long> {
    Optional<PieceJointeCircuit> findByCircuitDemandeCongeId(Long dossierId);
}
