package com.habitiq.fileprocessor.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileParserService {

    public String parseFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new RuntimeException("File name is null");
        }

        String lowerName = fileName.toLowerCase();

        try {
            if (lowerName.endsWith(".pdf")) {
                return parsePdf(file);
            } else if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                return parseExcel(file);
            } else if (lowerName.endsWith(".txt") || lowerName.endsWith(".csv")) {
                return parseTxt(file);
            } else {
                throw new RuntimeException("Unsupported file type: " + fileName);
            }
        } catch (Exception e) {
            log.error("Failed to parse file {}: {}", fileName, e.getMessage());
            throw new RuntimeException("File parsing failed: " + e.getMessage(), e);
        }
    }

    private String parsePdf(MultipartFile file) throws Exception {
        byte[] data = file.getInputStream().readAllBytes();
        try (PDDocument document = Loader.loadPDF(data)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            log.info("Parsed PDF: {} chars", text.length());
            return text;
        }
    }

    private String parseExcel(MultipartFile file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sb.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        sb.append(getCellValue(cell)).append("\t");
                    }
                    sb.append("\n");
                }
            }
        }
        log.info("Parsed Excel: {} chars", sb.length());
        return sb.toString();
    }

    private String parseTxt(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String text = reader.lines().collect(Collectors.joining("\n"));
            log.info("Parsed TXT: {} chars", text.length());
            return text;
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toString()
                    : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default      -> "";
        };
    }
}
