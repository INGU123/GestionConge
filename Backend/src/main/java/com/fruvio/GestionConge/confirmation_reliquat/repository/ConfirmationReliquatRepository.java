package com.fruvio.GestionConge.confirmation_reliquat.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.fruvio.GestionConge.confirmation_reliquat.entity.ConfirmationReliquat;

@Repository
public interface ConfirmationReliquatRepository extends JpaRepository<ConfirmationReliquat, Long> {
    List<ConfirmationReliquat> findByCircuitDemandeCongeIdOrderByDateConfirmationDesc(Long dossierId);
    List<ConfirmationReliquat> findByEmployeIdOrderByDateConfirmationDesc(Long employeId);
    List<ConfirmationReliquat> findByUtilisateurIdOrderByDateConfirmationDesc(Long utilisateurId);
    boolean existsByReferenceConfirmation(String referenceConfirmation);
}
