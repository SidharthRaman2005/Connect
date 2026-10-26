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

import com.connect.dto.CollabPostDto;
import com.connect.dto.CollabRelation;
import com.connect.dto.CollabRequestDto;
import com.connect.entity.CollabPost;
import com.connect.entity.CollabRequest;
import com.connect.entity.CollabRequestStatus;
import com.connect.entity.CollabStatus;
import com.connect.entity.CollabType;
import com.connect.entity.Student;
import com.connect.exception.BadRequestException;
import com.connect.exception.ForbiddenException;
import com.connect.repository.CollabPostRepository;
import com.connect.repository.CollabRequestRepository;
import com.connect.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class CollabServiceTest {

    @Mock
    private CollabPostRepository postRepository;

    @Mock
    private CollabRequestRepository requestRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CollabService collabService;

    private Student student(Long id) {
        Student s = new Student();
        ReflectionTestUtils.setField(s, "id", id);
        s.setName("S" + id);
        return s;
    }

    private CollabPost post(Long id, Student owner, CollabStatus status) {
        CollabPost p = new CollabPost(owner, "Title", "desc", CollabType.PROJECT);
        ReflectionTestUtils.setField(p, "id", id);
        p.setStatus(status);
        return p;
    }

    private CollabRequest req(Long id, CollabPost post, Student applicant, CollabRequestStatus status) {
        CollabRequest r = new CollabRequest(post, applicant);
        ReflectionTestUtils.setField(r, "id", id);
        r.setStatus(status);
        return r;
    }

    @Test
    void requestToJoinRejectsOwnPost() {
        when(postRepository.findById(9L)).thenReturn(Optional.of(post(9L, student(1L), CollabStatus.OPEN)));

        assertThatThrownBy(() -> collabService.requestToJoin(1L, 9L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requestToJoinRejectsClosedPost() {
        when(postRepository.findById(9L)).thenReturn(Optional.of(post(9L, student(2L), CollabStatus.CLOSED)));

        assertThatThrownBy(() -> collabService.requestToJoin(1L, 9L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requestToJoinRejectsDuplicate() {
        when(postRepository.findById(9L)).thenReturn(Optional.of(post(9L, student(2L), CollabStatus.OPEN)));
        when(requestRepository.existsByPostIdAndApplicantId(9L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> collabService.requestToJoin(1L, 9L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requestToJoinCreatesPending() {
        when(postRepository.findById(9L)).thenReturn(Optional.of(post(9L, student(2L), CollabStatus.OPEN)));
        when(requestRepository.existsByPostIdAndApplicantId(9L, 1L)).thenReturn(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L)));
        when(requestRepository.save(any(CollabRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CollabRequestDto dto = collabService.requestToJoin(1L, 9L);

        assertThat(dto.status()).isEqualTo(CollabRequestStatus.PENDING);
        assertThat(dto.applicant().id()).isEqualTo(1L);
    }

    @Test
    void acceptByNonOwnerIsForbidden() {
        CollabPost p = post(9L, student(2L), CollabStatus.OPEN);
        when(requestRepository.findById(5L)).thenReturn(Optional.of(req(5L, p, student(1L), CollabRequestStatus.PENDING)));

        assertThatThrownBy(() -> collabService.accept(3L, 5L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void acceptMarksAcceptedAndUnseen() {
        CollabPost p = post(9L, student(1L), CollabStatus.OPEN);
        CollabRequest r = req(5L, p, student(2L), CollabRequestStatus.PENDING);
        r.setSeen(true);
        when(requestRepository.findById(5L)).thenReturn(Optional.of(r));
        when(requestRepository.save(any(CollabRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CollabRequestDto dto = collabService.accept(1L, 5L);

        assertThat(dto.status()).isEqualTo(CollabRequestStatus.ACCEPTED);
        assertThat(r.isSeen()).isFalse(); // reset so the applicant is notified
    }

    @Test
    void closeByNonOwnerIsForbidden() {
        when(postRepository.findById(9L)).thenReturn(Optional.of(post(9L, student(1L), CollabStatus.OPEN)));

        assertThatThrownBy(() -> collabService.close(2L, 9L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void feedTagsViewerRelation() {
        Student me = student(1L);
        when(postRepository.findTop50ByStatusOrderByCreatedAtDesc(CollabStatus.OPEN))
                .thenReturn(List.of(post(9L, me, CollabStatus.OPEN), post(10L, student(2L), CollabStatus.OPEN)));
        when(requestRepository.findByApplicantId(1L)).thenReturn(List.of());

        List<CollabPostDto> feed = collabService.feed(1L);

        assertThat(feed).extracting(CollabPostDto::relation)
                .containsExactly(CollabRelation.OWNER, CollabRelation.NONE);
    }
}
