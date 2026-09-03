package com.laposte.adminmobile.service;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

/**
 * Generic JasperReports runner for the .jrxml definitions ported from the legacy webapp
 * (src/main/resources/reports/), replacing the ad-hoc fillReport()/export*() methods scattered
 * across admin.mobile.ctrl.EtatbeanCtrl.
 *
 * <p>NOTE (perf, intentional for this port): reports are compiled from .jrxml on every call via
 * {@link JasperCompileManager#compileReport(InputStream)} rather than shipping pre-compiled
 * .jasper binaries. This keeps the port simple (one source of truth, no separate build step) at
 * the cost of paying JasperReports' compilation cost on every export. A future optimization is to
 * pre-compile the .jrxml files to .jasper at build time (e.g. jasperreports-maven-plugin) and/or
 * cache the compiled {@link JasperReport} per report name in memory.
 */
@Service
public class JasperReportService {

    private final DataSource dataSource;
    private final ResourceLoader resourceLoader;
    private final String reportsLocation;

    public JasperReportService(DataSource dataSource,
                                ResourceLoader resourceLoader,
                                @Value("${app.reports.jasper-source-dir:classpath:reports/}") String reportsLocation) {
        this.dataSource = dataSource;
        this.resourceLoader = resourceLoader;
        this.reportsLocation = reportsLocation.endsWith("/") ? reportsLocation : reportsLocation + "/";
    }

    private JasperReport compile(String reportName) throws JRExceptionWrapper {
        Resource resource = resourceLoader.getResource(reportsLocation + reportName + ".jrxml");
        try (InputStream in = resource.getInputStream()) {
            return JasperCompileManager.compileReport(in);
        } catch (Exception e) {
            throw new JRExceptionWrapper("Impossible de compiler le rapport " + reportName, e);
        }
    }

    /**
     * Query-driven report: the .jrxml embeds its own SQL (see queryString), which Jasper executes
     * itself against a live JDBC connection. Use this for reports like releve_client1.jrxml and
     * releve_solde.jrxml, whose SELECT statements read directly from staging tables.
     */
    public JasperPrint fillFromQuery(String reportName, Map<String, Object> parameters) throws JRExceptionWrapper {
        JasperReport report = compile(reportName);
        try (Connection connection = dataSource.getConnection()) {
            return JasperFillManager.fillReport(report, parameters, connection);
        } catch (Exception e) {
            throw new JRExceptionWrapper("Impossible de generer le rapport " + reportName, e);
        }
    }

    /**
     * Bean-collection-driven report: fills against an in-memory list of Java beans instead of
     * letting Jasper query the database itself. Not used by the current Releves screens (their
     * reports are all query-driven), but kept here as ready-made infrastructure for reports whose
     * data is better assembled in Java (e.g. combined from several repositories/services).
     */
    public <T> JasperPrint fillFromBeans(String reportName, Map<String, Object> parameters, Collection<T> beans)
            throws JRExceptionWrapper {
        JasperReport report = compile(reportName);
        try {
            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(beans);
            return JasperFillManager.fillReport(report, parameters, ds);
        } catch (Exception e) {
            throw new JRExceptionWrapper("Impossible de generer le rapport " + reportName, e);
        }
    }

    public byte[] toPdf(JasperPrint jasperPrint) throws JRExceptionWrapper {
        try {
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (Exception e) {
            throw new JRExceptionWrapper("Impossible d'exporter le rapport en PDF", e);
        }
    }

    /**
     * Legacy-format .xls (not .xlsx) export, mirroring the JRXlsExporter flags EtatbeanCtrl used
     * (viewReportExcel/viewReportcptExcel/viewReportSimpleExcel): strip empty rows/columns/borders
     * so the sheet reads like a plain data grid rather than a paginated page layout.
     */
    public byte[] toXls(JasperPrint jasperPrint) throws JRExceptionWrapper {
        try {
            JRXlsExporter exporter = new JRXlsExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));

            SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
            configuration.setOnePagePerSheet(false);
            configuration.setDetectCellType(true);
            configuration.setRemoveEmptySpaceBetweenRows(true);
            configuration.setRemoveEmptySpaceBetweenColumns(true);
            configuration.setIgnoreCellBorder(true);
            configuration.setWhitePageBackground(false);
            configuration.setIgnorePageMargins(true);
            exporter.setConfiguration(configuration);

            exporter.exportReport();
            return out.toByteArray();
        } catch (Exception e) {
            throw new JRExceptionWrapper("Impossible d'exporter le rapport en Excel", e);
        }
    }

    /** Unchecked wrapper so controllers don't have to declare JRException/IOException/SQLException individually. */
    public static class JRExceptionWrapper extends RuntimeException {
        public JRExceptionWrapper(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
