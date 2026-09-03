package com.laposte.adminmobile.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ported from the legacy entity {@code admin.mobile.entities.Opmobile}.
 *
 * Note: {@code idappli} and {@code codeOperation} are raw FK values (BigInteger),
 * not JPA relations to {@link Applimobile} / {@link Operation}. Kept as-is from
 * the legacy source.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>Opmobile.findAll - SELECT o FROM Opmobile o</li>
 *   <li>Opmobile.findByIdmobile - SELECT o FROM Opmobile o WHERE o.idmobile = :idmobile</li>
 *   <li>Opmobile.findByLibellemobile - SELECT o FROM Opmobile o WHERE o.libellemobile = :libellemobile</li>
 *   <li>Opmobile.findByIdappli - SELECT o FROM Opmobile o WHERE o.idappli = :idappli</li>
 *   <li>Opmobile.findByCodeOperation - SELECT o FROM Opmobile o WHERE o.codeOperation = :codeOperation</li>
 * </ul>
 */
@Entity
@Table(name = "OPMOBILE")
public class Opmobile implements Serializable {

    private static final long serialVersionUID = 1L;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "IDMOBILE")
    private BigDecimal idmobile;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 25)
    @Column(name = "LIBELLEMOBILE")
    private String libellemobile;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IDAPPLI")
    private BigInteger idappli;
    @Column(name = "CODE_OPERATION")
    private BigInteger codeOperation;

    public Opmobile() {
    }

    public Opmobile(BigDecimal idmobile) {
        this.idmobile = idmobile;
    }

    public Opmobile(BigDecimal idmobile, String libellemobile, BigInteger idappli) {
        this.idmobile = idmobile;
        this.libellemobile = libellemobile;
        this.idappli = idappli;
    }

    public BigDecimal getIdmobile() {
        return idmobile;
    }

    public void setIdmobile(BigDecimal idmobile) {
        this.idmobile = idmobile;
    }

    public String getLibellemobile() {
        return libellemobile;
    }

    public void setLibellemobile(String libellemobile) {
        this.libellemobile = libellemobile;
    }

    public BigInteger getIdappli() {
        return idappli;
    }

    public void setIdappli(BigInteger idappli) {
        this.idappli = idappli;
    }

    public BigInteger getCodeOperation() {
        return codeOperation;
    }

    public void setCodeOperation(BigInteger codeOperation) {
        this.codeOperation = codeOperation;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idmobile != null ? idmobile.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Opmobile)) {
            return false;
        }
        Opmobile other = (Opmobile) object;
        if ((this.idmobile == null && other.idmobile != null) || (this.idmobile != null && !this.idmobile.equals(other.idmobile))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "admin.mobile.entities.Opmobile[ idmobile=" + idmobile + " ]";
    }

}
