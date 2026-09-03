package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.Frequences;
import com.laposte.adminmobile.repository.FrequencesRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Spring Boot port of {@code admin.mobile.dao.frequenceDao}.
 */
@Service
public class FrequencesService {

    private final FrequencesRepository frequencesRepository;

    @Autowired
    public FrequencesService(FrequencesRepository frequencesRepository) {
        this.frequencesRepository = frequencesRepository;
    }

    public List<Frequences> allfrequence() {
        return frequencesRepository.findAll();
    }
}
