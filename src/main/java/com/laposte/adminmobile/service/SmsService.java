package com.laposte.adminmobile.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.smsdao}.
 * <p>
 * {@code IMOBILE.INFO_SMS} is not one of the 18 JPA entities modeled for this port, so - exactly
 * like the legacy DAO - this service talks to it with plain SQL (here via {@link JdbcTemplate}
 * instead of hand-rolled {@code PreparedStatement}s) rather than through a Spring Data repository.
 */
@Service
public class SmsService {

    private static final DateFormat DATE_FORMAT_2 = new SimpleDateFormat("yyyyMMddHHmmss");

    private static final String SQL_SEQ_SMS = "SELECT SPT.SEQ_SMS.NEXTVAL FROM DUAL";

    private static final String SQL_INSERT_SMS =
            "INSERT INTO IMOBILE.INFO_SMS (NUM_SMS, IP_SMS, TELEPHONE_SMS, MESSAGE_SMS, CODE_OPERATION, FLAG, FLAG2) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SmsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Port of {@code smsdao.seq_sms()}: next value of the SPT.SEQ_SMS Oracle sequence. */
    public String seqSms() {
        return jdbcTemplate.queryForObject(SQL_SEQ_SMS, String.class);
    }

    /**
     * Port of {@code smsdao.sendsms(telephone, vContenu)}: inserts a row into IMOBILE.INFO_SMS
     * for the outbound-SMS gateway to pick up. The "228" Togo country-code prefix, the sender IP
     * (41.207.186.71), the CODE_OPERATION value (4001) and FLAG2="0" are reproduced verbatim from
     * the legacy DAO.
     */
    @Transactional
    public void sendsms(String telephone, String vContenu) {
        Date now = new Date();
        String numero = DATE_FORMAT_2.format(now);
        String seq = seqSms();

        jdbcTemplate.update(SQL_INSERT_SMS,
                numero + seq,
                "41.207.186.71",
                "228" + telephone,
                vContenu,
                "4001",
                seq,
                "0");
    }
}
