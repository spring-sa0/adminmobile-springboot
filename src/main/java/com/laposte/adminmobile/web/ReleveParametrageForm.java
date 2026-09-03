package com.laposte.adminmobile.web;

import java.util.ArrayList;
import java.util.List;

/**
 * Form backing bean for GET/POST /releves/parametrage, porting the checkbox-matrix screen
 * web/releveauto_new.xhtml (admin.mobile.ctrl.ReleveCtrl: journalier/hebdomadaire/quizaine/
 * mensuel/trimestre/semestre x pdf/xls/afb120/csv, plus a free-text comma-separated mail list).
 */
public class ReleveParametrageForm {

    private String numerocompte;
    private String email;

    private boolean journalier;
    private boolean hebdomadaire;
    private boolean quizaine;
    private boolean mensuel;
    private boolean trimestre;
    private boolean semestre;

    private boolean pdf;
    private boolean xls;
    private boolean afb120;
    private boolean csv;

    /** Legacy jourenvoi values (see ReleveCtrl.enrege()), in the same order as the form's checkboxes. */
    public List<String> selectedFrequences() {
        List<String> list = new ArrayList<>();
        if (journalier) list.add("journalier");
        if (hebdomadaire) list.add("hebdomadaire");
        if (quizaine) list.add("quizaine");
        if (mensuel) list.add("mensuel");
        if (trimestre) list.add("trimestre");
        if (semestre) list.add("semestre");
        return list;
    }

    /** Legacy typefichier values (see ReleveCtrl.enrege()), in the same order as the form's checkboxes. */
    public List<String> selectedFormats() {
        List<String> list = new ArrayList<>();
        if (pdf) list.add("pdf");
        if (xls) list.add("xls");
        if (afb120) list.add("afb120");
        if (csv) list.add("csv");
        return list;
    }

    public String getNumerocompte() {
        return numerocompte;
    }

    public void setNumerocompte(String numerocompte) {
        this.numerocompte = numerocompte;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isJournalier() {
        return journalier;
    }

    public void setJournalier(boolean journalier) {
        this.journalier = journalier;
    }

    public boolean isHebdomadaire() {
        return hebdomadaire;
    }

    public void setHebdomadaire(boolean hebdomadaire) {
        this.hebdomadaire = hebdomadaire;
    }

    public boolean isQuizaine() {
        return quizaine;
    }

    public void setQuizaine(boolean quizaine) {
        this.quizaine = quizaine;
    }

    public boolean isMensuel() {
        return mensuel;
    }

    public void setMensuel(boolean mensuel) {
        this.mensuel = mensuel;
    }

    public boolean isTrimestre() {
        return trimestre;
    }

    public void setTrimestre(boolean trimestre) {
        this.trimestre = trimestre;
    }

    public boolean isSemestre() {
        return semestre;
    }

    public void setSemestre(boolean semestre) {
        this.semestre = semestre;
    }

    public boolean isPdf() {
        return pdf;
    }

    public void setPdf(boolean pdf) {
        this.pdf = pdf;
    }

    public boolean isXls() {
        return xls;
    }

    public void setXls(boolean xls) {
        this.xls = xls;
    }

    public boolean isAfb120() {
        return afb120;
    }

    public void setAfb120(boolean afb120) {
        this.afb120 = afb120;
    }

    public boolean isCsv() {
        return csv;
    }

    public void setCsv(boolean csv) {
        this.csv = csv;
    }
}
