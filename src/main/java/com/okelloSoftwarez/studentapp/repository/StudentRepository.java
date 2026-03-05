package com.okelloSoftwarez.studentapp.repository;

import com.okelloSoftwarez.studentapp.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Find one student by studentId
    Optional<Student> findByStudentId(Long studentId);

    // Paginated results with optional filters
    @Query("SELECT s FROM Student s WHERE " +
            "(:studentId IS NULL OR s.studentId = :studentId) AND " +
            "(:studentClass IS NULL OR s.studentClass = :studentClass)")
    Page<Student> findWithFilters(
            @Param("studentId") Long studentId,
            @Param("studentClass") String studentClass,
            Pageable pageable);

    // Full list with same filters (used for exports)
    @Query("SELECT s FROM Student s WHERE " +
            "(:studentId IS NULL OR s.studentId = :studentId) AND " +
            "(:studentClass IS NULL OR s.studentClass = :studentClass)")
    List<Student> findAllWithFilters(
            @Param("studentId") Long studentId,
            @Param("studentClass") String studentClass);

    // Distinct class names for dropdown
    @Query("SELECT DISTINCT s.studentClass FROM Student s ORDER BY s.studentClass")
    List<String> findDistinctClasses();
}