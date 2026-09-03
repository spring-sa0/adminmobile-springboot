package com.laposte.adminmobile.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ported from the legacy entity {@code admin.mobile.entities.ModifCodesecret}.
 *
 * Note: {@code idprofil} is a raw BigInteger FK value here (not a JPA
 * relation), unlike {@link Users#getIdprofil()} / {@link UsersEposte#getIdprofil()}
 * which map it as a @ManyToOne to {@link Profil}. Kept as-is from the legacy source.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>ModifCodesecret.findAll - SELECT m FROM ModifCodesecret m</li>
 *   <li>ModifCodesecret.findByTelephone - SELECT m FROM ModifCodesecret m WHERE m.telephone = :telephone</li>
 *   <li>ModifCodesecret.findByLogin - SELECT m FROM ModifCodesecret m WHERE m.login = :login</li>
 *   <li>ModifCodesecret.findByPwd - SELECT m FROM ModifCodesecret m WHERE m.pwd = :pwd</li>
 *   <li>ModifCodesecret.findByDateCreation - SELECT m FROM ModifCodesecret m WHERE m.dateCreation = :dateCreation</li>
 *   <li>ModifCodesecret.findByActif - SELECT m FROM ModifCodesecret m WHERE m.actif = :actif</li>
 *   <li>ModifCodesecret.findByCodeSecret - SELECT m FROM ModifCodesecret m WHERE m.codeSecret = :codeSecret</li>
 *   <li>ModifCodesecret.findByIdprofil - SELECT m FROM ModifCodesecret m WHERE m.idprofil = :idprofil</li>
 *   <li>ModifCodesecret.findByOrigine - SELECT m FROM ModifCodesecret m WHERE m.origine = :origine</li>
 *   <li>ModifCodesecret.findByNom - SELECT m FROM ModifCodesecret m WHERE m.nom = :nom</li>
 *   <li>ModifCodesecret.findByPrenom - SELECT m FROM ModifCodesecret m WHERE m.prenom = :prenom</li>
 *   <li>ModifCodesecret.findByDateetheure - SELECT m FROM ModifCodesecret m WHERE m.dateetheure = :dateetheure</li>
 *   <li>ModifCodesecret.findByUtilisateur - SELECT m FROM ModifCodesecret m WHERE m.utilisateur = :utilisateur</li>
 * </ul>
 */
@Entity
@Table(name = "MODIF_CODESECRET")
public class ModifCodesecret implements Serializable {

    private static final long serialVersionUID = 1L;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "TELEPHONE")
    private BigDecimal telephone;
    @Size(max = 25)
    @Column(name = "LOGIN")
    private String login;
    @Size(max = 25)
    @Column(name = "PWD")
    private String pwd;
    @Column(name = "DATE_CREATION")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreation;
    @Column(name = "ACTIF")
    private Integer actif;
    @Basic(optional = false)
    @Size(min = 1, max = 25)
    @Column(name = "CODE_SECRET")
    private String codeSecret;
    @Column(name = "IDPROFIL")
    private BigInteger idprofil;
    @Size(max = 12)
    @Column(name = "ORIGINE")
    private String origine;
    @Size(max = 25)
    @Column(name = "NOM")
    private String nom;
    @Size(max = 50)
    @Column(name = "PRENOM")
    private String prenom;
    @Column(name = "DATEETHEURE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateetheure;
    @Size(max = 50)
    @Column(name = "UTILISATEUR")
    private String utilisateur;
    @Size(max = 25)
    @Column(name = "MOTIF")
    private String motif;

    public ModifCodesecret() {
    }

    public ModifCodesecret(BigDecimal telephone) {
        this.telephone = telephone;
    }

    public ModifCodesecret(BigDecimal telephone, String codeSecret) {
        this.telephone = telephone;
        this.codeSecret = codeSecret;
    }

    public BigDecimal getTelephone() {
        return telephone;
    }

    public void setTelephone(BigDecimal telephone) {
        this.telephone = telephone;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Integer getActif() {
        return actif;
    }

    public void setActif(Integer actif) {
        this.actif = actif;
    }

    public String getCodeSecret() {
        return codeSecret;
    }

    public void setCodeSecret(String codeSecret) {
        this.codeSecret = codeSecret;
    }

    public BigInteger getIdprofil() {
        return idprofil;
    }

    public void setIdprofil(BigInteger idprofil) {
        this.idprofil = idprofil;
    }

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public Date getDateetheure() {
        return dateetheure;
    }

    public void setDateetheure(Date dateetheure) {
        this.dateetheure = dateetheure;
    }

    public String getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(String utilisateur) {
        this.utilisateur = utilisateur;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (telephone != null ? telephone.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ModifCodesecret)) {
            return false;
        }
        ModifCodesecret other = (ModifCodesecret) object;
        if ((this.telephone == null && other.telephone != null) || (this.telephone != null && !this.telephone.equals(other.telephone))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "admin.mobile.entities.ModifCodesecret[ telephone=" + telephone + " ]";
    }

}
