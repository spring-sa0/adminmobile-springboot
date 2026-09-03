package com.laposte.adminmobile.service;

import com.laposte.adminmobile.repository.EposteFabricatedReportRepository;
import com.laposte.adminmobile.repository.EposteFabricatedReportRepository.AchatCreditRow;
import com.laposte.adminmobile.repository.EposteFabricatedReportRepository.ChequierRow;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Thin service wrapper around {@link EposteFabricatedReportRepository} - see its Javadoc for the
 * "unverified schema" flag that applies to both queries exposed here.
 */
@Service
public class EposteFabricatedReportService {

    private final EposteFabricatedReportRepository repository;

    @Autowired
    public EposteFabricatedReportService(EposteFabricatedReportRepository repository) {
        this.repository = repository;
    }

    public List<ChequierRow> chequiers(Date date1, Date date2) {
        return repository.chequiers(date1, date2);
    }

    public List<AchatCreditRow> achatsCredit(Date date1, Date date2) {
        return repository.achatsCredit(date1, date2);
    }
}
