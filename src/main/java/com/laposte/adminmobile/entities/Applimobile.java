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
 * Ported from the legacy entity {@code admin.mobile.entities.Applimobile}.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>Applimobile.findAll - SELECT a FROM Applimobile a</li>
 *   <li>Applimobile.findByIdappli - SELECT a FROM Applimobile a WHERE a.idappli = :idappli</li>
 *   <li>Applimobile.findByLibelleappli - SELECT a FROM Applimobile a WHERE a.libelleappli = :libelleappli</li>
 * </ul>
 */
@Entity
@Table(name = "APPLIMOBILE")
public class Applimobile implements Serializable {

    private static final long serialVersionUID = 1L;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "IDAPPLI")
    private BigDecimal idappli;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 25)
    @Column(name = "LIBELLEAPPLI")
    private String libelleappli;

    public Applimobile() {
    }

    public Applimobile(BigDecimal idappli) {
        this.idappli = idappli;
    }

    public Applimobile(BigDecimal idappli, String libelleappli) {
        this.idappli = idappli;
        this.libelleappli = libelleappli;
    }

    public BigDecimal getIdappli() {
        return idappli;
    }

    public void setIdappli(BigDecimal idappli) {
        this.idappli = idappli;
    }

    public String getLibelleappli() {
        return libelleappli;
    }

    public void setLibelleappli(String libelleappli) {
        this.libelleappli = libelleappli;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idappli != null ? idappli.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Applimobile)) {
            return false;
        }
        Applimobile other = (Applimobile) object;
        if ((this.idappli == null && other.idappli != null) || (this.idappli != null && !this.idappli.equals(other.idappli))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "admin.mobile.entities.Applimobile[ idappli=" + idappli + " ]";
    }

}
