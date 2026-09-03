package com.laposte.adminmobile.entities;

import java.io.Serializable;
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
 * Ported from the legacy entity {@code admin.mobile.entities.Operation}.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>Operation.findAll - SELECT o FROM Operation o WHERE o.codeApplication IN (24,25) ORDER BY o.libelleOperation ASC</li>
 *   <li>Operation.findByCodeOperation - SELECT o FROM Operation o WHERE o.codeOperation = :codeOperation</li>
 *   <li>Operation.findByLibelleOperation - SELECT o FROM Operation o WHERE o.libelleOperation = :libelleOperation</li>
 *   <li>Operation.findByDateCreation - SELECT o FROM Operation o WHERE o.dateCreation = :dateCreation</li>
 *   <li>Operation.findByCodeUtilisateur - SELECT o FROM Operation o WHERE o.codeUtilisateur = :codeUtilisateur</li>
 *   <li>Operation.findByCodeProduit - SELECT o FROM Operation o WHERE o.codeProduit = :codeProduit</li>
 *   <li>Operation.findByCodeApplication - SELECT o FROM Operation o WHERE o.codeApplication = :codeApplication</li>
 *   <li>Operation.findByNbreJourValeur - SELECT o FROM Operation o WHERE o.nbreJourValeur = :nbreJourValeur</li>
 *   <li>Operation.findByCodeTypeOperation - SELECT o FROM Operation o WHERE o.codeTypeOperation = :codeTypeOperation</li>
 *   <li>Operation.findByCodeJournal - SELECT o FROM Operation o WHERE o.codeJournal = :codeJournal</li>
 *   <li>Operation.findByCodeTaxe - SELECT o FROM Operation o WHERE o.codeTaxe = :codeTaxe</li>
 * </ul>
 * NOTE: findAll is not a plain "all rows" query - it filters codeApplication IN (24,25)
 * and orders by libelleOperation. Preserve this filter/order semantics when writing
 * the replacement repository method (it is not a true "findAll").
 */
@Entity
@Table(name = "OPERATION", catalog = "", schema = "SPT")
public class Operation implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "CODE_OPERATION")
    private Integer codeOperation;
    @Size(max = 100)
    @Column(name = "LIBELLE_OPERATION")
    private String libelleOperation;
    @Column(name = "DATE_CREATION")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreation;
    @Size(max = 30)
    @Column(name = "CODE_UTILISATEUR")
    private String codeUtilisateur;
    @Column(name = "CODE_PRODUIT")
    private Short codeProduit;
    @Column(name = "CODE_APPLICATION")
    private Short codeApplication;
    @Column(name = "NBRE_JOUR_VALEUR")
    private Short nbreJourValeur;
    @Column(name = "CODE_TYPE_OPERATION")
    private Short codeTypeOperation;
    @Column(name = "CODE_JOURNAL")
    private Short codeJournal;
    @Column(name = "CODE_TAXE")
    private Short codeTaxe;

    public Operation() {
    }

    public Operation(Integer codeOperation) {
        this.codeOperation = codeOperation;
    }

    public Integer getCodeOperation() {
        return codeOperation;
    }

    public void setCodeOperation(Integer codeOperation) {
        this.codeOperation = codeOperation;
    }

    public String getLibelleOperation() {
        return libelleOperation;
    }

    public void setLibelleOperation(String libelleOperation) {
        this.libelleOperation = libelleOperation;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getCodeUtilisateur() {
        return codeUtilisateur;
    }

    public void setCodeUtilisateur(String codeUtilisateur) {
        this.codeUtilisateur = codeUtilisateur;
    }

    public Short getCodeProduit() {
        return codeProduit;
    }

    public void setCodeProduit(Short codeProduit) {
        this.codeProduit = codeProduit;
    }

    public Short getCodeApplication() {
        return codeApplication;
    }

    public void setCodeApplication(Short codeApplication) {
        this.codeApplication = codeApplication;
    }

    public Short getNbreJourValeur() {
        return nbreJourValeur;
    }

    public void setNbreJourValeur(Short nbreJourValeur) {
        this.nbreJourValeur = nbreJourValeur;
    }

    public Short getCodeTypeOperation() {
        return codeTypeOperation;
    }

    public void setCodeTypeOperation(Short codeTypeOperation) {
        this.codeTypeOperation = codeTypeOperation;
    }

    public Short getCodeJournal() {
        return codeJournal;
    }

    public void setCodeJournal(Short codeJournal) {
        this.codeJournal = codeJournal;
    }

    public Short getCodeTaxe() {
        return codeTaxe;
    }

    public void setCodeTaxe(Short codeTaxe) {
        this.codeTaxe = codeTaxe;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (codeOperation != null ? codeOperation.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Operation)) {
            return false;
        }
        Operation other = (Operation) object;
        if ((this.codeOperation == null && other.codeOperation != null) || (this.codeOperation != null && !this.codeOperation.equals(other.codeOperation))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "admin.mobile.entities.Operation[ codeOperation=" + codeOperation + " ]";
    }

}
