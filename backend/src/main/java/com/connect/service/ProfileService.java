package com.connect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connect.dto.ProfileDto;
import com.connect.dto.PublicProfileDto;
import com.connect.dto.UpdateProfileRequest;
import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;
import com.connect.entity.Tag;
import com.connect.entity.TagStatus;
import com.connect.exception.BadRequestException;
import com.connect.exception.ForbiddenException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.StudentRepository;
import com.connect.repository.TagRepository;

/**
 * Reads and updates student profiles, enforcing per-profile visibility and the
 * approved-tags-only rule.
 */
@Service
public class ProfileService {

    private final StudentRepository studentRepository;
    private final TagRepository tagRepository;

    public ProfileService(StudentRepository studentRepository, TagRepository tagRepository) {
        this.studentRepository = studentRepository;
        this.tagRepository = tagRepository;
    }

    /** The authenticated student's own, full profile. */
    @Transactional(readOnly = true)
    public ProfileDto getOwnProfile(Long studentId) {
        return ProfileDto.from(load(studentId));
    }

    /**
     * Applies the editable profile fields and marks the profile complete, which
     * clears the first-time onboarding redirect.
     */
    @Transactional
    public ProfileDto updateProfile(Long studentId, UpdateProfileRequest request) {
        Student student = load(studentId);
        student.setBranch(request.branch().trim());
        student.setBio(request.bio());
        student.setProfileVisibility(request.visibility());
        student.setProfileCompleted(true);
        return ProfileDto.from(studentRepository.save(student));
    }

    /**
     * Replaces the caller's tag set with the given tags (M2.3). The id list is
     * the complete new set, so an empty list clears all tags.
     *
     * @throws BadRequestException if any id does not refer to an existing,
     *                             approved tag
     */
    @Transactional
    public ProfileDto updateTags(Long studentId, List<Long> tagIds) {
        Student student = load(studentId);

        if (tagIds == null || tagIds.isEmpty()) {
            student.getTags().clear();
            return ProfileDto.from(studentRepository.save(student));
        }

        List<Tag> found = tagRepository.findAllById(tagIds);
        boolean allApproved = found.stream().allMatch(t -> t.getStatus() == TagStatus.APPROVED);
        // findAllById drops unknown ids and de-duplicates, so a size mismatch (vs
        // the distinct requested ids) means at least one id was unknown.
        long distinctRequested = tagIds.stream().distinct().count();
        if (!allApproved || found.size() != distinctRequested) {
            throw new BadRequestException("One or more tags are invalid or not approved.");
        }

        student.getTags().clear();
        student.getTags().addAll(found);
        return ProfileDto.from(studentRepository.save(student));
    }

    /**
     * Another student's public profile. The owner can always see their own; a
     * PRIVATE profile is hidden from everyone else.
     *
     * @throws ForbiddenException       if the profile is private and the viewer
     *                                  is not its owner
     * @throws ResourceNotFoundException if no such student exists
     */
    @Transactional(readOnly = true)
    public PublicProfileDto getProfile(Long viewerId, Long targetId) {
        Student target = load(targetId);
        boolean isOwner = target.getId().equals(viewerId);
        if (!isOwner && target.getProfileVisibility() == ProfileVisibility.PRIVATE) {
            throw new ForbiddenException("This profile is private.");
        }
        return PublicProfileDto.from(target);
    }

    /** Sets the caller's profile picture to an already-stored URL/path. */
    @Transactional
    public ProfileDto updatePicture(Long studentId, String url) {
        Student student = load(studentId);
        student.setPictureUrl(url);
        return ProfileDto.from(studentRepository.save(student));
    }

    /** Removes the caller's profile picture. */
    @Transactional
    public ProfileDto clearPicture(Long studentId) {
        Student student = load(studentId);
        student.setPictureUrl(null);
        return ProfileDto.from(studentRepository.save(student));
    }

    private Student load(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }
}
