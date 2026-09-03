package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.Releve;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.releveDAo}.
 */
public interface ReleveRepository extends JpaRepository<Releve, Integer> {

    /** Mirrors the entity's {@code Releve.findAll} NamedQuery, which orders by idreleve DESC. */
    @Query("SELECT r FROM Releve r ORDER BY r.idreleve DESC")
    List<Releve> findAllOrderByIdreleveDesc();

    /** Port of {@code releveDAo.getReleveEnreg(numerocompte, jourenvoi, mail, typefichier)}. */
    List<Releve> findByNumerocompteAndJourenvoiAndMailAndTypefichier(
            Long numerocompte, String jourenvoi, String mail, String typefichier);
}
