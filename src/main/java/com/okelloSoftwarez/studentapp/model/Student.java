package com.okelloSoftwarez.studentapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long studentId;

    @Column(nullable = false, length = 8)
    private String firstName;

    @Column(nullable = false, length = 8)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dob;

    @Column(nullable = false, name = "student_class", length = 10)
    private String studentClass;

    @Column(nullable = false)
    private Integer score;
}