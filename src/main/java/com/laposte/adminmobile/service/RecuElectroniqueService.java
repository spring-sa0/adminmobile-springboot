package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.RecuElectronique;
import com.laposte.adminmobile.repository.RecuElectroniqueRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.RecuElectroniquedao}.
 */
@Service
public class RecuElectroniqueService {

    private final RecuElectroniqueRepository recuElectroniqueRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public RecuElectroniqueService(RecuElectroniqueRepository recuElectroniqueRepository) {
        this.recuElectroniqueRepository = recuElectroniqueRepository;
    }

    public List<RecuElectronique> alloperations() {
        return recuElectroniqueRepository.findAll();
    }

    public List<RecuElectronique> opbyTelephone(String ptelephone) {
        return recuElectroniqueRepository.findByTelephoneOrderByDateOperationDesc(ptelephone);
    }

    public List<RecuElectronique> opbyCompte(Long numerocompte) {
        return recuElectroniqueRepository.findByNumeroCompteOrderByDateOperationDesc(numerocompte);
    }

    /**
     * Port of {@code opbyPeriode(date1, date2)}. NB: the legacy method had a copy/paste bug -
     * {@code .setParameter("date2", date1)} bound date1 twice, so the upper bound was silently
     * ignored. Fixed here to actually use both bounds.
     */
    public List<RecuElectronique> opbyPeriode(Date date1, Date date2) {
        return recuElectroniqueRepository.findByDateOperationBetween(date1, date2);
    }

    public RecuElectronique find(Long id) {
        return recuElectroniqueRepository.findById(id).orElse(null);
    }

    @Transactional
    public RecuElectronique create(RecuElectronique a) {
        return recuElectroniqueRepository.save(a);
    }

    @Transactional
    public void delete(RecuElectronique a) {
        recuElectroniqueRepository.delete(a);
    }

    @Transactional
    public RecuElectronique edit(RecuElectronique a) {
        return recuElectroniqueRepository.save(a);
    }

    @Transactional
    public void refresh(RecuElectronique a) {
        entityManager.refresh(a);
    }

    public List<RecuElectronique> listopBynumeroOperation(Long numeroOperation) {
        return recuElectroniqueRepository.findByNumeroOperation(numeroOperation);
    }

    /**
     * Port of {@code recupListopByTelephone(ptelephone, date1, date2)}. The legacy SQL compared
     * {@code to_char(date_operation,'yyyymmdd')} against day-only bounds; to preserve that
     * "whole days inclusive" semantics against the {@code dateOperation} timestamp column, date1
     * is floored to 00:00:00.000 and date2 is ceiled to 23:59:59.999 before the range query.
     */
    public List<RecuElectronique> recupListopByTelephone(String ptelephone, Date date1, Date date2) {
        return recuElectroniqueRepository.findByTelephoneAndDateOperationBetweenOrderByDateOperationDesc(
                ptelephone, startOfDay(date1), endOfDay(date2));
    }

    /** Port of {@code recupListopByCompe(numerocompte, date1, date2)} - see day-window note above. */
    public List<RecuElectronique> recupListopByCompe(Long numerocompte, Date date1, Date date2) {
        return recuElectroniqueRepository.findByNumeroCompteAndDateOperationBetweenOrderByDateOperationDesc(
                numerocompte, startOfDay(date1), endOfDay(date2));
    }

    /** Port of {@code recupListopBynumop(numeroOperation)} - equivalent to {@link #listopBynumeroOperation}. */
    public List<RecuElectronique> recupListopBynumop(Long numeroOperation) {
        return recuElectroniqueRepository.findByNumeroOperation(numeroOperation);
    }

    public List<RecuElectronique> recupListopByRefid(String refbank) {
        return recuElectroniqueRepository.findByRefbank(refbank);
    }

    /**
     * Port of {@code recupListop(date1, date2, codeop)} - see day-window note above. {@code codeop}
     * is kept as a String to match the legacy signature and parsed to the entity's
     * {@link BigInteger} codeOperation field.
     */
    public List<RecuElectronique> recupListop(Date date1, Date date2, String codeop) {
        return recuElectroniqueRepository.findByDateOperationBetweenAndCodeOperationOrderByDateOperationDesc(
                startOfDay(date1), endOfDay(date2), new BigInteger(codeop));
    }

    /** Port of {@code recupop(numerop)} - equivalent to {@link #listopBynumeroOperation}, ordered. */
    public List<RecuElectronique> recupop(String numerop) {
        return recuElectroniqueRepository.findByNumeroOperationOrderByDateOperationDesc(Long.valueOf(numerop));
    }

    public String recupNomprenomcom(String numcompte) {
        return recuElectroniqueRepository.recupNomprenomcom(numcompte);
    }

    /** Every EcoCCP transaction receipt in a date range - see repository-level notes. */
    public List<RecuElectronique> recupListopParPeriode(Date date1, Date date2) {
        return recuElectroniqueRepository.findByDateOperationBetweenOrderByDateOperationDesc(
                startOfDay(date1), endOfDay(date2));
    }

    /** Same as above, filtered by {@code origine} (operator) when provided. */
    public List<RecuElectronique> recupListopParPeriodeEtOrigine(Date date1, Date date2, String origine) {
        if (origine == null || origine.isBlank()) {
            return recupListopParPeriode(date1, date2);
        }
        return recuElectroniqueRepository.findByDateOperationBetweenAndOrigineOrderByDateOperationDesc(
                startOfDay(date1), endOfDay(date2), origine);
    }

    /** Cancelled EcoCCP operations in a date range - see repository-level notes. */
    public List<RecuElectronique> recupListopAnnulees(Date date1, Date date2) {
        return recuElectroniqueRepository.findByMotifAnnulationIsNotNullAndDateOperationBetweenOrderByDateOperationDesc(
                startOfDay(date1), endOfDay(date2));
    }

    /** Best-faith "Eposte operations" scoping - see repository-level notes. */
    public List<RecuElectronique> recupListopEposte(Date date1, Date date2) {
        return recuElectroniqueRepository.findEposteScopedOperationsBetween(startOfDay(date1), endOfDay(date2));
    }

    /**
     * Port of {@code annulopeco(numop, login, message)}: calls the Oracle stored procedure
     * {@code procedures.annul_op_ecoccp(numop, login, message)} directly, exactly like the legacy
     * {@code CallableStatement} did. Kept as a raw {@link StoredProcedureQuery} rather than a
     * Spring Data {@code @Procedure} because that annotation requires a
     * {@code @NamedStoredProcedureQuery} declared on the entity, and {@link RecuElectronique} is
     * owned by a separate porting effort.
     */
    @Transactional
    public void annulopeco(Double numop, String login, String message) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("procedures.annul_op_ecoccp");
        query.registerStoredProcedureParameter(1, Double.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
        query.setParameter(1, numop);
        query.setParameter(2, login);
        query.setParameter(3, message);
        query.execute();
    }

    /** Port of {@code restaurerop(numop, login)}: calls {@code procedures.restaurer_op_ecoccp(numop, login)}. */
    @Transactional
    public void restaurerop(Double numop, String login) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("procedures.restaurer_op_ecoccp");
        query.registerStoredProcedureParameter(1, Double.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
        query.setParameter(1, numop);
        query.setParameter(2, login);
        query.execute();
    }

    private static Date startOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date endOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }
}
