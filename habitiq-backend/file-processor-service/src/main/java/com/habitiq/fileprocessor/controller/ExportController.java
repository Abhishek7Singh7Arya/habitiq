package com.habitiq.fileprocessor.controller;

import com.habitiq.common.dto.ApiResponse;
import com.habitiq.fileprocessor.dto.RoutineExportDto;
import com.habitiq.fileprocessor.service.RoutineExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/files/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Download weekly routines in PDF or Excel format")
public class ExportController {

    private final RoutineExportService exportService;
    private final RestTemplate restTemplate;

    @GetMapping("/routine/{routineId}")
    @Operation(summary = "Export a confirmed routine to PDF or Excel")
    public ResponseEntity<?> exportRoutine(
            @PathVariable("routineId") UUID routineId,
            @RequestParam(value = "format", defaultValue = "pdf") String format,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        log.info("Request to export routine {} in format: {}", routineId, format);

        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }
        if (userId != null) {
            headers.set("X-User-Id", userId);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ApiResponse<RoutineExportDto>> response = restTemplate.exchange(
                    "http://ai-agent-service/api/ai/routines/" + routineId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<RoutineExportDto>>() {}
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null || !response.getBody().isSuccess()) {
                log.error("Failed to retrieve routine: status={}", response.getStatusCode());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Failed to retrieve routine from AI agent service", "ROUTINE_FETCH_FAILED"));
            }

            RoutineExportDto routine = response.getBody().getData();
            if (routine == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Routine data is null", "ROUTINE_NOT_FOUND"));
            }

            byte[] fileBytes;
            String fileName = routine.getTitle().toLowerCase().replaceAll("[^a-z0-9]", "_");
            MediaType mediaType;

            if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
                fileBytes = exportService.exportToExcel(routine);
                fileName += ".xlsx";
                mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else {
                fileBytes = exportService.exportToPdf(routine);
                fileName += ".pdf";
                mediaType = MediaType.APPLICATION_PDF;
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(mediaType);
            responseHeaders.setContentDisposition(ContentDisposition.builder("attachment").filename(fileName).build());
            responseHeaders.setContentLength(fileBytes.length);

            return new ResponseEntity<>(fileBytes, responseHeaders, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error occurred while exporting routine {}", routineId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Export failed: " + e.getMessage(), "EXPORT_FAILED"));
        }
    }
}
