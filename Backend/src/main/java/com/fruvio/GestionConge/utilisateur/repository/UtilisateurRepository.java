package com.fruvio.GestionConge.utilisateur.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByMatricule(String matricule);

    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByResetToken(String resetToken);

    boolean existsByMatricule(String matricule);

    boolean existsByEmail(String email);

    boolean existsByNumeroCinIgnoreCase(String numeroCin);

    @Query("SELECT u FROM Utilisateur u WHERE u.manager_id = :managerId")
    List<Utilisateur> findByManagerId(@Param("managerId") Long managerId);

    @Query("SELECT u FROM Utilisateur u WHERE u.service_id = :serviceId")
    List<Utilisateur> findByServiceId(@Param("serviceId") Long serviceId);
}