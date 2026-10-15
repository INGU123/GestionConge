package com.fruvio.GestionConge.utilisateur.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fruvio.GestionConge.utilisateur.entity.PasswordResetRequestEntity;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequestEntity, Long> {
    List<PasswordResetRequestEntity> findByStatutOrderByDateDemandeDesc(String statut);
}
