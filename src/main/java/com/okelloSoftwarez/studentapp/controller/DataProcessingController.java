package com.okelloSoftwarez.studentapp.controller;

import com.okelloSoftwarez.studentapp.dto.ApiResponse;
import com.okelloSoftwarez.studentapp.service.DataProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class DataProcessingController {

    private static final Logger log = LoggerFactory.getLogger(DataProcessingController.class);

    @Autowired
    private DataProcessingService dataProcessingService;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<String>> processExcel(
            @RequestParam("file") MultipartFile file) {

        // VALIDATION 1: Check a file was actually attached
        if (file == null || file.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("No file was uploaded. Please select an Excel file."));
        }

        // VALIDATION 2: Check it's an Excel file
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null ||
                !originalFileName.toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(
                            "Invalid file type. Please upload an Excel file (.xlsx)"));
        }

        log.info("Received Excel file for processing: {}", originalFileName);
        log.info("File size: {} MB", file.getSize() / (1024 * 1024));

        try {
            // Call the service to process Excel and generate CSV
            String csvFilePath = dataProcessingService.processExcelToCsv(file);

            // Return success with the CSV file path
            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Excel file processed successfully. CSV file created.",
                            csvFilePath));

        } catch (Exception e) {
            log.error("Error processing Excel file: {}", e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "Failed to process Excel file: " + e.getMessage()));
        }
    }
}