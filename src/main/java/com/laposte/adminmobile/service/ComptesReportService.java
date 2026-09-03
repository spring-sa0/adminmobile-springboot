package com.laposte.adminmobile.service;

import com.laposte.adminmobile.repository.ComptesReportRepository;
import com.laposte.adminmobile.repository.ComptesReportRepository.CompteRow;
import com.laposte.adminmobile.repository.ComptesReportRepository.CompteSoldeRow;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Thin service wrapper around {@link ComptesReportRepository} - see its Javadoc for provenance. */
@Service
public class ComptesReportService {

    private final ComptesReportRepository comptesReportRepository;

    @Autowired
    public ComptesReportService(ComptesReportRepository comptesReportRepository) {
        this.comptesReportRepository = comptesReportRepository;
    }

    public Long countComptes(Date date1, Date date2) {
        return comptesReportRepository.countComptes(date1, date2);
    }

    public List<CompteRow> listComptes(Date date1, Date date2) {
        return comptesReportRepository.listComptes(date1, date2);
    }

    public List<CompteRow> listComptesActifs(Date date1, Date date2) {
        return comptesReportRepository.listComptesActifs(date1, date2);
    }

    public List<CompteSoldeRow> solde(Date date1, Date date2) {
        return comptesReportRepository.solde(date1, date2);
    }

    public List<CompteSoldeRow> listeSolde(Date date1, Date date2) {
        return comptesReportRepository.listeSolde(date1, date2);
    }

    public List<CompteSoldeRow> comptesNegatifs() {
        return comptesReportRepository.comptesNegatifs();
    }
}
