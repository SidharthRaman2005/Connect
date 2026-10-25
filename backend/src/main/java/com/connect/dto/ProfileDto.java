package com.connect.dto;

import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;

/**
 * The authenticated student's own, full profile. Includes fields only the owner
 * should see (email, visibility setting). Other students are served
 * {@link PublicProfileDto} instead.
 */
public record ProfileDto(
        Long id,
        String email,
        String name,
        String pictureUrl,
        Integer batch,
        String branch,
        String bio,
        ProfileVisibility visibility,
        boolean profileCompleted) {

    public static ProfileDto from(Student student) {
        return new ProfileDto(
                student.getId(),
                student.getEmail(),
                student.getName(),
                student.getPictureUrl(),
                student.getBatch(),
                student.getBranch(),
                student.getBio(),
                student.getProfileVisibility(),
                student.isProfileCompleted());
    }
}
