package com.laposte.adminmobile.entities;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ported from the legacy entity {@code admin.mobile.entities.UsersEposte}.
 *
 * FLAG: this entity maps to the SAME table (USERS_EPOSTE) as {@link Users},
 * under a different Java class name, exactly as in the legacy source - both
 * classes are ported here verbatim rather than merged, since other legacy
 * code may reference either class name. This class carries three extra
 * columns not present on {@link Users} (terminal, appareil, codePasse) and
 * stricter validation on telephone/codeSecret. Decide whether to
 * de-duplicate these two entities before writing repositories against either.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>UsersEposte.findAll - SELECT u FROM UsersEposte u</li>
 *   <li>UsersEposte.findByTelephone - SELECT u FROM UsersEposte u WHERE u.telephone = :telephone</li>
 *   <li>UsersEposte.findByLogin - SELECT u FROM UsersEposte u WHERE u.login = :login</li>
 *   <li>UsersEposte.findByPwd - SELECT u FROM UsersEposte u WHERE u.pwd = :pwd</li>
 *   <li>UsersEposte.findByDateCreation - SELECT u FROM UsersEposte u WHERE u.dateCreation = :dateCreation</li>
 *   <li>UsersEposte.findByActif - SELECT u FROM UsersEposte u WHERE u.actif = :actif</li>
 *   <li>UsersEposte.findByCodeSecret - SELECT u FROM UsersEposte u WHERE u.codeSecret = :codeSecret</li>
 *   <li>UsersEposte.findByOrigine - SELECT u FROM UsersEposte u WHERE u.origine = :origine</li>
 *   <li>UsersEposte.findByNom - SELECT u FROM UsersEposte u WHERE u.nom = :nom</li>
 *   <li>UsersEposte.findByPrenom - SELECT u FROM UsersEposte u WHERE u.prenom = :prenom</li>
 *   <li>UsersEposte.findByDateconnexion - SELECT u FROM UsersEposte u WHERE u.dateconnexion = :dateconnexion</li>
 *   <li>UsersEposte.findByMail - SELECT u FROM UsersEposte u WHERE u.mail = :mail</li>
 *   <li>UsersEposte.findByAdresse - SELECT u FROM UsersEposte u WHERE u.adresse = :adresse</li>
 *   <li>UsersEposte.findByErrCode - SELECT u FROM UsersEposte u WHERE u.errCode = :errCode</li>
 *   <li>UsersEposte.findByNumeroCompte - SELECT u FROM UsersEposte u WHERE u.numeroCompte = :numeroCompte</li>
 *   <li>UsersEposte.findByTerminal - SELECT u FROM UsersEposte u WHERE u.terminal = :terminal</li>
 *   <li>UsersEposte.findByAppareil - SELECT u FROM UsersEposte u WHERE u.appareil = :appareil</li>
 *   <li>UsersEposte.findByCodePasse - SELECT u FROM UsersEposte u WHERE u.codePasse = :codePasse</li>
 * </ul>
 */
@Entity
@Table(name = "USERS_EPOSTE")
public class UsersEposte implements Serializable {

    private static final long serialVersionUID = 1L;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "TELEPHONE")
    private String telephone;
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
    private BigInteger actif;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 25)
    @Column(name = "CODE_SECRET")
    private String codeSecret;
    @Size(max = 12)
    @Column(name = "ORIGINE")
    private String origine;
    @Size(max = 25)
    @Column(name = "NOM")
    private String nom;
    @Size(max = 50)
    @Column(name = "PRENOM")
    private String prenom;
    @Column(name = "DATECONNEXION")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateconnexion;
    @Size(max = 50)
    @Column(name = "MAIL")
    private String mail;
    @Size(max = 50)
    @Column(name = "ADRESSE")
    private String adresse;
    @Column(name = "ERR_CODE")
    private BigInteger errCode;
    @Column(name = "NUMERO_COMPTE")
    private BigInteger numeroCompte;
    @Size(max = 200)
    @Column(name = "TERMINAL")
    private String terminal;
    @Size(max = 200)
    @Column(name = "APPAREIL")
    private String appareil;
    @Column(name = "CODE_PASSE")
    private BigInteger codePasse;
    @JoinColumn(name = "IDPROFIL", referencedColumnName = "IDPROFIL")
    @ManyToOne
    private Profil idprofil;

    public UsersEposte() {
    }

    public UsersEposte(String telephone) {
        this.telephone = telephone;
    }

    public UsersEposte(String telephone, String codeSecret) {
        this.telephone = telephone;
        this.codeSecret = codeSecret;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
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

    public BigInteger getActif() {
        return actif;
    }

    public void setActif(BigInteger actif) {
        this.actif = actif;
    }

    public String getCodeSecret() {
        return codeSecret;
    }

    public void setCodeSecret(String codeSecret) {
        this.codeSecret = codeSecret;
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

    public Date getDateconnexion() {
        return dateconnexion;
    }

    public void setDateconnexion(Date dateconnexion) {
        this.dateconnexion = dateconnexion;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public BigInteger getErrCode() {
        return errCode;
    }

    public void setErrCode(BigInteger errCode) {
        this.errCode = errCode;
    }

    public BigInteger getNumeroCompte() {
        return numeroCompte;
    }

    public void setNumeroCompte(BigInteger numeroCompte) {
        this.numeroCompte = numeroCompte;
    }

    public String getTerminal() {
        return terminal;
    }

    public void setTerminal(String terminal) {
        this.terminal = terminal;
    }

    public String getAppareil() {
        return appareil;
    }

    public void setAppareil(String appareil) {
        this.appareil = appareil;
    }

    public BigInteger getCodePasse() {
        return codePasse;
    }

    public void setCodePasse(BigInteger codePasse) {
        this.codePasse = codePasse;
    }

    public Profil getIdprofil() {
        return idprofil;
    }

    public void setIdprofil(Profil idprofil) {
        this.idprofil = idprofil;
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
        if (!(object instanceof UsersEposte)) {
            return false;
        }
        UsersEposte other = (UsersEposte) object;
        if ((this.telephone == null && other.telephone != null) || (this.telephone != null && !this.telephone.equals(other.telephone))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "admin.mobile.entities.UsersEposte[ telephone=" + telephone + " ]";
    }

}
