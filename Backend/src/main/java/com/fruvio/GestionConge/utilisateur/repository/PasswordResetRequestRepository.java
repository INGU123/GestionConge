package com.fruvio.GestionConge.utilisateur.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fruvio.GestionConge.utilisateur.entity.PasswordResetRequestEntity;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequestEntity, Long> {
    List<PasswordResetRequestEntity> findByStatutOrderByDateDemandeDesc(String statut);
    Optional<PasswordResetRequestEntity> findFirstByUtilisateur_IdAndStatutOrderByDateTraitementDesc(Long utilisateurId, String statut);
}
