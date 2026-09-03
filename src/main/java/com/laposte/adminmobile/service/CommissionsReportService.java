package com.laposte.adminmobile.service;

import com.laposte.adminmobile.repository.CommissionsReportRepository;
import com.laposte.adminmobile.repository.ReportAggregateRow;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Thin service wrapper around {@link CommissionsReportRepository} - see its Javadoc for provenance. */
@Service
public class CommissionsReportService {

    private final CommissionsReportRepository commissionsReportRepository;

    @Autowired
    public CommissionsReportService(CommissionsReportRepository commissionsReportRepository) {
        this.commissionsReportRepository = commissionsReportRepository;
    }

    public List<ReportAggregateRow> commissionsSummary(Date date1, Date date2, String operateur) {
        return commissionsReportRepository.commissionsSummary(date1, date2, operateur);
    }
}
