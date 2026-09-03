package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.Releve;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Server-side equivalent of PrimeFaces' {@code p:dataExporter type="xls"} on releveliste.xhtml's
 * dataTable: dumps the RELEVE parametrage grid (not a Jasper report) to a spreadsheet. Produces
 * .xlsx via Apache POI's XSSF rather than the legacy .xls PrimeFaces produced, since there is no
 * Jasper template behind this screen's export - it's a plain listing dump.
 */
@Service
public class ReleveGridExportService {

    private static final String[] HEADERS = {
            "Numero Compte", "Intitule Compte", "Mail", "Frequence", "Format"
    };

    public byte[] exportReleveGrid(List<Releve> releves) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Listereleve");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }

            int rowIdx = 1;
            for (Releve releve : releves) {
                Row row = sheet.createRow(rowIdx++);

                Cell numeroCell = row.createCell(0);
                if (releve.getNumerocompte() != null) {
                    numeroCell.setCellValue(releve.getNumerocompte());
                } else {
                    numeroCell.setBlank();
                }

                row.createCell(1).setCellValue(nullToEmpty(releve.getIntitule()));
                row.createCell(2).setCellValue(nullToEmpty(releve.getMail()));
                row.createCell(3).setCellValue(nullToEmpty(releve.getJourenvoi()));
                row.createCell(4).setCellValue(nullToEmpty(releve.getTypefichier()));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
