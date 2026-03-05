package com.okelloSoftwarez.studentapp.service;

import com.okelloSoftwarez.studentapp.model.Student;
import com.okelloSoftwarez.studentapp.repository.StudentRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataUploadService {

    private static final Logger log = LoggerFactory.getLogger(DataUploadService.class);

    // How many records to save in one database call
    private static final int BATCH_SIZE = 500;

    @Autowired
    private StudentRepository studentRepository;

    public int uploadCsvToDatabase(MultipartFile file)
            throws IOException, CsvValidationException {

        // STEP 1: Save uploaded CSV file temporarily to disk
        Path tempFile = Files.createTempFile("upload_", ".csv");
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        log.info("Uploaded CSV saved temporarily at: {}", tempFile);

        int totalSaved = 0;

        // STEP 2: Open the CSV file for reading
        try (CSVReader csvReader = new CSVReader(
                new FileReader(tempFile.toFile()))) {

            // STEP 3: Skip the header row
            String[] header = csvReader.readNext();
            log.info("Skipping header row: {}", String.join(", ", header));

            // STEP 4: Prepare the batch list
            List<Student> batch = new ArrayList<>(BATCH_SIZE);

            String[] row;

            // STEP 5: Read each row one by one
            while ((row = csvReader.readNext()) != null) {

                // Skip empty rows
                if (row.length < 6) {
                    continue;
                }

                try {
                    // STEP 6: Parse all 6 fields from the CSV row
                    // CSV columns: studentId, firstName, lastName,
                    // DOB, class, score

                    // Column 0 — studentId
                    Long studentId = Long.parseLong(row[0].trim());

                    // Column 1 — firstName
                    String firstName = row[1].trim();

                    // Column 2 — lastName
                    String lastName = row[2].trim();

                    // Column 3 — DOB (format: yyyy-MM-dd)
                    LocalDate dob = LocalDate.parse(row[3].trim());

                    // Column 4 — class
                    String studentClass = row[4].trim();

                    // Column 5 — score, ADD 5 before saving
                    int originalScore = Integer.parseInt(row[5].trim());
                    int newScore = originalScore + 5;

                    // STEP 7: Create Student object
                    Student student = new Student();
                    student.setStudentId(studentId);
                    student.setFirstName(firstName);
                    student.setLastName(lastName);
                    student.setDob(dob);
                    student.setStudentClass(studentClass);
                    student.setScore(newScore);

                    // Add to current batch
                    batch.add(student);

                    // STEP 8: When batch is full, save to database
                    // then clear the batch for the next group
                    if (batch.size() >= BATCH_SIZE) {
                        studentRepository.saveAll(batch);
                        totalSaved += batch.size();
                        batch.clear();

                        // Log progress every 10 batches (every 5000 records)
                        if (totalSaved % 5000 == 0) {
                            log.info("Saved {} records so far...", totalSaved);
                        }
                    }

                } catch (Exception e) {
                    // Log the bad row and continue — don't stop the whole process
                    log.warn("Skipping invalid row {}: {}", totalSaved + 1, e.getMessage());
                }
            }

            if (!batch.isEmpty()) {
                studentRepository.saveAll(batch);
                totalSaved += batch.size();
                log.info("Saved final batch of {} records", batch.size());
            }

        }

        // STEP 10: Delete the temporary file
        Files.deleteIfExists(tempFile);
        log.info("Temporary CSV file deleted");

        log.info("Upload complete. Total records saved to database: {}", totalSaved);

        return totalSaved;
    }
}