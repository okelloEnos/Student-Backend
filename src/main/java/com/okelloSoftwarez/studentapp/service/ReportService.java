package com.okelloSoftwarez.studentapp.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.okelloSoftwarez.studentapp.dto.StudentDTO;
import com.okelloSoftwarez.studentapp.model.Student;
import com.okelloSoftwarez.studentapp.repository.StudentRepository;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private StudentRepository studentRepository;

    // Convert Student entity to StudentDTO
    private StudentDTO toDTO(Student student) {
        return new StudentDTO(
                student.getId(),
                student.getStudentId(),
                student.getFirstName(),
                student.getLastName(),
                student.getDob(),
                student.getStudentClass(),
                student.getScore());
    }

    // METHOD 1: Get paginated students with filters
    public Page<StudentDTO> getStudents(
            Long studentId,
            String studentClass,
            int page,
            int size) {

        // Build pageable — sort by studentId ascending
        PageRequest pageable = PageRequest.of(
                page, size, Sort.by("studentId").ascending());

        // Fetch from repository with filters
        Page<Student> studentPage = studentRepository.findWithFilters(
                studentId,
                studentClass,
                pageable);

        // Convert entities to DTOs
        List<StudentDTO> dtos = studentPage.getContent()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, studentPage.getTotalElements());
    }

    // METHOD 2: Get distinct class names for dropdown
    public List<String> getDistinctClasses() {
        return studentRepository.findDistinctClasses();
    }

    // METHOD 3: Export to Excel
    public byte[] exportToExcel(Long studentId, String studentClass)
            throws IOException {

        // Fetch all matching records (no pagination)
        List<Student> students = studentRepository.findAllWithFilters(
                studentId, studentClass);

        log.info("Exporting {} records to Excel", students.size());

        try (
                XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Students");

            // ---- Style for header row ----
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // ---- Write header row ----
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Student ID", "First Name", "Last Name", "DOB", "Class", "Score"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000); // Set column width
            }

            // ---- Write data rows ----
            int rowNum = 1;
            for (Student s : students) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(s.getStudentId());
                row.createCell(1).setCellValue(s.getFirstName());
                row.createCell(2).setCellValue(s.getLastName());
                row.createCell(3).setCellValue(s.getDob().toString());
                row.createCell(4).setCellValue(s.getStudentClass());
                row.createCell(5).setCellValue(s.getScore());
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // METHOD 4: Export to CSV
    public byte[] exportToCsv(Long studentId, String studentClass)
            throws IOException {

        // Fetch all matching records
        List<Student> students = studentRepository.findAllWithFilters(
                studentId, studentClass);

        log.info("Exporting {} records to CSV", students.size());

        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                CSVWriter csvWriter = new CSVWriter(
                        new OutputStreamWriter(outputStream))) {
            // Write header
            csvWriter.writeNext(new String[] {
                    "studentId", "firstName", "lastName", "DOB", "class", "score"
            });

            // Write data rows
            for (Student s : students) {
                csvWriter.writeNext(new String[] {
                        String.valueOf(s.getStudentId()),
                        s.getFirstName(),
                        s.getLastName(),
                        s.getDob().toString(),
                        s.getStudentClass(),
                        String.valueOf(s.getScore())
                });
            }

            // Flush before returning bytes
            csvWriter.flush();
            return outputStream.toByteArray();
        }
    }

    // METHOD 5: Export to PDF
    public byte[] exportToPdf(Long studentId, String studentClass)
            throws Exception {

        // Fetch all matching records
        List<Student> students = studentRepository.findAllWithFilters(
                studentId, studentClass);

        log.info("Exporting {} records to PDF", students.size());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // ---- Set up PDF document (landscape for wide table) ----
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, outputStream);
        document.open();

        // ---- Title ----
        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 16,
                com.itextpdf.text.Font.BOLD);
        Paragraph title = new Paragraph("Student Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // ---- Subtitle with record count ----
        com.itextpdf.text.Font subtitleFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10,
                com.itextpdf.text.Font.NORMAL);
        Paragraph subtitle = new Paragraph(
                "Total Records: " + students.size(), subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(15);
        document.add(subtitle);

        // ---- Create table with 6 columns ----
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1.5f, 2f, 2f, 2f, 1.5f, 1.5f });

        // ---- Table header style ----
        com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10,
                com.itextpdf.text.Font.BOLD, BaseColor.WHITE);

        // ---- Write table headers ----
        String[] headers = {
                "Student ID", "First Name", "Last Name", "DOB", "Class", "Score"
        };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new BaseColor(0, 51, 102));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // ---- Table data style ----
        com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 9);

        // ---- Write data rows ----
        boolean alternate = false;
        for (Student s : students) {
            BaseColor rowColor = alternate
                    ? new BaseColor(240, 240, 240)
                    : BaseColor.WHITE;

            String[] rowData = {
                    String.valueOf(s.getStudentId()),
                    s.getFirstName(),
                    s.getLastName(),
                    s.getDob().toString(),
                    s.getStudentClass(),
                    String.valueOf(s.getScore())
            };

            for (String value : rowData) {
                PdfPCell cell = new PdfPCell(new Phrase(value, dataFont));
                cell.setBackgroundColor(rowColor);
                cell.setPadding(5);
                table.addCell(cell);
            }

            alternate = !alternate; // Alternate row color
        }

        document.add(table);
        document.close();

        return outputStream.toByteArray();
    }
}
