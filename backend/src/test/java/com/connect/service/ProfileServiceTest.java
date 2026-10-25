package com.connect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.connect.dto.ProfileDto;
import com.connect.dto.PublicProfileDto;
import com.connect.dto.UpdateProfileRequest;
import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;
import com.connect.entity.Tag;
import com.connect.entity.TagCategory;
import com.connect.entity.TagStatus;
import com.connect.exception.BadRequestException;
import com.connect.exception.ForbiddenException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.StudentRepository;
import com.connect.repository.TagRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private ProfileService profileService;

    /** A fully populated student with the given id and visibility. */
    private Student student(Long id, ProfileVisibility visibility) {
        Student s = new Student();
        ReflectionTestUtils.setField(s, "id", id); // no setter; id is DB-generated
        s.setEmail("26mic7142@vitapstudent.ac.in");
        s.setName("Test Student");
        s.setBatch(26);
        s.setBranch("MIC");
        s.setBio("hi");
        s.setProfileVisibility(visibility);
        return s;
    }

    private Tag tag(Long id, TagCategory category, String name, TagStatus status) {
        Tag t = new Tag(category, name, status);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    @Test
    void getOwnProfileReturnsFullProfileIncludingEmailAndVisibility() {
        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(student(1L, ProfileVisibility.PRIVATE)));

        ProfileDto dto = profileService.getOwnProfile(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.email()).isEqualTo("26mic7142@vitapstudent.ac.in");
        assertThat(dto.batch()).isEqualTo(26);
        assertThat(dto.visibility()).isEqualTo(ProfileVisibility.PRIVATE);
    }

    @Test
    void updateProfileTrimsBranchAppliesFieldsAndCompletesOnboarding() {
        Student existing = student(1L, ProfileVisibility.PUBLIC);
        existing.setProfileCompleted(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileDto dto = profileService.updateProfile(1L,
                new UpdateProfileRequest("  CSE  ", "my new bio", ProfileVisibility.PRIVATE));

        assertThat(dto.branch()).isEqualTo("CSE"); // trimmed
        assertThat(dto.bio()).isEqualTo("my new bio");
        assertThat(dto.visibility()).isEqualTo(ProfileVisibility.PRIVATE);
        assertThat(dto.profileCompleted()).isTrue();
    }

    @Test
    void updatePictureSetsTheUrl() {
        Student existing = student(1L, ProfileVisibility.PUBLIC);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileDto dto = profileService.updatePicture(1L, "/uploads/abc.png");

        assertThat(dto.pictureUrl()).isEqualTo("/uploads/abc.png");
    }

    @Test
    void clearPictureRemovesTheUrl() {
        Student existing = student(1L, ProfileVisibility.PUBLIC);
        existing.setPictureUrl("/uploads/abc.png");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileDto dto = profileService.clearPicture(1L);

        assertThat(dto.pictureUrl()).isNull();
    }

    @Test
    void updateTagsReplacesTagsWithApprovedOnes() {
        Student existing = student(1L, ProfileVisibility.PUBLIC);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tagRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(
                tag(10L, TagCategory.SKILL, "Java", TagStatus.APPROVED),
                tag(20L, TagCategory.CLUB, "IEEE", TagStatus.APPROVED)));

        ProfileDto dto = profileService.updateTags(1L, List.of(10L, 20L));

        assertThat(dto.tags()).extracting("name").containsExactly("Java", "IEEE");
    }

    @Test
    void updateTagsWithEmptyListClearsTags() {
        Student existing = student(1L, ProfileVisibility.PUBLIC);
        existing.getTags().add(tag(10L, TagCategory.SKILL, "Java", TagStatus.APPROVED));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileDto dto = profileService.updateTags(1L, List.of());

        assertThat(dto.tags()).isEmpty();
    }

    @Test
    void updateTagsRejectsUnknownTagId() {
        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(student(1L, ProfileVisibility.PUBLIC)));
        // Only one of the two requested ids exists.
        when(tagRepository.findAllById(List.of(10L, 99L)))
                .thenReturn(List.of(tag(10L, TagCategory.SKILL, "Java", TagStatus.APPROVED)));

        assertThatThrownBy(() -> profileService.updateTags(1L, List.of(10L, 99L)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateTagsRejectsUnapprovedTag() {
        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(student(1L, ProfileVisibility.PUBLIC)));
        when(tagRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(tag(10L, TagCategory.SKILL, "Pending", TagStatus.PENDING)));

        assertThatThrownBy(() -> profileService.updateTags(1L, List.of(10L)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getProfileReturnsPublicViewForPublicProfile() {
        when(studentRepository.findById(2L))
                .thenReturn(Optional.of(student(2L, ProfileVisibility.PUBLIC)));

        PublicProfileDto dto = profileService.getProfile(1L, 2L);

        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.name()).isEqualTo("Test Student");
        assertThat(dto.branch()).isEqualTo("MIC");
    }

    @Test
    void getProfileHidesPrivateProfileFromOtherStudents() {
        when(studentRepository.findById(2L))
                .thenReturn(Optional.of(student(2L, ProfileVisibility.PRIVATE)));

        assertThatThrownBy(() -> profileService.getProfile(1L, 2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getProfileLetsOwnerViewTheirOwnPrivateProfile() {
        when(studentRepository.findById(2L))
                .thenReturn(Optional.of(student(2L, ProfileVisibility.PRIVATE)));

        PublicProfileDto dto = profileService.getProfile(2L, 2L);

        assertThat(dto.id()).isEqualTo(2L);
    }

    @Test
    void getProfileTreatsNullVisibilityAsPublicForLegacyRows() {
        when(studentRepository.findById(2L))
                .thenReturn(Optional.of(student(2L, null)));

        PublicProfileDto dto = profileService.getProfile(1L, 2L);

        assertThat(dto.id()).isEqualTo(2L);
    }

    @Test
    void getProfileThrowsWhenStudentMissing() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
