package com.okelloSoftwarez.studentapp.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
public class DataGenerationService {

    // Logger to print progress messages to the console
    private static final Logger log = LoggerFactory.getLogger(DataGenerationService.class);

    // The 5 possible class options
    private static final String[] CLASSES = {
            "Class1", "Class2", "Class3", "Class4", "Class5"
    };

    // All lowercase letters to build random names from
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

    // Date formatter for writing dates to Excel
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Reads the path from application.properties
    @Value("${app.storage.base-path}")
    private String basePath;

    public String generateExcelFile(int numberOfRecords) throws IOException {

        // STEP 1: Create storage directory if it doesn't exist
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs(); // creates all folders in the path
            log.info("Created storage directory: {}", basePath);
        }

        // STEP 2: Build the file name and full path
        String fileName = "students_" + System.currentTimeMillis() + ".xlsx";
        String filePath = basePath + File.separator + fileName;

        log.info("Starting Excel generation for {} records", numberOfRecords);
        log.info("File will be saved to: {}", filePath);

        // STEP 3: Create the streaming workbook
        // 1000 = number of rows kept in memory at a time
        // older rows are flushed to disk automatically
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {

            // Compress temp files to save disk space
            workbook.setCompressTempFiles(true);

            // Create the sheet
            Sheet sheet = workbook.createSheet("Students");

            // STEP 4: Write the header row (row 0)
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "studentId", "firstName", "lastName", "DOB", "class", "score"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // STEP 5: Set up random data tools
            Random random = new Random();

            // Calculate the range of days between 2000-01-01 and 2010-12-31
            LocalDate startDate = LocalDate.of(2000, 1, 1);
            LocalDate endDate = LocalDate.of(2010, 12, 31);
            long daysBetween = endDate.toEpochDay() - startDate.toEpochDay();

            // STEP 6: Loop and write each student row
            // Row index starts at 1 (row 0 is the header)
            for (int i = 1; i <= numberOfRecords; i++) {

                Row row = sheet.createRow(i);

                // studentId — starts at 1, increments by 1
                row.createCell(0).setCellValue(i);

                // firstName — random 3 to 8 lowercase letters
                row.createCell(1).setCellValue(
                        generateRandomString(random, 3, 8));

                // lastName — random 3 to 8 lowercase letters
                row.createCell(2).setCellValue(
                        generateRandomString(random, 3, 8));

                // DOB — random date between 2000-01-01 and 2010-12-31
                long randomDay = startDate.toEpochDay() +
                        (long) (random.nextDouble() * daysBetween);
                LocalDate dob = LocalDate.ofEpochDay(randomDay);
                row.createCell(3).setCellValue(
                        dob.format(DATE_FORMATTER));

                // class — random pick from the 5 options
                row.createCell(4).setCellValue(
                        CLASSES[random.nextInt(CLASSES.length)]);

                // score — random number between 55 and 75 (inclusive)
                row.createCell(5).setCellValue(
                        55 + random.nextInt(21) // 21 because nextInt is exclusive
                );

                // Log progress every 100,000 rows so you can see it working
                if (i % 100_000 == 0) {
                    log.info("Generated {} / {} rows", i, numberOfRecords);
                }
            }

            // STEP 7: Write the workbook to the file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            // Clean up temp files used by streaming workbook
            workbook.dispose();

            log.info("Excel file generated successfully: {}", filePath);
        }

        // STEP 8: Return the file path to the controller
        return filePath;
    }

    // HELPER METHOD: Generate a random string between minLength and maxLength
    // characters
    private String generateRandomString(Random random, int minLength, int maxLength) {
        // Pick a random length between min and max
        int length = minLength + random.nextInt(maxLength - minLength + 1);

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            // Pick a random letter from the alphabet
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}