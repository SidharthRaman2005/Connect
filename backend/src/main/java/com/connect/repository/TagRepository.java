package com.connect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.connect.entity.Tag;
import com.connect.entity.TagCategory;
import com.connect.entity.TagStatus;

/**
 * Data access for {@link Tag}s.
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /** All tags in a moderation state, ordered for stable catalog display. */
    List<Tag> findByStatusOrderByCategoryAscNameAsc(TagStatus status);

    /** Tags in one category and moderation state, ordered by name. */
    List<Tag> findByCategoryAndStatusOrderByNameAsc(TagCategory category, TagStatus status);

    /** Whether a tag with this name already exists in the category (case-insensitive). */
    boolean existsByCategoryAndNameIgnoreCase(TagCategory category, String name);
}
