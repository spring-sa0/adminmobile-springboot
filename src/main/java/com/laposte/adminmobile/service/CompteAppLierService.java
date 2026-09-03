package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.CompteAppLier;
import com.laposte.adminmobile.repository.CompteAppLierRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Spring Boot port of {@code admin.mobile.dao.compteAppLierdao}.
 * The legacy DAO was read-only (no create/edit/delete/refresh), so this service is too.
 */
@Service
public class CompteAppLierService {

    private final CompteAppLierRepository compteAppLierRepository;

    @Autowired
    public CompteAppLierService(CompteAppLierRepository compteAppLierRepository) {
        this.compteAppLierRepository = compteAppLierRepository;
    }

    public List<CompteAppLier> allcomptelier() {
        return compteAppLierRepository.findAll();
    }

    public List<CompteAppLier> compteByTelpehone(String tel) {
        return compteAppLierRepository.findByTelephoneClient(tel);
    }
}
