package com.laposte.adminmobile.export;

import jakarta.servlet.http.HttpServletRequest;

/** Formats d'export des listes, demandes par le parametre {@code ?export=xlsx|pdf}. */
public enum FormatExport {

    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PDF("pdf", "application/pdf");

    public static final String PARAMETRE = "export";

    private final String extension;
    private final String contentType;

    FormatExport(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    /** Format demande par la requete GET, ou null s'il ne s'agit pas d'un export. */
    public static FormatExport demande(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod())) {
            return null;
        }
        String valeur = request.getParameter(PARAMETRE);
        for (FormatExport format : values()) {
            if (format.extension.equalsIgnoreCase(valeur)) {
                return format;
            }
        }
        return null;
    }
}
