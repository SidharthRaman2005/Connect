package com.connect.dto;

import java.time.Instant;
import com.connect.entity.StudentReport;

public record StudentReportDto(Long id, StudentBrief target, String reason, String details, String status, Instant createdAt) {
    public static StudentReportDto from(StudentReport report) {
        return new StudentReportDto(report.getId(), StudentBrief.from(report.getTarget()), report.getReason(),
                report.getDetails(), report.getStatus().name(), report.getCreatedAt());
    }
}
