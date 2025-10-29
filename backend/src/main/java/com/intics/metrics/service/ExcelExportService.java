package com.intics.metrics.service;

import com.intics.metrics.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] generateFailureRecoveryReportsExcel(
            List<RecoveredDocument> recovered,
            List<WaitingDocument> waiting,
            List<InboundFailedDocument> inboundFailed,
            List<ProcessFailedDocument> processFailed,
            List<AbortedDocument> aborted) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {
            
            createRecoveredSheet(workbook, recovered);
            createWaitingSheet(workbook, waiting);
            createInboundFailedSheet(workbook, inboundFailed);
            createProcessFailedSheet(workbook, processFailed);
            createAbortedSheet(workbook, aborted);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createRecoveredSheet(Workbook workbook, List<RecoveredDocument> data) {
        Sheet sheet = workbook.createSheet("Recovered from Inbound Failed");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        Row headerRow = sheet.createRow(0);
        String[] headers = {"DCN ID", "SI Case ID", "Document Type", "Download Completed On"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (RecoveredDocument doc : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(doc.getDcnId());
            row.createCell(1).setCellValue(doc.getSiCaseId());
            row.createCell(2).setCellValue(doc.getDocumentType());
            row.createCell(3).setCellValue(doc.getDownloadCompletedOn());
        }

        autoSizeColumns(sheet, headers.length);
    }

    private void createWaitingSheet(Workbook workbook, List<WaitingDocument> data) {
        Sheet sheet = workbook.createSheet("Kill Switch - Documents Waiting");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        Row headerRow = sheet.createRow(0);
        String[] headers = {"DCN ID", "SI Case ID", "Download Completed On", "Status", "Time Remaining to Process"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (WaitingDocument doc : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(doc.getDcnId());
            row.createCell(1).setCellValue(doc.getSiCaseId());
            row.createCell(2).setCellValue(doc.getDownloadCompletedOn());
            row.createCell(3).setCellValue(doc.getStatus());
            row.createCell(4).setCellValue(doc.getTimeRemainingToProcess());
        }

        autoSizeColumns(sheet, headers.length);
    }

    private void createInboundFailedSheet(Workbook workbook, List<InboundFailedDocument> data) {
        Sheet sheet = workbook.createSheet("Inbound Failed Status");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Download Completed On", "Transaction ID", "DCN ID", "SI Case ID"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (InboundFailedDocument doc : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(doc.getDownloadCompletedOn());
            row.createCell(1).setCellValue(doc.getTransactionId());
            row.createCell(2).setCellValue(doc.getDcnId());
            row.createCell(3).setCellValue(doc.getSiCaseId());
        }

        autoSizeColumns(sheet, headers.length);
    }

    private void createProcessFailedSheet(Workbook workbook, List<ProcessFailedDocument> data) {
        Sheet sheet = workbook.createSheet("Process Failed Status");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Download Completed On", "DCN ID", "SI Case ID", "Status", "Error Code", "Error Message"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (ProcessFailedDocument doc : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(doc.getDownloadCompletedOn());
            row.createCell(1).setCellValue(doc.getDcnId());
            row.createCell(2).setCellValue(doc.getSiCaseId());
            row.createCell(3).setCellValue(doc.getStatus());
            row.createCell(4).setCellValue(doc.getErrorCode());
            row.createCell(5).setCellValue(doc.getErrorMessage());
        }

        autoSizeColumns(sheet, headers.length);
    }

    private void createAbortedSheet(Workbook workbook, List<AbortedDocument> data) {
        Sheet sheet = workbook.createSheet("Aborted Status");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Download Completed On", "DCN ID", "SI Case ID", "Status", "Error Code", "Error Message"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (AbortedDocument doc : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(doc.getDownloadCompletedOn());
            row.createCell(1).setCellValue(doc.getDcnId());
            row.createCell(2).setCellValue(doc.getSiCaseId());
            row.createCell(3).setCellValue(doc.getStatus());
            row.createCell(4).setCellValue(doc.getErrorCode());
            row.createCell(5).setCellValue(doc.getErrorMessage());
        }

        autoSizeColumns(sheet, headers.length);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

}
