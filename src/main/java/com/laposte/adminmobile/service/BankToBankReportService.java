package com.laposte.adminmobile.service;

import com.laposte.adminmobile.repository.BankToBankReportRepository;
import com.laposte.adminmobile.repository.BankToBankRow;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Thin service wrapper around {@link BankToBankReportRepository} - see its Javadoc for provenance. */
@Service
public class BankToBankReportService {

    private final BankToBankReportRepository bankToBankReportRepository;

    @Autowired
    public BankToBankReportService(BankToBankReportRepository bankToBankReportRepository) {
        this.bankToBankReportRepository = bankToBankReportRepository;
    }

    public List<BankToBankRow> findBankToBank(Date date1, Date date2, String operateur) {
        return bankToBankReportRepository.findBankToBank(date1, date2, operateur);
    }
}
