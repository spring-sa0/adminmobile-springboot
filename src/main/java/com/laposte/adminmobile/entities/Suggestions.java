package com.laposte.adminmobile.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ported from the legacy entity {@code admin.mobile.entities.Suggestions}.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>Suggestions.findAll - SELECT s FROM Suggestions s</li>
 *   <li>Suggestions.findByIdsugest - SELECT s FROM Suggestions s WHERE s.idsugest = :idsugest</li>
 *   <li>Suggestions.findByTelClient - SELECT s FROM Suggestions s WHERE s.telClient = :telClient</li>
 *   <li>Suggestions.findByLibellesugest - SELECT s FROM Suggestions s WHERE s.libellesugest = :libellesugest</li>
 *   <li>Suggestions.findByDatesugest - SELECT s FROM Suggestions s WHERE s.datesugest = :datesugest</li>
 * </ul>
 */
@Entity
@Table(name = "SUGGESTIONS")
@SequenceGenerator(name = "SEQ_ENREG", allocationSize = 1, sequenceName = "SEQ_ENREG")
public class Suggestions implements Serializable {

    private static final long serialVersionUID = 1L;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ENREG")
    @Basic(optional = false)
    @NotNull
    @Column(name = "IDSUGEST")
    private BigDecimal idsugest;
    @Size(max = 25)
    @Column(name = "TEL_CLIENT")
    private String telClient;
    @Size(max = 500)
    @Column(name = "LIBELLESUGEST")
    private String libellesugest;
    @Column(name = "DATESUGEST")
    @Temporal(TemporalType.TIMESTAMP)
    private Date datesugest;

    public Suggestions() {
    }

    public Suggestions(BigDecimal idsugest) {
        this.idsugest = idsugest;
    }

    public BigDecimal getIdsugest() {
        return idsugest;
    }

    public void setIdsugest(BigDecimal idsugest) {
        this.idsugest = idsugest;
    }

    public String getTelClient() {
        return telClient;
    }

    public void setTelClient(String telClient) {
        this.telClient = telClient;
    }

    public String getLibellesugest() {
        return libellesugest;
    }

    public void setLibellesugest(String libellesugest) {
        this.libellesugest = libellesugest;
    }

    public Date getDatesugest() {
        return datesugest;
    }

    public void setDatesugest(Date datesugest) {
        this.datesugest = datesugest;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idsugest != null ? idsugest.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Suggestions)) {
            return false;
        }
        Suggestions other = (Suggestions) object;
        if ((this.idsugest == null && other.idsugest != null) || (this.idsugest != null && !this.idsugest.equals(other.idsugest))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "admin.mobile.entities.Suggestions[ idsugest=" + idsugest + " ]";
    }

}
