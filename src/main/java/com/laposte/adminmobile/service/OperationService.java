package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.Operation;
import com.laposte.adminmobile.repository.OperationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.operationdao}.
 */
@Service
public class OperationService {

    private final OperationRepository operationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public OperationService(OperationRepository operationRepository) {
        this.operationRepository = operationRepository;
    }

    public List<Operation> allOp() {
        return operationRepository.findAllForMobileApplications();
    }

    /** codeop kept as String to match the legacy DAO's signature; the id field itself is an Integer. */
    public List<Operation> opbyCodeop(String codeop) {
        return operationRepository.findByCodeOperation(Integer.valueOf(codeop));
    }

    public Operation find(Integer id) {
        return operationRepository.findById(id).orElse(null);
    }

    @Transactional
    public Operation create(Operation a) {
        return operationRepository.save(a);
    }

    @Transactional
    public void delete(Operation a) {
        operationRepository.delete(a);
    }

    @Transactional
    public Operation edit(Operation a) {
        return operationRepository.save(a);
    }

    @Transactional
    public void refresh(Operation a) {
        entityManager.refresh(a);
    }

    public List<Operation> getLibelleop(String libelleop) {
        return operationRepository.findByLibelleOperation(libelleop);
    }
}
