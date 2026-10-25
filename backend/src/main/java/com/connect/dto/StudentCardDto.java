package com.connect.dto;

import java.util.List;

import com.connect.entity.Student;

/**
 * A compact student summary used in discovery feeds, search results, and
 * connection lists. Includes the viewer-relative connection {@link RelationState}
 * and, where relevant, how many tags are shared with the viewer.
 */
public record StudentCardDto(
        Long id,
        String name,
        String pictureUrl,
        Integer batch,
        String branch,
        List<TagDto> tags,
        int sharedTagCount,
        RelationState relation) {

    public static StudentCardDto of(Student student, int sharedTagCount, RelationState relation) {
        return new StudentCardDto(
                student.getId(),
                student.getName(),
                student.getPictureUrl(),
                student.getBatch(),
                student.getBranch(),
                TagDto.sortedListFrom(student.getTags()),
                sharedTagCount,
                relation);
    }
}
