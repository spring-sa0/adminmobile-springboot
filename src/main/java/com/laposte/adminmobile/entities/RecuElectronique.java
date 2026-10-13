package com.laposte.adminmobile.entities;

import java.io.Serializable;
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
 * Ported from the legacy entity {@code admin.mobile.entities.RecuElectronique}.
 *
 * Legacy NamedQueries (reimplement as Spring Data repository methods / @Query):
 * <ul>
 *   <li>RecuElectronique.findAll - SELECT r FROM RecuElectronique r</li>
 *   <li>RecuElectronique.findByNumeroOperation - SELECT r FROM RecuElectronique r WHERE r.numeroOperation = :numeroOperation</li>
 *   <li>RecuElectronique.findByRefbank - SELECT r FROM RecuElectronique r WHERE r.refbank = :refbank</li>
 *   <li>RecuElectronique.findByNumeroCompte - SELECT r FROM RecuElectronique r WHERE r.numeroCompte = :numeroCompte order by r.dateOperation desc</li>
 *   <li>RecuElectronique.findByMontant - SELECT r FROM RecuElectronique r WHERE r.montant = :montant</li>
 *   <li>RecuElectronique.findByTelephone - SELECT r FROM RecuElectronique r WHERE r.telephone = :telephone order by r.dateOperation desc</li>
 *   <li>RecuElectronique.findByDateOperation - SELECT r FROM RecuElectronique r WHERE r.dateOperation = :dateOperation</li>
 *   <li>RecuElectronique.findByNumeroDest - SELECT r FROM RecuElectronique r WHERE r.numeroDest = :numeroDest</li>
 *   <li>RecuElectronique.findBySoldeAvant - SELECT r FROM RecuElectronique r WHERE r.soldeAvant = :soldeAvant</li>
 *   <li>RecuElectronique.findBySoldeApres - SELECT r FROM RecuElectronique r WHERE r.soldeApres = :soldeApres</li>
 *   <li>RecuElectronique.findByComOperation - SELECT r FROM RecuElectronique r WHERE r.comOperation = :comOperation</li>
 *   <li>RecuElectronique.findByComSpt - SELECT r FROM RecuElectronique r WHERE r.comSpt = :comSpt</li>
 *   <li>RecuElectronique.findByCodeOperation - SELECT r FROM RecuElectronique r WHERE r.codeOperation = :codeOperation</li>
 *   <li>RecuElectronique.findByLibelleOperation - SELECT r FROM RecuElectronique r WHERE r.libelleOperation = :libelleOperation</li>
 *   <li>RecuElectronique.findByOrigine - SELECT r FROM RecuElectronique r WHERE r.origine = :origine</li>
 * </ul>
 * Note: findByNumeroCompte and findByTelephone order by dateOperation desc -
 * preserve this ordering when replacing them with derived query methods
 * (e.g. findByNumeroCompteOrderByDateOperationDesc).
 */
@Entity
@Table(name = "RECU_ELECTRONIQUE", catalog = "", schema = "SPT")
public class RecuElectronique implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "NUMERO_OPERATION")
    private Long numeroOperation;
    @Size(max = 50)
    @Column(name = "REFBANK")
    private String refbank;
    @Column(name = "NUMERO_COMPTE")
    private Long numeroCompte;
    @Column(name = "MONTANT")
    private Long montant;
    @Size(max = 12)
    @Column(name = "TELEPHONE")
    private String telephone;
    @Column(name = "DATE_OPERATION")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateOperation;
    @Size(max = 25)
    @Column(name = "NUMERO_DEST")
    private String numeroDest;
    @Column(name = "SOLDE_AVANT")
    private BigInteger soldeAvant;
    @Column(name = "SOLDE_APRES")
    private BigInteger soldeApres;
    @Column(name = "COM_OPERATION")
    private BigInteger comOperation;
    @Column(name = "COM_SPT")
    private BigInteger comSpt;
    @Column(name = "CODE_OPERATION")
    private BigInteger codeOperation;
    @Size(max = 100)
    @Column(name = "LIBELLE_OPERATION")
    private String libelleOperation;
    @Size(max = 25)
    @Column(name = "ORIGINE")
    private String origine;
    // Pas de MOTIF_ANNULATION dans SPT.RECU_ELECTRONIQUE (elle n'existe que dans RECU_ELECTRONIQUE_2).
    // Une annulation (PROCEDURES.ANNUL_OP_ECOCCP) insere une contre-ecriture "ANNUL ..." portant
    // ANCIEN_NUM_OP = operation annulee et CODE_UTILISATEUR = qui a annule.
    @Size(max = 50)
    @Column(name = "CODE_UTILISATEUR")
    private String codeUtilisateur;
    @Column(name = "ANCIEN_NUM_OP")
    private Long ancienNumOp;

    public RecuElectronique() {
    }

    public RecuElectronique(Long numeroOperation) {
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

    public String getCodeUtilisateur() {
        return codeUtilisateur;
    }

    public void setCodeUtilisateur(String codeUtilisateur) {
        this.codeUtilisateur = codeUtilisateur;
    }

    public Long getAncienNumOp() {
        return ancienNumOp;
    }

    public void setAncienNumOp(Long ancienNumOp) {
        this.ancienNumOp = ancienNumOp;
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
        if (!(object instanceof RecuElectronique)) {
            return false;
        }
        RecuElectronique other = (RecuElectronique) object;
        if ((this.numeroOperation == null && other.numeroOperation != null) || (this.numeroOperation != null && !this.numeroOperation.equals(other.numeroOperation))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return  numeroOperation.toString();
    }

}
