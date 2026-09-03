package com.laposte.adminmobile.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ported from the legacy entity {@code admin.mobile.entities.Frequences}.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>Frequences.findAll - SELECT f FROM Frequences f</li>
 *   <li>Frequences.findByIdfreq - SELECT f FROM Frequences f WHERE f.idfreq = :idfreq</li>
 *   <li>Frequences.findByLibelle - SELECT f FROM Frequences f WHERE f.libelle = :libelle</li>
 * </ul>
 */
@Entity
@Table(name = "FREQUENCES", schema = "IMOBILE")
public class Frequences implements Serializable {

    private static final long serialVersionUID = 1L;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "IDFREQ")
    private BigDecimal idfreq;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 25)
    @Column(name = "LIBELLE")
    private String libelle;

    public Frequences() {
    }

    public Frequences(BigDecimal idfreq) {
        this.idfreq = idfreq;
    }

    public Frequences(BigDecimal idfreq, String libelle) {
        this.idfreq = idfreq;
        this.libelle = libelle;
    }

    public BigDecimal getIdfreq() {
        return idfreq;
    }

    public void setIdfreq(BigDecimal idfreq) {
        this.idfreq = idfreq;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idfreq != null ? idfreq.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Frequences)) {
            return false;
        }
        Frequences other = (Frequences) object;
        if ((this.idfreq == null && other.idfreq != null) || (this.idfreq != null && !this.idfreq.equals(other.idfreq))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return libelle;
    }

}
