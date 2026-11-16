package com.fruvio.GestionConge.utilisateur.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.fruvio.GestionConge.utilisateur.entity.PasswordResetRequestEntity;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequestEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PasswordResetRequestEntity r where r.id = :id")
    Optional<PasswordResetRequestEntity> findByIdForUpdate(@Param("id") Long id);
    List<PasswordResetRequestEntity> findByStatutOrderByDateDemandeDesc(String statut);
    Optional<PasswordResetRequestEntity> findFirstByUtilisateur_IdAndStatutOrderByDateTraitementDesc(Long utilisateurId, String statut);
}
