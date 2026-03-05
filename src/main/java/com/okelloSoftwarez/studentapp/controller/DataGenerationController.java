package com.okelloSoftwarez.studentapp.controller;

import com.okelloSoftwarez.studentapp.dto.ApiResponse;
import com.okelloSoftwarez.studentapp.dto.GenerateRequest;
import com.okelloSoftwarez.studentapp.service.DataGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DataGenerationController {

    private static final Logger log = LoggerFactory.getLogger(DataGenerationController.class);

    // Spring Boot automatically injects the service here
    @Autowired
    private DataGenerationService dataGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<String>> generateData(
            @RequestBody GenerateRequest request) {

        // Basic validation — number must be greater than 0
        if (request.getNumberOfRecords() <= 0) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("Number of records must be greater than 0"));
        }

        log.info("Received request to generate {} records", request.getNumberOfRecords());

        try {
            // Call the service to do the actual work
            String filePath = dataGenerationService.generateExcelFile(
                    request.getNumberOfRecords());

            // Return success response with the file path
            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Excel file generated successfully with " +
                                    request.getNumberOfRecords() + " records",
                            filePath));

        } catch (Exception e) {
            log.error("Error generating Excel file: {}", e.getMessage());

            // Return error response
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "Failed to generate Excel file: " + e.getMessage()));
        }
    }
}