package com.laposte.adminmobile.entities;

import java.io.Serializable;
import java.util.Collection;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ported from the legacy entity {@code admin.mobile.entities.EtatReclamation}.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>EtatReclamation.findAll - SELECT e FROM EtatReclamation e</li>
 *   <li>EtatReclamation.findByIdetat - SELECT e FROM EtatReclamation e WHERE e.idetat = :idetat</li>
 *   <li>EtatReclamation.findByLibelleetat - SELECT e FROM EtatReclamation e WHERE e.libelleetat = :libelleetat</li>
 * </ul>
 */
@Entity
@Table(name = "ETAT_RECLAMATION")
public class EtatReclamation implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 5)
    @Column(name = "IDETAT")
    private String idetat;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "LIBELLEETAT")
    private String libelleetat;
    @OneToMany(mappedBy = "idetat")
    private Collection<Reclamations> reclamationsCollection;

    public EtatReclamation() {
    }

    public EtatReclamation(String idetat) {
        this.idetat = idetat;
    }

    public EtatReclamation(String idetat, String libelleetat) {
        this.idetat = idetat;
        this.libelleetat = libelleetat;
    }

    public String getIdetat() {
        return idetat;
    }

    public void setIdetat(String idetat) {
        this.idetat = idetat;
    }

    public String getLibelleetat() {
        return libelleetat;
    }

    public void setLibelleetat(String libelleetat) {
        this.libelleetat = libelleetat;
    }

    public Collection<Reclamations> getReclamationsCollection() {
        return reclamationsCollection;
    }

    public void setReclamationsCollection(Collection<Reclamations> reclamationsCollection) {
        this.reclamationsCollection = reclamationsCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idetat != null ? idetat.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof EtatReclamation)) {
            return false;
        }
        EtatReclamation other = (EtatReclamation) object;
        if ((this.idetat == null && other.idetat != null) || (this.idetat != null && !this.idetat.equals(other.idetat))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "admin.mobile.entities.EtatReclamation[ idetat=" + idetat + " ]";
    }

}
