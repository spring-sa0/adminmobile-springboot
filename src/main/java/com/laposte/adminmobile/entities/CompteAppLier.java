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
 * Ported from the legacy entity {@code admin.mobile.entities.CompteAppLier}.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>CompteAppLier.findAll - SELECT c FROM CompteAppLier c</li>
 *   <li>CompteAppLier.findByNumeroClient - SELECT c FROM CompteAppLier c WHERE c.numeroClient = :numeroClient</li>
 *   <li>CompteAppLier.findByNumeroCompte - SELECT c FROM CompteAppLier c WHERE c.numeroCompte = :numeroCompte</li>
 *   <li>CompteAppLier.findByNomClient - SELECT c FROM CompteAppLier c WHERE c.nomClient = :nomClient</li>
 *   <li>CompteAppLier.findByPrenomClient - SELECT c FROM CompteAppLier c WHERE c.prenomClient = :prenomClient</li>
 *   <li>CompteAppLier.findByTelephoneClient - SELECT c FROM CompteAppLier c WHERE c.telephoneClient = :telephoneClient</li>
 *   <li>CompteAppLier.findByCodeNature - SELECT c FROM CompteAppLier c WHERE c.codeNature = :codeNature</li>
 *   <li>CompteAppLier.findByOriginelink - SELECT c FROM CompteAppLier c WHERE c.originelink = :originelink</li>
 *   <li>CompteAppLier.findByDateLinkage - SELECT c FROM CompteAppLier c WHERE c.dateLinkage = :dateLinkage</li>
 *   <li>CompteAppLier.findByDateDelinkage - SELECT c FROM CompteAppLier c WHERE c.dateDelinkage = :dateDelinkage</li>
 * </ul>
 */
@Entity
@Table(name = "COMPTE_APP_LIER", catalog = "", schema = "SPT")
public class CompteAppLier implements Serializable {

    private static final long serialVersionUID = 1L;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "NUMERO_CLIENT")
    private BigDecimal numeroClient;
    @Column(name = "NUMERO_COMPTE")
    private BigInteger numeroCompte;
    @Size(max = 60)
    @Column(name = "NOM_CLIENT")
    private String nomClient;
    @Size(max = 60)
    @Column(name = "PRENOM_CLIENT")
    private String prenomClient;
    @Size(max = 12)
    @Column(name = "TELEPHONE_CLIENT")
    private String telephoneClient;
    @Column(name = "CODE_NATURE")
    private BigInteger codeNature;
    @Size(max = 25)
    @Column(name = "ORIGINELINK")
    private String originelink;
    @Column(name = "DATE_LINKAGE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateLinkage;
    @Column(name = "DATE_DELINKAGE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateDelinkage;

    public CompteAppLier() {
    }

    public CompteAppLier(BigDecimal numeroClient) {
        this.numeroClient = numeroClient;
    }

    public BigDecimal getNumeroClient() {
        return numeroClient;
    }

    public void setNumeroClient(BigDecimal numeroClient) {
        this.numeroClient = numeroClient;
    }

    public BigInteger getNumeroCompte() {
        return numeroCompte;
    }

    public void setNumeroCompte(BigInteger numeroCompte) {
        this.numeroCompte = numeroCompte;
    }

    public String getNomClient() {
        return nomClient;
    }

    public void setNomClient(String nomClient) {
        this.nomClient = nomClient;
    }

    public String getPrenomClient() {
        return prenomClient;
    }

    public void setPrenomClient(String prenomClient) {
        this.prenomClient = prenomClient;
    }

    public String getTelephoneClient() {
        return telephoneClient;
    }

    public void setTelephoneClient(String telephoneClient) {
        this.telephoneClient = telephoneClient;
    }

    public BigInteger getCodeNature() {
        return codeNature;
    }

    public void setCodeNature(BigInteger codeNature) {
        this.codeNature = codeNature;
    }

    public String getOriginelink() {
        return originelink;
    }

    public void setOriginelink(String originelink) {
        this.originelink = originelink;
    }

    public Date getDateLinkage() {
        return dateLinkage;
    }

    public void setDateLinkage(Date dateLinkage) {
        this.dateLinkage = dateLinkage;
    }

    public Date getDateDelinkage() {
        return dateDelinkage;
    }

    public void setDateDelinkage(Date dateDelinkage) {
        this.dateDelinkage = dateDelinkage;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (numeroClient != null ? numeroClient.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CompteAppLier)) {
            return false;
        }
        CompteAppLier other = (CompteAppLier) object;
        if ((this.numeroClient == null && other.numeroClient != null) || (this.numeroClient != null && !this.numeroClient.equals(other.numeroClient))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "admin.mobile.entities.CompteAppLier[ numeroClient=" + numeroClient + " ]";
    }

}
