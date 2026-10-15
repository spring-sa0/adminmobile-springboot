package com.laposte.adminmobile.service;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.fonts.FontInfo;
import net.sf.jasperreports.engine.fonts.FontUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les rapports ne doivent pas dependre des polices installees sur le serveur : sous Linux (Docker),
 * Arial et Times New Roman n'existent pas et le remplissage echouait (JRFontNotFoundException).
 */
class JasperFontsTest {

    private static final Pattern FONT_NAME = Pattern.compile("fontName=\"([^\"]+)\"");

    /**
     * Toute police citee dans un .jrxml doit etre fournie par l'extension embarquee
     * (fonts/fonts.xml), et non par le systeme : ce test echoue sous Windows comme sous Linux si
     * une police est ajoutee a un rapport sans etre declaree.
     */
    @Test
    void everyFontUsedByReportsIsBundled() throws Exception {
        Set<String> polices = new TreeSet<>();
        for (Resource jrxml : new PathMatchingResourcePatternResolver().getResources("classpath:reports/*.jrxml")) {
            String contenu = jrxml.getContentAsString(StandardCharsets.UTF_8);
            Matcher m = FONT_NAME.matcher(contenu);
            while (m.find()) {
                polices.add(m.group(1));
            }
        }
        assertThat(polices).contains("Arial");

        FontUtil fontUtil = FontUtil.getInstance(DefaultJasperReportsContext.getInstance());
        for (String police : polices) {
            FontInfo info = fontUtil.getFontInfo(police, null);
            assertThat(info).as("police '%s' absente de fonts/fonts.xml", police).isNotNull();
            assertThat(info.getFontFamily().getNormalFace().getFont()).as("fichier de la police '%s'", police).isNotNull();
        }
    }

    /** Remplissage + export Excel des rapports utilises par l'application, sans base de donnees. */
    @ParameterizedTest
    @ValueSource(strings = {"releve_client1", "releve_solde"})
    void reportsFillAndExportWithoutSystemFonts(String rapport) throws Exception {
        JasperReport compile;
        try (InputStream in = new DefaultResourceLoader().getResource("classpath:reports/" + rapport + ".jrxml").getInputStream()) {
            compile = JasperCompileManager.compileReport(in);
        }
        JasperPrint print = JasperFillManager.fillReport(compile, new HashMap<>(), new JREmptyDataSource(3));

        byte[] xls = new JasperReportService(null, new DefaultResourceLoader(), "classpath:reports/").toXls(print);
        assertThat(xls).isNotEmpty();
    }
}
