package com.connect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.connect.dto.TagDto;
import com.connect.entity.Tag;
import com.connect.entity.TagCategory;
import com.connect.entity.TagStatus;
import com.connect.repository.TagRepository;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private Tag tag(Long id, TagCategory category, String name) {
        Tag t = new Tag(category, name, TagStatus.APPROVED);
        ReflectionTestUtils.setField(t, "id", id); // no setter; id is DB-generated
        return t;
    }

    @Test
    void getApprovedTagsReturnsOnlyApprovedMappedToDtos() {
        when(tagRepository.findByStatusOrderByCategoryAscNameAsc(TagStatus.APPROVED))
                .thenReturn(List.of(
                        tag(1L, TagCategory.SKILL, "Java"),
                        tag(2L, TagCategory.CLUB, "IEEE")));

        List<TagDto> tags = tagService.getApprovedTags();

        assertThat(tags).extracting(TagDto::name).containsExactly("Java", "IEEE");
        assertThat(tags).extracting(TagDto::category)
                .containsExactly(TagCategory.SKILL, TagCategory.CLUB);
        assertThat(tags).extracting(TagDto::id).containsExactly(1L, 2L);
    }

    @Test
    void getApprovedTagsByCategoryQueriesThatCategory() {
        when(tagRepository.findByCategoryAndStatusOrderByNameAsc(TagCategory.SKILL, TagStatus.APPROVED))
                .thenReturn(List.of(
                        tag(1L, TagCategory.SKILL, "C++"),
                        tag(2L, TagCategory.SKILL, "Python")));

        List<TagDto> tags = tagService.getApprovedTags(TagCategory.SKILL);

        assertThat(tags).extracting(TagDto::name).containsExactly("C++", "Python");
        assertThat(tags).allMatch(t -> t.category() == TagCategory.SKILL);
    }

    @Test
    void getApprovedTagsReturnsEmptyListWhenNoneApproved() {
        when(tagRepository.findByStatusOrderByCategoryAscNameAsc(TagStatus.APPROVED))
                .thenReturn(List.of());

        assertThat(tagService.getApprovedTags()).isEmpty();
    }
}
