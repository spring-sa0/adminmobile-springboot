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
 * Ported from the legacy entity {@code admin.mobile.entities.Formats}.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>Formats.findAll - SELECT f FROM Formats f</li>
 *   <li>Formats.findByIdformat - SELECT f FROM Formats f WHERE f.idformat = :idformat</li>
 *   <li>Formats.findByLibelle - SELECT f FROM Formats f WHERE f.libelle = :libelle</li>
 * </ul>
 */
@Entity
@Table(name = "FORMATS", schema = "IMOBILE")
public class Formats implements Serializable {

    private static final long serialVersionUID = 1L;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "IDFORMAT")
    private BigDecimal idformat;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10)
    @Column(name = "LIBELLE")
    private String libelle;

    public Formats() {
    }

    public Formats(BigDecimal idformat) {
        this.idformat = idformat;
    }

    public Formats(BigDecimal idformat, String libelle) {
        this.idformat = idformat;
        this.libelle = libelle;
    }

    public BigDecimal getIdformat() {
        return idformat;
    }

    public void setIdformat(BigDecimal idformat) {
        this.idformat = idformat;
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
        hash += (idformat != null ? idformat.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Formats)) {
            return false;
        }
        Formats other = (Formats) object;
        if ((this.idformat == null && other.idformat != null) || (this.idformat != null && !this.idformat.equals(other.idformat))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return   libelle ;
    }

}
