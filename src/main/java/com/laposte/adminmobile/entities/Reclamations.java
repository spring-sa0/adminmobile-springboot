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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ported from the legacy entity {@code admin.mobile.entities.Reclamations}.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>Reclamations.findAll - SELECT r FROM Reclamations r</li>
 *   <li>Reclamations.findByIdreclamation - SELECT r FROM Reclamations r WHERE r.idreclamation = :idreclamation</li>
 *   <li>Reclamations.findByTelClient - SELECT r FROM Reclamations r WHERE r.telClient = :telClient</li>
 *   <li>Reclamations.findByMotifreclamatin - SELECT r FROM Reclamations r WHERE r.motifreclamatin = :motifreclamatin</li>
 *   <li>Reclamations.findByDatereclamation - SELECT r FROM Reclamations r WHERE r.datereclamation = :datereclamation</li>
 * </ul>
 */
@Entity
@Table(name = "RECLAMATIONS")
@SequenceGenerator(name = "SEQ_ENREG", allocationSize = 1, sequenceName = "SEQ_ENREG")
public class Reclamations implements Serializable {

    @Column(name = "DATEAFFECT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateaffect;
    @Column(name = "DATECLOTURE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date datecloture;

    @JoinColumn(name = "IDETAT", referencedColumnName = "IDETAT")
    @ManyToOne
    private EtatReclamation idetat;
    @JoinColumn(name = "IDSUPPORT", referencedColumnName = "IDSUPPORT")
    @ManyToOne
    private Support idsupport;
    @JoinColumn(name = "IDTYPE", referencedColumnName = "IDTYPE")
    @ManyToOne
    private TypeReclammation idtype;

    @Size(max = 200)
    @Column(name = "REMARQUES")
    private String remarques;

    private static final long serialVersionUID = 1L;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ENREG")
    @Basic(optional = false)
    @NotNull
    @Column(name = "IDRECLAMATION")
    private BigDecimal idreclamation;
    @Size(max = 25)
    @Column(name = "TEL_CLIENT")
    private String telClient;
    @Size(max = 500)
    @Column(name = "MOTIFRECLAMATIN")
    private String motifreclamatin;
    @Column(name = "DATERECLAMATION")
    @Temporal(TemporalType.TIMESTAMP)
    private Date datereclamation;

    public Reclamations() {
    }

    public Reclamations(BigDecimal idreclamation) {
        this.idreclamation = idreclamation;
    }

    public BigDecimal getIdreclamation() {
        return idreclamation;
    }

    public void setIdreclamation(BigDecimal idreclamation) {
        this.idreclamation = idreclamation;
    }

    public String getTelClient() {
        return telClient;
    }

    public void setTelClient(String telClient) {
        this.telClient = telClient;
    }

    public String getMotifreclamatin() {
        return motifreclamatin;
    }

    public void setMotifreclamatin(String motifreclamatin) {
        this.motifreclamatin = motifreclamatin;
    }

    public Date getDatereclamation() {
        return datereclamation;
    }

    public void setDatereclamation(Date datereclamation) {
        this.datereclamation = datereclamation;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idreclamation != null ? idreclamation.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Reclamations)) {
            return false;
        }
        Reclamations other = (Reclamations) object;
        if ((this.idreclamation == null && other.idreclamation != null) || (this.idreclamation != null && !this.idreclamation.equals(other.idreclamation))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "admin.mobile.entities.Reclamations[ idreclamation=" + idreclamation + " ]";
    }


    public String getRemarques() {
        return remarques;
    }

    public void setRemarques(String remarques) {
        this.remarques = remarques;
    }

    public EtatReclamation getIdetat() {
        return idetat;
    }

    public void setIdetat(EtatReclamation idetat) {
        this.idetat = idetat;
    }

    public Support getIdsupport() {
        return idsupport;
    }

    public void setIdsupport(Support idsupport) {
        this.idsupport = idsupport;
    }

    public TypeReclammation getIdtype() {
        return idtype;
    }

    public void setIdtype(TypeReclammation idtype) {
        this.idtype = idtype;
    }

    public Date getDateaffect() {
        return dateaffect;
    }

    public void setDateaffect(Date dateaffect) {
        this.dateaffect = dateaffect;
    }

    public Date getDatecloture() {
        return datecloture;
    }

    public void setDatecloture(Date datecloture) {
        this.datecloture = datecloture;
    }

}
