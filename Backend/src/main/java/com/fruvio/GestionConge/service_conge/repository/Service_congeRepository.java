package com.fruvio.GestionConge.service_conge.repository;

import com.fruvio.GestionConge.service_conge.entity.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Service_congeRepository extends JpaRepository<Services, Long> {
    // Tu peux ajouter des méthodes personnalisées si besoin
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Services s WHERE s.responsable_id = :userId")
    boolean existsByResponsableId(@Param("userId") int userId);
}
