package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.Operation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.operationdao}.
 */
public interface OperationRepository extends JpaRepository<Operation, Integer> {

    /**
     * Mirrors the entity's {@code Operation.findAll} NamedQuery, which - despite the name -
     * is not a true "find all": it filters on {@code codeApplication IN (24,25)} (the mobile-app
     * operation codes) and orders by label. The legacy {@code operationdao.allOp()} called
     * exactly this NamedQuery.
     */
    @Query("SELECT o FROM Operation o WHERE o.codeApplication IN (24, 25) ORDER BY o.libelleOperation ASC")
    List<Operation> findAllForMobileApplications();

    /**
     * Legacy {@code operationdao.opbyCodeop(String)} called the "Operation.findByCodeOperation"
     * NamedQuery but bound the parameter under the name "telephone" (a copy/paste bug - the
     * NamedQuery declares {@code :codeOperation}), which would have thrown
     * {@code IllegalArgumentException} at runtime. Fixed here to bind the correct parameter.
     */
    List<Operation> findByCodeOperation(Integer codeOperation);

    List<Operation> findByLibelleOperation(String libelleOperation);
}
