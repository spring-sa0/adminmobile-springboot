package com.laposte.adminmobile.service;

import com.laposte.adminmobile.repository.MarchandReportRepository;
import com.laposte.adminmobile.repository.MarchandReportRepository.MerchantOperationRow;
import com.laposte.adminmobile.repository.ReportAggregateRow;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Thin service wrapper around {@link MarchandReportRepository} and the merchant-account/number
 * lookups on {@link UsersService}/{@link FormatsService} - see {@link MarchandReportRepository}'s
 * Javadoc for provenance.
 */
@Service
public class MarchandReportService {

    private final MarchandReportRepository marchandReportRepository;

    @Autowired
    public MarchandReportService(MarchandReportRepository marchandReportRepository) {
        this.marchandReportRepository = marchandReportRepository;
    }

    /** Shared by /marchand/operations and /marchand/operations-liste. */
    public List<MerchantOperationRow> operations(Date date1, Date date2) {
        return marchandReportRepository.operations(date1, date2);
    }

    public List<ReportAggregateRow> commissions(Date date1, Date date2) {
        return marchandReportRepository.commissions(date1, date2);
    }
}
