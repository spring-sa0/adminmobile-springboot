package com.laposte.adminmobile.entities;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

/**
 * Ported from the legacy class {@code admin.mobile.entities.Recu}.
 *
 * NOTE: unlike the other 17 classes in this package, the legacy source for
 * this class is NOT a JPA entity - it carries no {@code @Entity}, no
 * {@code @Table} and no {@code @Id} annotation despite importing
 * {@code javax.persistence.*} (those imports were unused in the original and
 * have been dropped here). It looks like a plain transfer/report object (a
 * receipt / "recu" printout), most likely superseded by {@link RecuElectronique}
 * which IS a real entity mapped to RECU_ELECTRONIQUE. Ported field-for-field,
 * unchanged, including its no-relation, no-persistence nature - do not add
 * {@code @Entity}/{@code @Table} without confirming with the schema owner.
 * Its {@code toString()} also has a pre-existing legacy bug: it hardcodes the
 * label "RecuElectronique" instead of "Recu" - kept verbatim.
 *
 * No legacy NamedQueries existed on this class (it is not a JPA entity).
 */
public class Recu implements Serializable {

    private Long numeroOperation;
    private String refbank;
    private Long numeroCompte;
    private Long montant;
    private String telephone;
    private Date dateOperation;
    private String numeroDest;
    private BigInteger soldeAvant;
    private BigInteger soldeApres;
    private BigInteger comOperation;
    private BigInteger comSpt;
    private BigInteger codeOperation;
    private String libelleOperation;
    private String origine;

    public Recu() {
    }

    public Recu(Long numeroOperation) {
        this.numeroOperation = numeroOperation;
    }

    public Long getNumeroOperation() {
        return numeroOperation;
    }

    public void setNumeroOperation(Long numeroOperation) {
        this.numeroOperation = numeroOperation;
    }

    public String getRefbank() {
        return refbank;
    }

    public void setRefbank(String refbank) {
        this.refbank = refbank;
    }

    public Long getNumeroCompte() {
        return numeroCompte;
    }

    public void setNumeroCompte(Long numeroCompte) {
        this.numeroCompte = numeroCompte;
    }

    public Long getMontant() {
        return montant;
    }

    public void setMontant(Long montant) {
        this.montant = montant;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Date getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(Date dateOperation) {
        this.dateOperation = dateOperation;
    }

    public String getNumeroDest() {
        return numeroDest;
    }

    public void setNumeroDest(String numeroDest) {
        this.numeroDest = numeroDest;
    }

    public BigInteger getSoldeAvant() {
        return soldeAvant;
    }

    public void setSoldeAvant(BigInteger soldeAvant) {
        this.soldeAvant = soldeAvant;
    }

    public BigInteger getSoldeApres() {
        return soldeApres;
    }

    public void setSoldeApres(BigInteger soldeApres) {
        this.soldeApres = soldeApres;
    }

    public BigInteger getComOperation() {
        return comOperation;
    }

    public void setComOperation(BigInteger comOperation) {
        this.comOperation = comOperation;
    }

    public BigInteger getComSpt() {
        return comSpt;
    }

    public void setComSpt(BigInteger comSpt) {
        this.comSpt = comSpt;
    }

    public BigInteger getCodeOperation() {
        return codeOperation;
    }

    public void setCodeOperation(BigInteger codeOperation) {
        this.codeOperation = codeOperation;
    }

    public String getLibelleOperation() {
        return libelleOperation;
    }

    public void setLibelleOperation(String libelleOperation) {
        this.libelleOperation = libelleOperation;
    }

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (numeroOperation != null ? numeroOperation.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Recu)) {
            return false;
        }
        Recu other = (Recu) object;
        if ((this.numeroOperation == null && other.numeroOperation != null) || (this.numeroOperation != null && !this.numeroOperation.equals(other.numeroOperation))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "admin.mobile.entities.RecuElectronique[ numeroOperation=" + numeroOperation + " ]";
    }

}
