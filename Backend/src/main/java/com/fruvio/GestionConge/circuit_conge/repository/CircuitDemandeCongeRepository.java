package com.fruvio.GestionConge.circuit_conge.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.fruvio.GestionConge.circuit_conge.entity.CircuitDemandeConge;

public interface CircuitDemandeCongeRepository extends JpaRepository<CircuitDemandeConge, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CircuitDemandeConge c where c.id = :id")
    Optional<CircuitDemandeConge> findByIdForUpdate(@Param("id") Long id);
    List<CircuitDemandeConge> findByAssistantIdOrderByDateCreationDesc(Long assistantId);
    List<CircuitDemandeConge> findByUtilisateurId(Long utilisateurId);
    List<CircuitDemandeConge> findByStatutOrderByDateCreationAsc(String statut);
    List<CircuitDemandeConge> findByChefIdAndStatutOrderByDateCreationAsc(Long chefId, String statut);
    List<CircuitDemandeConge> findByDirecteurIdAndStatutOrderByDateCreationAsc(Long directeurId, String statut);
    List<CircuitDemandeConge> findByAdministrationPersonnelIdAndStatutOrderByDateCreationAsc(Long adminId, String statut);
}
