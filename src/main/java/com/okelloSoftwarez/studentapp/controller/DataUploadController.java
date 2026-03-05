package com.okelloSoftwarez.studentapp.controller;

import com.okelloSoftwarez.studentapp.dto.ApiResponse;
import com.okelloSoftwarez.studentapp.service.DataUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class DataUploadController {

    private static final Logger log = LoggerFactory.getLogger(DataUploadController.class);

    @Autowired
    private DataUploadService dataUploadService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Integer>> uploadCsv(
            @RequestParam("file") MultipartFile file) {

        // VALIDATION 1: Check a file was actually attached
        if (file == null || file.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(
                            "No file was uploaded. Please select a CSV file."));
        }

        // VALIDATION 2: Check it is a CSV file
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null ||
                !originalFileName.toLowerCase().endsWith(".csv")) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(
                            "Invalid file type. Please upload a CSV file (.csv)"));
        }

        log.info("Received CSV file for upload: {}", originalFileName);
        log.info("File size: {} MB", file.getSize() / (1024 * 1024));

        try {
            // Call the service to process CSV and save to database
            int totalSaved = dataUploadService.uploadCsvToDatabase(file);

            // Return success with total records saved
            return ResponseEntity.ok(
                    ApiResponse.success(
                            "CSV uploaded successfully. " + totalSaved + " records saved to database.",
                            totalSaved));

        } catch (Exception e) {
            log.error("Error uploading CSV file: {}", e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "Failed to upload CSV file: " + e.getMessage()));
        }
    }
}