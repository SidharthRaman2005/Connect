package com.connect.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.connect.entity.Tag;
import com.connect.entity.TagCategory;
import com.connect.entity.TagStatus;

@DataJpaTest
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Test
    void findsApprovedTagsByCategoryOrderedByName() {
        tagRepository.save(new Tag(TagCategory.SKILL, "Python", TagStatus.APPROVED));
        tagRepository.save(new Tag(TagCategory.SKILL, "C++", TagStatus.APPROVED));
        tagRepository.save(new Tag(TagCategory.SKILL, "Hidden", TagStatus.PENDING));
        tagRepository.save(new Tag(TagCategory.CLUB, "IEEE", TagStatus.APPROVED));

        var skills = tagRepository.findByCategoryAndStatusOrderByNameAsc(
                TagCategory.SKILL, TagStatus.APPROVED);

        assertThat(skills).extracting(Tag::getName).containsExactly("C++", "Python");
    }

    @Test
    void findsAllApprovedTagsOrderedByCategoryThenName() {
        tagRepository.save(new Tag(TagCategory.CLUB, "IEEE", TagStatus.APPROVED));
        tagRepository.save(new Tag(TagCategory.SKILL, "Python", TagStatus.APPROVED));
        tagRepository.save(new Tag(TagCategory.SKILL, "C++", TagStatus.APPROVED));
        tagRepository.save(new Tag(TagCategory.INTEREST, "Robotics", TagStatus.REJECTED));

        var approved = tagRepository.findByStatusOrderByCategoryAscNameAsc(TagStatus.APPROVED);

        // category is stored as a STRING, so it sorts alphabetically: CLUB before
        // SKILL; names sort within each category. REJECTED is excluded.
        assertThat(approved).extracting(Tag::getName).containsExactly("IEEE", "C++", "Python");
    }

    @Test
    void existsByCategoryAndNameIsCaseInsensitive() {
        tagRepository.save(new Tag(TagCategory.SKILL, "Java", TagStatus.APPROVED));

        assertThat(tagRepository.existsByCategoryAndNameIgnoreCase(TagCategory.SKILL, "java")).isTrue();
        assertThat(tagRepository.existsByCategoryAndNameIgnoreCase(TagCategory.SKILL, "JAVA")).isTrue();
        assertThat(tagRepository.existsByCategoryAndNameIgnoreCase(TagCategory.INTEREST, "Java")).isFalse();
    }

    @Test
    void enforcesUniqueNameWithinCategory() {
        tagRepository.saveAndFlush(new Tag(TagCategory.SKILL, "Java", TagStatus.APPROVED));

        assertThatThrownBy(() ->
                tagRepository.saveAndFlush(new Tag(TagCategory.SKILL, "Java", TagStatus.PENDING)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsSameNameInDifferentCategories() {
        tagRepository.saveAndFlush(new Tag(TagCategory.SKILL, "Design", TagStatus.APPROVED));

        // Same name under a different category is allowed.
        Tag interest = tagRepository.saveAndFlush(
                new Tag(TagCategory.INTEREST, "Design", TagStatus.APPROVED));

        assertThat(interest.getId()).isNotNull();
    }
}
