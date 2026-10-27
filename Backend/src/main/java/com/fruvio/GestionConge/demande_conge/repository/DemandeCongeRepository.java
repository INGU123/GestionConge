package com.fruvio.GestionConge.demande_conge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import com.fruvio.GestionConge.demande_conge.entity.DemandeConge;

@Repository
public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long> {
    // Verrou exclusif pour serialiser validation et annulation d'une demande.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DemandeConge d where d.id = :id")
    Optional<DemandeConge> verrouillerDemande(@Param("id") Long id);

    List<DemandeConge> findByUtilisateurId(Long utilisateurId);

    boolean existsByUtilisateurId(Long utilisateurId);

    boolean existsByValideePar(Long valideePar);

    List<DemandeConge> findByValideeParAndStatut(Long managerId, String statut);

    List<DemandeConge> findByStatut(String statut);

    List<DemandeConge> findByUtilisateurIdIn(List<Long> utilisateurIds);

    List<DemandeConge> findByUtilisateurIdInAndStatut(List<Long> utilisateurIds, String statut);
}