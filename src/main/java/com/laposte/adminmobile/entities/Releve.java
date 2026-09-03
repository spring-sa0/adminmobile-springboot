package com.laposte.adminmobile.entities;

import java.io.Serializable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ported from the legacy entity {@code admin.mobile.entities.Releve}.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>Releve.findAll - SELECT r FROM Releve r order by R.idreleve DESC</li>
 *   <li>Releve.findByIdreleve - SELECT r FROM Releve r WHERE r.idreleve = :idreleve</li>
 *   <li>Releve.findByNumerocompte - SELECT r FROM Releve r WHERE r.numerocompte = :numerocompte</li>
 *   <li>Releve.findByJourenvoi - SELECT r FROM Releve r WHERE r.jourenvoi = :jourenvoi</li>
 *   <li>Releve.findByMail - SELECT r FROM Releve r WHERE r.mail = :mail</li>
 *   <li>Releve.findByTypefichier - SELECT r FROM Releve r WHERE r.typefichier = :typefichier</li>
 * </ul>
 * Note: legacy findAll orders by idreleve DESC (not a plain "all rows" query) -
 * preserve this ordering, e.g. findAllByOrderByIdreleveDesc().
 */
@Entity
@Table(name = "RELEVE", schema = "IMOBILE")
@SequenceGenerator(name = "SEQ_RELEVE", schema = "IMOBILE", allocationSize = 1, sequenceName = "SEQ_RELEVE")
public class Releve implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RELEVE")
    @Basic(optional = false)
    @NotNull
    @Column(name = "IDRELEVE")
    private Integer idreleve;
    @Column(name = "NUMEROCOMPTE")
    private Long numerocompte;
    @Size(max = 50)
    @Column(name = "JOURENVOI")
    private String jourenvoi;
    @Size(max = 50)
    @Column(name = "MAIL")
    private String mail;
    @Size(max = 25)
    @Column(name = "TYPEFICHIER")
    private String typefichier;
    @Size(max = 200)
    @Column(name = "INTITULE")
    private String intitule;

    public Releve() {
    }

    public Releve(Integer idreleve) {
        this.idreleve = idreleve;
    }

    public Integer getIdreleve() {
        return idreleve;
    }

    public void setIdreleve(Integer idreleve) {
        this.idreleve = idreleve;
    }

    public Long getNumerocompte() {
        return numerocompte;
    }

    public void setNumerocompte(Long numerocompte) {
        this.numerocompte = numerocompte;
    }

    public String getJourenvoi() {
        return jourenvoi;
    }

    public void setJourenvoi(String jourenvoi) {
        this.jourenvoi = jourenvoi;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getTypefichier() {
        return typefichier;
    }

    public void setTypefichier(String typefichier) {
        this.typefichier = typefichier;
    }

    public String getIntitule() {
        return intitule;
    }

    public void setIntitule(String intitule) {
        this.intitule = intitule;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idreleve != null ? idreleve.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Releve)) {
            return false;
        }
        Releve other = (Releve) object;
        if ((this.idreleve == null && other.idreleve != null) || (this.idreleve != null && !this.idreleve.equals(other.idreleve))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "admin.mobile.entities.Releve[ idreleve=" + idreleve + " ]";
    }

}
