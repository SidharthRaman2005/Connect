package com.connect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connect.dto.TagDto;
import com.connect.entity.TagCategory;
import com.connect.entity.TagStatus;
import com.connect.repository.TagRepository;

/**
 * Serves the approved tag catalog students choose from (M2.2).
 *
 * <p>Reads only expose {@link TagStatus#APPROVED} tags. Creating and approving
 * tags is an admin concern handled in M6.1.
 */
@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /** Every approved tag, across all categories, ordered by category then name. */
    @Transactional(readOnly = true)
    public List<TagDto> getApprovedTags() {
        return tagRepository.findByStatusOrderByCategoryAscNameAsc(TagStatus.APPROVED)
                .stream()
                .map(TagDto::from)
                .toList();
    }

    /** Approved tags in a single category, ordered by name. */
    @Transactional(readOnly = true)
    public List<TagDto> getApprovedTags(TagCategory category) {
        return tagRepository.findByCategoryAndStatusOrderByNameAsc(category, TagStatus.APPROVED)
                .stream()
                .map(TagDto::from)
                .toList();
    }
}
