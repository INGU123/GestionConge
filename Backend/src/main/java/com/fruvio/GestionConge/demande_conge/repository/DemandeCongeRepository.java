package com.fruvio.GestionConge.demande_conge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fruvio.GestionConge.demande_conge.entity.DemandeConge;

@Repository
public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long> {
    List<DemandeConge> findByUtilisateurId(Long utilisateurId);

    boolean existsByUtilisateurId(Long utilisateurId);

    boolean existsByValideePar(Long valideePar);

    List<DemandeConge> findByValideeParAndStatut(Long managerId, String statut);

    List<DemandeConge> findByStatut(String statut);

    List<DemandeConge> findByUtilisateurIdIn(List<Long> utilisateurIds);

    List<DemandeConge> findByUtilisateurIdInAndStatut(List<Long> utilisateurIds, String statut);
}