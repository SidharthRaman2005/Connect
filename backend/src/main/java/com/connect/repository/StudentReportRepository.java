package com.connect.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.connect.entity.StudentReport;

public interface StudentReportRepository extends JpaRepository<StudentReport, Long> {
    List<StudentReport> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
}
