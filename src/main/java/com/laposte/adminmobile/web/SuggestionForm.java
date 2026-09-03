package com.laposte.adminmobile.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Form-backing bean for the {@code sugestion.xhtml} creation screen (legacy
 * {@code UserCtrl2#envoyeSugestion()}/{@code enregsugest()}). {@link Suggestions} has no
 * "application" column either; {@code idappli} is kept transient, only used to compose the
 * notification e-mail subject, matching the legacy form.
 */
public class SuggestionForm {

    @NotBlank(message = "Telephone Client obligatoire")
    @Pattern(regexp = "\\d{8}", message = "Telephone invalide (8 chiffres attendus)")
    private String telephone;

    private BigDecimal idappli;

    @NotBlank(message = "Suggestion obligatoire")
    @Size(max = 500, message = "500 caracteres maximum")
    private String motif;

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
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
