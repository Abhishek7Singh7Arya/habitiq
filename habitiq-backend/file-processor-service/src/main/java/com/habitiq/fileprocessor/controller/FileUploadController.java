package com.habitiq.fileprocessor.controller;

import com.habitiq.common.dto.ApiResponse;
import com.habitiq.fileprocessor.service.FileParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File Processor", description = "Upload and parse diet/workout files")
public class FileUploadController {

    private final FileParserService fileParserService;

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a PDF, Excel or TXT file and extract text content")
    public ResponseEntity<ApiResponse<Map<String, String>>> parseFile(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is empty", "EMPTY_FILE"));
        }

        String extractedText = fileParserService.parseFile(file);
        return ResponseEntity.ok(ApiResponse.success(
                "File parsed successfully",
                Map.of(
                    "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                    "extractedText", extractedText,
                    "characterCount", String.valueOf(extractedText.length())
                )
        ));
    }
}
