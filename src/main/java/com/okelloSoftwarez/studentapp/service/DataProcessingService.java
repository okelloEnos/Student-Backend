package com.okelloSoftwarez.studentapp.service;

import com.opencsv.CSVWriter;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DataProcessingService.class);

    @Value("${app.storage.base-path}")
    private String basePath;

    public String processExcelToCsv(MultipartFile file) throws Exception {

        // ------------------------------------------------
        // STEP 1: Save uploaded file temporarily
        // ------------------------------------------------
        Path tempFile = Files.createTempFile("upload_", ".xlsx");
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        log.info("Uploaded Excel saved temporarily at: {}", tempFile);

        // ------------------------------------------------
        // STEP 2: Prepare CSV output path
        // ------------------------------------------------
        File dir = new File(basePath);
        if (!dir.exists())
            dir.mkdirs();

        String csvFileName = "students_processed_" + System.currentTimeMillis() + ".csv";
        String csvFilePath = basePath + File.separator + csvFileName;
        log.info("CSV output will be saved to: {}", csvFilePath);

        // ------------------------------------------------
        // STEP 3: Open CSV writer
        // ------------------------------------------------
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(csvFilePath))) {

            // Write header first
            csvWriter.writeNext(new String[] {
                    "studentId", "firstName", "lastName", "DOB", "class", "score"
            });

            // ------------------------------------------------
            // STEP 4: Use SAX streaming reader — reads row
            // by row without loading whole file into memory
            // ------------------------------------------------
            try (OPCPackage opcPackage = OPCPackage.open(tempFile.toFile())) {

                XSSFReader xssfReader = new XSSFReader(opcPackage);
                SharedStrings sharedStrings = xssfReader.getSharedStringsTable();
                StylesTable stylesTable = xssfReader.getStylesTable();

                // Our custom handler that processes each row
                RowHandler rowHandler = new RowHandler(csvWriter, log);

                ContentHandler handler = new XSSFSheetXMLHandler(
                        stylesTable, sharedStrings, rowHandler, false);

                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                SAXParser parser = factory.newSAXParser();
                XMLReader reader = parser.getXMLReader();
                reader.setContentHandler(handler);

                // Read first sheet only
                XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) xssfReader.getSheetsData();

                if (sheets.hasNext()) {
                    try (InputStream sheetStream = sheets.next()) {
                        reader.parse(new InputSource(sheetStream));
                    }
                }

                log.info("CSV processing complete. Total rows written: {}",
                        rowHandler.getRowCount());
            }
        }

        // ------------------------------------------------
        // STEP 5: Delete temp file
        // ------------------------------------------------
        Files.deleteIfExists(tempFile);
        log.info("Temporary file deleted");

        return csvFilePath;
    }

    // ================================================
    // INNER CLASS: Handles each row from the SAX reader
    // ================================================
    private static class RowHandler implements SheetContentsHandler {

        private final CSVWriter csvWriter;
        private final Logger log;

        // Tracks current row data
        private String[] currentRow = new String[6];
        private int currentRowNum = -1;
        private int rowCount = 0;

        public RowHandler(CSVWriter csvWriter, Logger log) {
            this.csvWriter = csvWriter;
            this.log = log;
        }

        @Override
        public void startRow(int rowNum) {
            currentRowNum = rowNum;
            currentRow = new String[] { "", "", "", "", "", "" };
        }

        @Override
        public void endRow(int rowNum) {
            // Skip header row (row 0)
            if (rowNum == 0)
                return;

            try {
                // Add 10 to score (column index 5)
                if (currentRow[5] != null && !currentRow[5].isEmpty()) {
                    double score = Double.parseDouble(currentRow[5]);
                    currentRow[5] = String.valueOf((int) score + 10);
                }

                csvWriter.writeNext(currentRow);
                rowCount++;

                if (rowCount % 100_000 == 0) {
                    log.info("Processed {} rows", rowCount);
                }

            } catch (Exception e) {
                log.warn("Skipping row {}: {}", rowNum, e.getMessage());
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue,
                XSSFComment comment) {
            // Get column index from cell reference (A=0, B=1, C=2...)
            int colIndex = getColIndex(cellReference);
            if (colIndex >= 0 && colIndex < 6) {
                currentRow[colIndex] = formattedValue != null
                        ? formattedValue
                        : "";
            }
        }

        // Convert column letter to index e.g. A→0, B→1, F→5
        private int getColIndex(String cellRef) {
            if (cellRef == null)
                return -1;
            int col = 0;
            for (char c : cellRef.toCharArray()) {
                if (Character.isLetter(c)) {
                    col = col * 26 + (Character.toUpperCase(c) - 'A' + 1);
                } else {
                    break;
                }
            }
            return col - 1;
        }

        public int getRowCount() {
            return rowCount;
        }
    }
}
