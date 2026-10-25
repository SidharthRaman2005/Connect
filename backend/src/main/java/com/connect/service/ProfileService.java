package com.connect.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connect.dto.ProfileDto;
import com.connect.dto.PublicProfileDto;
import com.connect.dto.UpdateProfileRequest;
import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;
import com.connect.exception.ForbiddenException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.StudentRepository;

/**
 * Reads and updates student profiles, enforcing per-profile visibility.
 */
@Service
public class ProfileService {

    private final StudentRepository studentRepository;

    public ProfileService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
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

    private Student load(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }
}
