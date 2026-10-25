package com.connect.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connect.dto.TagDto;
import com.connect.entity.TagCategory;
import com.connect.service.TagService;

/**
 * Read-only access to the approved tag catalog (M2.2).
 *
 * <p>Students use this to populate skill/interest/club pickers. There is no
 * create endpoint by design — tags are admin-curated (M6.1), not user-defined.
 * Requires a valid session JWT.
 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    /**
     * Approved tags, optionally narrowed to one category via {@code ?category=}.
     * An unrecognised category value yields a 400.
     */
    @GetMapping
    public List<TagDto> listTags(@RequestParam(required = false) TagCategory category) {
        return category == null
                ? tagService.getApprovedTags()
                : tagService.getApprovedTags(category);
    }
}
