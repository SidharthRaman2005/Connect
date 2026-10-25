package com.connect.dto;

import java.util.List;

import com.connect.entity.Student;

/**
 * A student's profile as seen by <em>other</em> students. Excludes private
 * fields such as the email address and the visibility setting itself.
 */
public record PublicProfileDto(
        Long id,
        String name,
        String pictureUrl,
        Integer batch,
        String branch,
        String bio,
        List<TagDto> tags) {

    public static PublicProfileDto from(Student student) {
        return new PublicProfileDto(
                student.getId(),
                student.getName(),
                student.getPictureUrl(),
                student.getBatch(),
                student.getBranch(),
                student.getBio(),
                TagDto.sortedListFrom(student.getTags()));
    }
}
