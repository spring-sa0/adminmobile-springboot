package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.Formats;
import com.laposte.adminmobile.repository.FormatsRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Spring Boot port of {@code admin.mobile.dao.formatDao}.
 * <p>
 * {@link #getIntitule(String)} does not concern the {@link Formats} entity at all - it is a
 * verbatim copy-paste (present identically in the legacy {@code usersdao}) of a lookup against
 * {@code SPT.COMPTE}, a table not modeled as a JPA entity in this port. It is kept here as a
 * plain JDBC passthrough for fidelity with the original DAO's public surface.
 */
@Service
public class FormatsService {

    private static final String SQL_INTITULE = "SELECT INTITULE FROM SPT.COMPTE WHERE NUMERO_COMPTE = ?";

    private final FormatsRepository formatsRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FormatsService(FormatsRepository formatsRepository, JdbcTemplate jdbcTemplate) {
        this.formatsRepository = formatsRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Formats> allformats() {
        return formatsRepository.findAll();
    }

    /** See class javadoc: unrelated to Formats, kept only because the legacy DAO exposed it. */
    public String getIntitule(String numero) {
        try {
            return jdbcTemplate.queryForObject(SQL_INTITULE, String.class, numero);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
