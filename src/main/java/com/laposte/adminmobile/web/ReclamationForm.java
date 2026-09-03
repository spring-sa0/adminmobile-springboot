package com.laposte.adminmobile.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Form-backing bean for the {@code reclamation.xhtml} creation screen (legacy
 * {@code UserCtrl2#envoyeRecla()}/{@code enregRecla()}, bound there via {@code userCtrl2}).
 * <p>
 * {@link Reclamations} itself has no "application" column - in the legacy screen the
 * "Application" dropdown (bound to {@code userCtrl2.applimobile}) is not persisted at all, it is
 * only interpolated into the notification e-mail subject. {@code idappli} here is kept transient
 * for the same purpose and is optional, matching the legacy form (only telephone/type/motif carry
 * {@code required="true"} there).
 */
public class ReclamationForm {

    @NotBlank(message = "Telephone Client obligatoire")
    @Pattern(regexp = "\\d{8}", message = "Telephone invalide (8 chiffres attendus)")
    private String telephone;

    @NotNull(message = "Type obligatoire")
    private BigDecimal idtype;

    private BigDecimal idappli;

    @NotBlank(message = "Motif obligatoire")
    @Size(max = 500, message = "500 caracteres maximum")
    private String motif;

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public BigDecimal getIdtype() {
        return idtype;
    }

    public void setIdtype(BigDecimal idtype) {
        this.idtype = idtype;
    }

    public BigDecimal getIdappli() {
        return idappli;
    }

    public void setIdappli(BigDecimal idappli) {
        this.idappli = idappli;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}
