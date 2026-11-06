package com.fruvio.GestionConge.utilisateur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fruvio.GestionConge.utilisateur.entity.HistoriqueCompte;

@Repository
public interface HistoriqueCompteRepository extends JpaRepository<HistoriqueCompte, Long> {

    List<HistoriqueCompte> findByUtilisateurIdOrderByDateActionDesc(Long utilisateurId);
}
