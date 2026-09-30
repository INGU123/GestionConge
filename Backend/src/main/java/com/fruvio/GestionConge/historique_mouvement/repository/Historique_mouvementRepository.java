package com.fruvio.GestionConge.historique_mouvement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fruvio.GestionConge.historique_mouvement.entity.Historique_mouvement;

@Repository
public interface Historique_mouvementRepository extends JpaRepository<Historique_mouvement, Long> {
    List<Historique_mouvement> findByUtilisateurId(Long utilisateurId);

    boolean existsByUtilisateurId(Long utilisateurId);

    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM Historique_mouvement h WHERE h.effectue_par = :userId")
    boolean existsByActorId(@Param("userId") Long userId);

    List<Historique_mouvement> findByUtilisateurIdIn(List<Long> utilisateurIds);
}
