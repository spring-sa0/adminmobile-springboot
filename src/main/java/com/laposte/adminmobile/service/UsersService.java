package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.Users;
import com.laposte.adminmobile.repository.UsersRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.usersdao}.
 */
@Service
public class UsersService {

    private final UsersRepository usersRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public UsersService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    /**
     * The legacy {@code allUsers()} actually ran the "UsersEposte.findAll" NamedQuery (not
     * "Users.findAll") even though it returns {@code List<Users>} - both entities map to the same
     * USERS_EPOSTE table with equivalent rows, so {@code Users}' own findAll is behaviourally
     * equivalent and is used here.
     */
    public List<Users> allUsers() {
        return usersRepository.findAll();
    }

    public List<Users> userbyTelephone(String ptelephone) {
        return usersRepository.findByTelephone(ptelephone);
    }

    public Users find(String id) {
        return usersRepository.findById(id).orElse(null);
    }

    @Transactional
    public Users create(Users a) {
        return usersRepository.save(a);
    }

    @Transactional
    public void delete(Users a) {
        usersRepository.delete(a);
    }

    @Transactional
    public Users edit(Users a) {
        return usersRepository.save(a);
    }

    @Transactional
    public void refresh(Users a) {
        entityManager.refresh(a);
    }

    /** Voir {@link UsersRepository#renseignerDateCreation} : 1 si la date a ete renseignee, 0 sinon. */
    @Transactional
    public int renseignerDateCreation(String tel) {
        return usersRepository.renseignerDateCreation(tel);
    }

    @Transactional
    public int reinitPass(String tel, String pass) {
        return usersRepository.reinitPass(tel, pass);
    }

    @Transactional
    public int paramMarchand(String tel, String numcpte) {
        return usersRepository.paramMarchand(tel, numcpte);
    }

    @Transactional
    public int activerMarchand(String tel) {
        return usersRepository.activerMarchand(tel);
    }

    @Transactional
    public int desactiverMarchand(String tel) {
        return usersRepository.desactiverMarchand(tel);
    }

    @Transactional
    public int desactivecompte(String tel) {
        return usersRepository.desactivecompte(tel);
    }

    @Transactional
    public int activecompte(String tel) {
        return usersRepository.activecompte(tel);
    }

    @Transactional
    public int modifProfil(String tel, int newprofil, String numcompte) {
        return usersRepository.modifProfil(tel, newprofil, numcompte);
    }

    public Integer getProfilUtil(String login) {
        return usersRepository.getProfilUtil(login);
    }

    public String getIntitule(String numero) {
        return usersRepository.getIntitule(numero);
    }

    @Transactional
    public int updateEmail(String tel, String email) {
        return usersRepository.updateEmail(tel, email);
    }

    /** Eposte accounts that transacted in the date range - see repository-level notes. */
    public List<Users> usersWithOperationsBetween(Date date1, Date date2) {
        return usersRepository.findUsersWithOperationsBetween(startOfDay(date1), endOfDay(date2));
    }

    /** Eposte accounts that did NOT transact in the date range - see repository-level notes. */
    public List<Users> usersWithoutOperationsBetween(Date date1, Date date2) {
        return usersRepository.findUsersWithoutOperationsBetween(startOfDay(date1), endOfDay(date2));
    }

    /** Port of {@code etatcomptedesa.xhtml} - deactivated Eposte accounts (ACTIF = 2). */
    public List<Users> comptesDesactives() {
        return usersRepository.findByActifOrderByDateCreationDesc(BigInteger.valueOf(2));
    }

    /**
     * Port of {@code souscript.xhtml} - new Eposte subscriptions in the period, optionally
     * narrowed by operateur (origine). {@code "Tout"} and a blank/absent value both mean "no filter".
     */
    public List<Users> souscriptions(Date date1, Date date2, String operateur) {
        if (operateur == null || operateur.isBlank() || "Tout".equalsIgnoreCase(operateur)) {
            return usersRepository.findByDateCreationBetweenOrderByDateCreationDesc(startOfDay(date1), endOfDay(date2));
        }
        return usersRepository.findByDateCreationBetweenAndOrigineOrderByDateCreationDesc(
                startOfDay(date1), endOfDay(date2), operateur);
    }

    /** Merchant ("Marchand") accounts - IDPROFIL = 2. Backs {@code /marchand/comptes}. */
    public List<Users> merchantAccounts() {
        return usersRepository.findMerchantAccounts();
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
