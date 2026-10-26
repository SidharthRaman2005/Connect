package com.connect.dto;

import com.connect.entity.Student;

/**
 * A minimal student reference for embedding in other DTOs (collab owners,
 * applicants). No tags or relation — just enough to identify and link.
 */
public record StudentBrief(
        Long id,
        String name,
        String pictureUrl,
        String branch,
        Integer batch) {

    public static StudentBrief from(Student s) {
        return new StudentBrief(s.getId(), s.getName(), s.getPictureUrl(), s.getBranch(), s.getBatch());
    }
}
