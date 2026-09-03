package com.laposte.adminmobile.service;

import com.laposte.adminmobile.repository.RecapReportRepository;
import com.laposte.adminmobile.repository.ReportAggregateRow;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Thin service wrapper around {@link RecapReportRepository} - see its Javadoc for provenance. */
@Service
public class RecapReportService {

    private final RecapReportRepository recapReportRepository;

    @Autowired
    public RecapReportService(RecapReportRepository recapReportRepository) {
        this.recapReportRepository = recapReportRepository;
    }

    public List<ReportAggregateRow> recapParClient(Date date1, Date date2) {
        return recapReportRepository.recapParClient(date1, date2);
    }

    public List<ReportAggregateRow> recapParCompte(Date date1, Date date2) {
        return recapReportRepository.recapParCompte(date1, date2);
    }

    public List<ReportAggregateRow> recapParProduit(Date date1, Date date2) {
        return recapReportRepository.recapParProduit(date1, date2);
    }

    public List<ReportAggregateRow> recapSouscriptions(Date date1, Date date2) {
        return recapReportRepository.recapSouscriptions(date1, date2);
    }
}
