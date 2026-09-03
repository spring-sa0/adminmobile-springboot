package com.laposte.adminmobile.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

/**
 * Ported from the legacy entity {@code admin.mobile.entities.Profil}.
 *
 * Note: both {@link Users} and {@link UsersEposte} map to the same
 * USERS_EPOSTE table and both declare a @ManyToOne "idprofil" field back to
 * this entity - see the flags on those two classes.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>Profil.findAll - SELECT p FROM Profil p</li>
 *   <li>Profil.findByIdprofil - SELECT p FROM Profil p WHERE p.idprofil = :idprofil</li>
 *   <li>Profil.findByLibelle - SELECT p FROM Profil p WHERE p.libelle = :libelle</li>
 * </ul>
 */
@Entity
@Table(name = "PROFIL")
public class Profil implements Serializable {

    @OneToMany(mappedBy = "idprofil")
    private Collection<UsersEposte> usersEposteCollection;

    private static final long serialVersionUID = 1L;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Id
    @Column(name = "IDPROFIL")
    private BigDecimal idprofil;
    @Size(max = 25)
    @Column(name = "LIBELLE")
    private String libelle;
    @OneToMany(mappedBy = "idprofil")
    private Collection<Users> usersCollection;

    public Profil() {
    }

    public Profil(BigDecimal idprofil) {
        this.idprofil = idprofil;
    }

    public BigDecimal getIdprofil() {
        return idprofil;
    }

    public void setIdprofil(BigDecimal idprofil) {
        this.idprofil = idprofil;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Collection<Users> getUsersCollection() {
        return usersCollection;
    }

    public void setUsersCollection(Collection<Users> usersCollection) {
        this.usersCollection = usersCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idprofil != null ? idprofil.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Profil)) {
            return false;
        }
        Profil other = (Profil) object;
        if ((this.idprofil == null && other.idprofil != null) || (this.idprofil != null && !this.idprofil.equals(other.idprofil))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return   libelle ;
    }

    public Collection<UsersEposte> getUsersEposteCollection() {
        return usersEposteCollection;
    }

    public void setUsersEposteCollection(Collection<UsersEposte> usersEposteCollection) {
        this.usersEposteCollection = usersEposteCollection;
    }

}
