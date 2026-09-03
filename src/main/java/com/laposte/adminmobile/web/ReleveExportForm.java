package com.laposte.adminmobile.web;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Form backing bean for the two Jasper export screens (releves/excel-afb120,
 * releves/excel-togocel). Mirrors the datedebut/datefin/numerocompte fields of
 * admin.mobile.ctrl.EtatbeanCtrl.
 */
public class ReleveExportForm {

    private String numerocompte;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datedebut;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datefin;

    public String getNumerocompte() {
        return numerocompte;
    }

    public void setNumerocompte(String numerocompte) {
        this.numerocompte = numerocompte;
    }

    public LocalDate getDatedebut() {
        return datedebut;
    }

    public void setDatedebut(LocalDate datedebut) {
        this.datedebut = datedebut;
    }

    public LocalDate getDatefin() {
        return datefin;
    }

    public void setDatefin(LocalDate datefin) {
        this.datefin = datefin;
    }
}
