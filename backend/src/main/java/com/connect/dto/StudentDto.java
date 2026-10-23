package com.connect.dto;

import com.connect.entity.Student;

/**
 * Public view of a {@link Student} returned to the frontend. Excludes internal
 * fields like the Google id and timestamps.
 */
public record StudentDto(
        Long id,
        String email,
        String name,
        String pictureUrl,
        boolean profileCompleted) {

    public static StudentDto from(Student student) {
        return new StudentDto(
                student.getId(),
                student.getEmail(),
                student.getName(),
                student.getPictureUrl(),
                student.isProfileCompleted());
    }
}
