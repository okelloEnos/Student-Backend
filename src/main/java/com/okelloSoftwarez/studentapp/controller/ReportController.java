package com.okelloSoftwarez.studentapp.controller;

import com.okelloSoftwarez.studentapp.dto.ApiResponse;
import com.okelloSoftwarez.studentapp.dto.StudentDTO;
import com.okelloSoftwarez.studentapp.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;

    // ENDPOINT 1: Get paginated students with filters
    // GET /api/report/students?page=0&size=20
    @GetMapping("/students")
    public ResponseEntity<ApiResponse<Page<StudentDTO>>> getStudents(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String studentClass,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Treat empty string as null so filters are ignored
        String classFilter = (studentClass != null && studentClass.isBlank())
                ? null
                : studentClass;

        log.info("Fetching students — studentId: {}, class: {}, page: {}, size: {}",
                studentId, classFilter, page, size);

        try {
            Page<StudentDTO> result = reportService.getStudents(
                    studentId, classFilter, page, size);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Students fetched successfully. " +
                                    "Total records: " + result.getTotalElements(),
                            result));

        } catch (Exception e) {
            log.error("Error fetching students: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "Failed to fetch students: " + e.getMessage()));
        }
    }

    // ENDPOINT 2: Get distinct class names for dropdown
    // GET /api/report/classes
    @GetMapping("/classes")
    public ResponseEntity<ApiResponse<List<String>>> getClasses() {

        log.info("Fetching distinct class names");

        try {
            List<String> classes = reportService.getDistinctClasses();

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Classes fetched successfully",
                            classes));

        } catch (Exception e) {
            log.error("Error fetching classes: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "Failed to fetch classes: " + e.getMessage()));
        }
    }

    // ENDPOINT 3: Export to Excel
    // GET /api/report/export/excel
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String studentClass) {

        String classFilter = (studentClass != null && studentClass.isBlank())
                ? null
                : studentClass;

        log.info("Exporting Excel — studentId: {}, class: {}", studentId, classFilter);

        try {
            byte[] excelBytes = reportService.exportToExcel(studentId, classFilter);

            // Tell the browser this is a downloadable Excel file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData(
                    "attachment", "students_export.xlsx");
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error exporting Excel: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    // ENDPOINT 4: Export to CSV
    // GET /api/report/export/csv
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String studentClass) {

        String classFilter = (studentClass != null && studentClass.isBlank())
                ? null
                : studentClass;

        log.info("Exporting CSV — studentId: {}, class: {}", studentId, classFilter);

        try {
            byte[] csvBytes = reportService.exportToCsv(studentId, classFilter);

            // Tell the browser this is a downloadable CSV file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData(
                    "attachment", "students_export.csv");
            headers.setContentLength(csvBytes.length);

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error exporting CSV: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    // ENDPOINT 5: Export to PDF
    // GET /api/report/export/pdf
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String studentClass) {

        String classFilter = (studentClass != null && studentClass.isBlank())
                ? null
                : studentClass;

        log.info("Exporting PDF — studentId: {}, class: {}", studentId, classFilter);

        try {
            byte[] pdfBytes = reportService.exportToPdf(studentId, classFilter);

            // Tell the browser this is a downloadable PDF file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(
                    "attachment", "students_export.pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error exporting PDF: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}