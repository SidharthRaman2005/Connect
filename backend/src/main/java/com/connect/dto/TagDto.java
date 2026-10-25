package com.connect.dto;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.connect.entity.Tag;
import com.connect.entity.TagCategory;

/**
 * A tag as offered to students for selection. The moderation status is internal
 * and deliberately omitted — students only ever see approved tags.
 */
public record TagDto(
        Long id,
        String name,
        TagCategory category) {

    private static final Comparator<TagDto> BY_CATEGORY_THEN_NAME =
            Comparator.comparing(TagDto::category).thenComparing(TagDto::name);

    public static TagDto from(Tag tag) {
        return new TagDto(tag.getId(), tag.getName(), tag.getCategory());
    }

    /** Maps tags to DTOs in a stable order (category, then name). */
    public static List<TagDto> sortedListFrom(Collection<Tag> tags) {
        return tags.stream()
                .map(TagDto::from)
                .sorted(BY_CATEGORY_THEN_NAME)
                .toList();
    }
}
