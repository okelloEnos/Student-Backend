package com.okelloSoftwarez.studentapp.service;

import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class DataProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DataProcessingService.class);

    @Value("${app.storage.base-path}")
    private String basePath;

    public String processExcelToCsv(MultipartFile file) throws IOException {

        // STEP 1: Save the uploaded file temporarily to disk
        // We need it on disk to open it as an Excel workbook
        Path tempFile = Files.createTempFile("upload_", ".xlsx");
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        log.info("Uploaded Excel file saved temporarily at: {}", tempFile);

        // STEP 2: Build the CSV output file path
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String csvFileName = "students_processed_" + System.currentTimeMillis() + ".csv";
        String csvFilePath = basePath + File.separator + csvFileName;

        log.info("CSV output will be saved to: {}", csvFilePath);

        // STEP 3: Open Excel file and write to CSV
        try (
                // Open the Excel file for reading
                Workbook workbook = WorkbookFactory.create(tempFile.toFile());

                // Open the CSV file for writing
                CSVWriter csvWriter = new CSVWriter(new FileWriter(csvFilePath))) {
            // Get the first sheet
            Sheet sheet = workbook.getSheetAt(0);

            // Get total rows for logging
            int totalRows = sheet.getLastRowNum();
            log.info("Total rows in Excel (including header): {}", totalRows);

            // STEP 4: Write CSV header row first
            String[] csvHeader = {
                    "studentId", "firstName", "lastName", "DOB", "class", "score"
            };
            csvWriter.writeNext(csvHeader);

            // STEP 5: Loop through each row
            // Start at 1 to SKIP the header row (row 0)
            int processedCount = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                // Skip if row is completely empty
                if (row == null) {
                    continue;
                }

                // STEP 6: Read all 6 columns from the Excel row

                // studentId — numeric
                String studentId = getCellValueAsString(row.getCell(0));

                // firstName — string
                String firstName = getCellValueAsString(row.getCell(1));

                // lastName — string
                String lastName = getCellValueAsString(row.getCell(2));

                // DOB — date stored as string
                String dob = getCellValueAsString(row.getCell(3));

                // class — string
                String studentClass = getCellValueAsString(row.getCell(4));

                // score — numeric, ADD 10 before saving
                double originalScore = row.getCell(5) != null
                        ? row.getCell(5).getNumericCellValue()
                        : 0;
                int newScore = (int) originalScore + 10;

                // STEP 7: Write the row to CSV
                String[] csvRow = {
                        studentId,
                        firstName,
                        lastName,
                        dob,
                        studentClass,
                        String.valueOf(newScore)
                };
                csvWriter.writeNext(csvRow);

                processedCount++;

                // Log progress every 100,000 rows
                if (processedCount % 100_000 == 0) {
                    log.info("Processed {} / {} rows", processedCount, totalRows);
                }
            }

            log.info("CSV processing complete. Total rows written: {}", processedCount);
        }

        // STEP 8: Delete the temporary Excel file
        Files.deleteIfExists(tempFile);
        log.info("Temporary file deleted");

        // STEP 9: Return the CSV file path
        return csvFilePath;
    }

    // HELPER METHOD: Read any cell as a String
    // regardless of its type (number, string, date etc)
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case NUMERIC:
                // Check if it's a date cell
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue()
                            .toLocalDate()
                            .toString();
                }
                // Otherwise it's a plain number
                // Use long to avoid decimals on whole numbers
                return String.valueOf((long) cell.getNumericCellValue());

            case STRING:
                return cell.getStringCellValue().trim();

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                return cell.getCellFormula();

            case BLANK:
            default:
                return "";
        }
    }
}