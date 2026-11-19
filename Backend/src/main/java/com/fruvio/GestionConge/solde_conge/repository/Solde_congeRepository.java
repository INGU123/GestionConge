package com.fruvio.GestionConge.solde_conge.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;

@Repository
public interface Solde_congeRepository extends JpaRepository<Solde_conge, Long> {

    // Compatibilité des soldes historiques liés à un compte utilisateur.
    List<Solde_conge> findByUtilisateurId(Long utilisateurId);
    boolean existsByUtilisateurId(Long utilisateurId);
    List<Solde_conge> findByUtilisateurIdIn(List<Long> utilisateurIds);
    List<Solde_conge> findByUtilisateurIdAndTypeCongeId(Long utilisateurId, Long typeCongeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Solde_conge> findByUtilisateurIdAndTypeCongeIdAndPeriode(Long utilisateurId, Long typeCongeId, int periode);

    List<Solde_conge> findByUtilisateurIdAndPeriode(Long utilisateurId, int periode);
    boolean existsByUtilisateurIdAndTypeCongeIdAndPeriode(Long utilisateurId, Long typeCongeId, int periode);

    // Nouveau référentiel : soldes rattachés directement à un employé sans compte de connexion.
    List<Solde_conge> findByEmployeId(Long employeId);
    List<Solde_conge> findByEmployeIdAndPeriode(Long employeId, int periode);
    List<Solde_conge> findByEmployeIdAndTypeCongeId(Long employeId, Long typeCongeId);
    boolean existsByEmployeIdAndTypeCongeIdAndPeriode(Long employeId, Long typeCongeId, int periode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Solde_conge> findByEmployeIdAndTypeCongeIdAndPeriode(Long employeId, Long typeCongeId, int periode);
}
