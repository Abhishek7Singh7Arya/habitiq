package com.habitiq.fileprocessor.service;

import com.habitiq.fileprocessor.dto.RoutineExportDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class RoutineExportService {

    private static final float MARGIN = 50;
    private static final float Y_START = 750;
    private static final float LINE_HEIGHT = 18;

    public byte[] exportToPdf(RoutineExportDto routine) {
        log.info("Generating PDF for routine: {}", routine.getTitle());
        try (PDDocument document = new PDDocument()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream[] streamHolder = new PDPageContentStream[1];
            streamHolder[0] = new PDPageContentStream(document, page);
            
            float[] yHolder = new float[]{ Y_START };

            // Title
            writeText(document, streamHolder, yHolder, "HABITIQ FITNESS & DIET ROUTINE", fontBold, 18, MARGIN);
            yHolder[0] -= 10;
            writeText(document, streamHolder, yHolder, "Routine Title: " + routine.getTitle(), fontBold, 12, MARGIN);
            
            if (routine.getDescription() != null && !routine.getDescription().isBlank()) {
                writeText(document, streamHolder, yHolder, "Coach Notes: " + routine.getDescription(), fontOblique, 10, MARGIN);
            }
            yHolder[0] -= 15;

            if (routine.getDays() != null) {
                for (RoutineExportDto.RoutineDayDto day : routine.getDays()) {
                    if (yHolder[0] < 120) {
                        addNewPage(document, streamHolder, yHolder);
                    }
                    yHolder[0] -= 10;
                    writeText(document, streamHolder, yHolder, "■ " + day.getDayLabel().toUpperCase(), fontBold, 12, MARGIN);
                    
                    if (day.getNotes() != null && !day.getNotes().isBlank()) {
                        writeText(document, streamHolder, yHolder, "  Day Info: " + day.getNotes(), fontOblique, 9, MARGIN + 10);
                    }
                    yHolder[0] -= 5;

                    List<RoutineExportDto.RoutineTaskDto> tasks = day.getTasks();
                    if (tasks == null || tasks.isEmpty()) {
                        writeText(document, streamHolder, yHolder, "  No tasks scheduled.", fontRegular, 10, MARGIN + 15);
                    } else {
                        for (RoutineExportDto.RoutineTaskDto task : tasks) {
                            if (yHolder[0] < 80) {
                                addNewPage(document, streamHolder, yHolder);
                            }
                            
                            String timeStr = task.getScheduledTime() != null ? "[" + task.getScheduledTime() + "] " : "";
                            String typeStr = task.getTaskType() != null ? task.getTaskType().toUpperCase() + ": " : "";
                            String taskDesc = timeStr + typeStr + task.getDescription();
                            
                            List<String> wrappedLines = wrapText(taskDesc, fontRegular, 10, PDRectangle.A4.getWidth() - 2 * MARGIN - 30);
                            
                            for (int i = 0; i < wrappedLines.size(); i++) {
                                if (yHolder[0] < 60) {
                                    addNewPage(document, streamHolder, yHolder);
                                }
                                String lineText = (i == 0 ? "• " : "  ") + wrappedLines.get(i);
                                writeText(document, streamHolder, yHolder, lineText, fontRegular, 10, MARGIN + 15);
                            }
                            
                            if (task.getNotes() != null && !task.getNotes().isBlank()) {
                                if (yHolder[0] < 60) {
                                    addNewPage(document, streamHolder, yHolder);
                                }
                                writeText(document, streamHolder, yHolder, "    *Note: " + task.getNotes(), fontOblique, 8, MARGIN + 15);
                            }
                        }
                    }
                    yHolder[0] -= 10;
                }
            }

            streamHolder[0].close();
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate PDF routine", e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    public byte[] exportToExcel(RoutineExportDto routine) {
        log.info("Generating Excel for routine: {}", routine.getTitle());
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Weekly Routine");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dayStyle = workbook.createCellStyle();
            Font dayFont = workbook.createFont();
            dayFont.setBold(true);
            dayFont.setFontHeightInPoints((short) 11);
            dayFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            dayStyle.setFont(dayFont);
            dayStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            dayStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setWrapText(true);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(routine.getTitle().toUpperCase());
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            Row headerRow = sheet.createRow(2);
            String[] columns = {"Day Label", "Time", "Task Type", "Description", "Duration (Min)", "Task Notes"};
            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            if (routine.getDays() != null) {
                for (RoutineExportDto.RoutineDayDto day : routine.getDays()) {
                    Row dayRow = sheet.createRow(rowIdx++);
                    Cell dayCell = dayRow.createCell(0);
                    dayCell.setCellValue(day.getDayLabel().toUpperCase());
                    dayCell.setCellStyle(dayStyle);
                    
                    for (int col = 1; col < columns.length; col++) {
                        dayRow.createCell(col).setCellStyle(dayStyle);
                    }

                    if (day.getTasks() != null) {
                        for (RoutineExportDto.RoutineTaskDto task : day.getTasks()) {
                            Row row = sheet.createRow(rowIdx++);
                            row.createCell(0).setCellValue(day.getDayLabel());
                            row.createCell(1).setCellValue(task.getScheduledTime() != null ? task.getScheduledTime() : "-");
                            row.createCell(2).setCellValue(task.getTaskType() != null ? task.getTaskType() : "-");
                            
                            Cell descCell = row.createCell(3);
                            descCell.setCellValue(task.getDescription());
                            descCell.setCellStyle(textStyle);

                            row.createCell(4).setCellValue(task.getDurationMinutes() != null ? String.valueOf(task.getDurationMinutes()) : "-");
                            row.createCell(5).setCellValue(task.getNotes() != null ? task.getNotes() : "-");
                        }
                    }
                    rowIdx++;
                }
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 4000) {
                    sheet.setColumnWidth(i, 4000);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate Excel routine", e);
            throw new RuntimeException("Excel generation failed: " + e.getMessage(), e);
        }
    }

    private void writeText(PDDocument document, PDPageContentStream[] streamHolder, float[] yHolder, 
                            String text, PDType1Font font, float fontSize, float x) throws IOException {
        streamHolder[0].beginText();
        streamHolder[0].setFont(font, fontSize);
        streamHolder[0].newLineAtOffset(x, yHolder[0]);
        streamHolder[0].showText(cleanString(text));
        streamHolder[0].endText();
        yHolder[0] -= LINE_HEIGHT;
    }

    private void addNewPage(PDDocument document, PDPageContentStream[] streamHolder, float[] yHolder) throws IOException {
        streamHolder[0].close();
        PDPage newPage = new PDPage(PDRectangle.A4);
        document.addPage(newPage);
        streamHolder[0] = new PDPageContentStream(document, newPage);
        yHolder[0] = Y_START;
    }

    private String cleanString(String text) {
        if (text == null) return "";
        return text.replace("“", "\"")
                .replace("”", "\"")
                .replace("‘", "'")
                .replace("’", "'")
                .replace("—", "-")
                .replaceAll("[^\\x20-\\x7E]", "");
    }

    private List<String> wrapText(String text, PDType1Font font, float fontSize, float width) {
        java.util.List<String> result = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        try {
            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                float size = font.getStringWidth(cleanString(testLine)) / 1000 * fontSize;
                if (size > width) {
                    result.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }
            if (currentLine.length() > 0) {
                result.add(currentLine.toString());
            }
        } catch (IOException e) {
            result.add(text);
        }
        return result;
    }
}
