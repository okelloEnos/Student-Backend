package com.okelloSoftwarez.studentapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {

    private Long id;
    private Long studentId;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String studentClass;
    private Integer score;
}