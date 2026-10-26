package com.connect.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connect.dto.CollabNotificationDto;
import com.connect.dto.CollabPostDto;
import com.connect.dto.CollabRelation;
import com.connect.dto.CollabRequestDto;
import com.connect.dto.CreateCollabRequest;
import com.connect.dto.OwnerCollabPostDto;
import com.connect.entity.CollabPost;
import com.connect.entity.CollabRequest;
import com.connect.entity.CollabRequestStatus;
import com.connect.entity.CollabStatus;
import com.connect.entity.Student;
import com.connect.exception.BadRequestException;
import com.connect.exception.ForbiddenException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.CollabPostRepository;
import com.connect.repository.CollabRequestRepository;
import com.connect.repository.StudentRepository;

/**
 * Collaboration recruitment (M4.2): publishing posts, browsing the feed,
 * requesting to join, the owner accepting members, and closing posts. Accepting
 * a request marks it unseen so the accepted student can be notified.
 */
@Service
public class CollabService {

    private final CollabPostRepository postRepository;
    private final CollabRequestRepository requestRepository;
    private final StudentRepository studentRepository;

    public CollabService(CollabPostRepository postRepository,
                         CollabRequestRepository requestRepository,
                         StudentRepository studentRepository) {
        this.postRepository = postRepository;
        this.requestRepository = requestRepository;
        this.studentRepository = studentRepository;
    }

    /** Recent open posts, each tagged with the viewer's relation to it. */
    @Transactional(readOnly = true)
    public List<CollabPostDto> feed(Long viewerId) {
        Map<Long, CollabRequestStatus> myRequests = new HashMap<>();
        for (CollabRequest r : requestRepository.findByApplicantId(viewerId)) {
            myRequests.put(r.getPost().getId(), r.getStatus());
        }
        return postRepository.findTop50ByStatusOrderByCreatedAtDesc(CollabStatus.OPEN).stream()
                .map(p -> CollabPostDto.of(p, relationFor(p, viewerId, myRequests)))
                .toList();
    }

    @Transactional
    public CollabPostDto create(Long ownerId, CreateCollabRequest request) {
        Student owner = loadStudent(ownerId);
        CollabPost post = new CollabPost(owner,
                request.title().trim(),
                blankToNull(request.description()),
                request.type());
        return CollabPostDto.of(postRepository.save(post), CollabRelation.OWNER);
    }

    /** The caller's posts with their join requests, for management. */
    @Transactional(readOnly = true)
    public List<OwnerCollabPostDto> myPosts(Long ownerId) {
        return postRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(p -> {
                    List<CollabRequestDto> requests = requestRepository
                            .findByPostIdOrderByCreatedAtDesc(p.getId()).stream()
                            .map(CollabRequestDto::from)
                            .toList();
                    int accepted = requestRepository.countByPostIdAndStatus(
                            p.getId(), CollabRequestStatus.ACCEPTED);
                    return OwnerCollabPostDto.of(p, requests, accepted);
                })
                .toList();
    }

    @Transactional
    public OwnerCollabPostDto close(Long ownerId, Long postId) {
        CollabPost post = loadOwnedPost(ownerId, postId);
        post.setStatus(CollabStatus.CLOSED);
        postRepository.save(post);
        List<CollabRequestDto> requests = requestRepository
                .findByPostIdOrderByCreatedAtDesc(post.getId()).stream()
                .map(CollabRequestDto::from)
                .toList();
        int accepted = requestRepository.countByPostIdAndStatus(post.getId(), CollabRequestStatus.ACCEPTED);
        return OwnerCollabPostDto.of(post, requests, accepted);
    }

    /** Sends a request to join a post. */
    @Transactional
    public CollabRequestDto requestToJoin(Long applicantId, Long postId) {
        CollabPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Collab post not found."));
        if (post.getStatus() != CollabStatus.OPEN) {
            throw new BadRequestException("This collab is closed.");
        }
        if (post.getOwner().getId().equals(applicantId)) {
            throw new BadRequestException("You cannot request to join your own collab.");
        }
        if (requestRepository.existsByPostIdAndApplicantId(postId, applicantId)) {
            throw new BadRequestException("You have already requested to join this collab.");
        }
        Student applicant = loadStudent(applicantId);
        return CollabRequestDto.from(requestRepository.save(new CollabRequest(post, applicant)));
    }

    /** Owner accepts a join request, adding the applicant to the collab. */
    @Transactional
    public CollabRequestDto accept(Long ownerId, Long requestId) {
        CollabRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found."));
        if (!request.getPost().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("Only the collab owner can accept requests.");
        }
        if (request.getStatus() != CollabRequestStatus.PENDING) {
            throw new BadRequestException("This request has already been handled.");
        }
        request.setStatus(CollabRequestStatus.ACCEPTED);
        request.setSeen(false); // so the applicant is notified
        return CollabRequestDto.from(requestRepository.save(request));
    }

    /** The caller's "you're in" notifications (accepted, not yet seen). */
    @Transactional(readOnly = true)
    public List<CollabNotificationDto> notifications(Long meId) {
        return requestRepository
                .findByApplicantIdAndStatusAndSeenFalse(meId, CollabRequestStatus.ACCEPTED).stream()
                .map(CollabNotificationDto::from)
                .toList();
    }

    /** Marks all of the caller's collab notifications as seen. */
    @Transactional
    public void markNotificationsSeen(Long meId) {
        List<CollabRequest> unseen = requestRepository
                .findByApplicantIdAndStatusAndSeenFalse(meId, CollabRequestStatus.ACCEPTED);
        unseen.forEach(r -> r.setSeen(true));
        requestRepository.saveAll(unseen);
    }

    private CollabRelation relationFor(CollabPost post, Long viewerId,
                                       Map<Long, CollabRequestStatus> myRequests) {
        if (post.getOwner().getId().equals(viewerId)) {
            return CollabRelation.OWNER;
        }
        CollabRequestStatus mine = myRequests.get(post.getId());
        if (mine == CollabRequestStatus.ACCEPTED) {
            return CollabRelation.MEMBER;
        }
        if (mine == CollabRequestStatus.PENDING) {
            return CollabRelation.REQUESTED;
        }
        return CollabRelation.NONE;
    }

    private CollabPost loadOwnedPost(Long ownerId, Long postId) {
        CollabPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Collab post not found."));
        if (!post.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You can only manage your own collabs.");
        }
        return post;
    }

    private Student loadStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
