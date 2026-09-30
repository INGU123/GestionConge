package com.fruvio.GestionConge.solde_conge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fruvio.GestionConge.solde_conge.entity.Solde_conge;

@Repository
public interface Solde_congeRepository extends JpaRepository<Solde_conge, Long> {

    // Récupérer tous les soldes d’un utilisateur
    List<Solde_conge> findByUtilisateurId(Long utilisateurId);

    boolean existsByUtilisateurId(Long utilisateurId);

    // Récupérer tous les soldes d'une liste d'utilisateurs
    List<Solde_conge> findByUtilisateurIdIn(List<Long> utilisateurIds);

    // Récupérer tous les soldes d’un utilisateur pour un type de congé
    List<Solde_conge> findByUtilisateurIdAndTypeCongeId(Long utilisateurId, Long typeCongeId);

    // Récupérer un solde précis pour un utilisateur, un type de congé et une période
    Optional<Solde_conge> findByUtilisateurIdAndTypeCongeIdAndPeriode(Long utilisateurId, Long typeCongeId, int periode);

    // Récupérer tous les soldes d’un utilisateur pour une année/période
    List<Solde_conge> findByUtilisateurIdAndPeriode(Long utilisateurId, int periode);

    // Vérifier si un solde existe déjà pour un utilisateur, un type de congé et une période
    boolean existsByUtilisateurIdAndTypeCongeIdAndPeriode(Long utilisateurId, Long typeCongeId, int periode);
}
