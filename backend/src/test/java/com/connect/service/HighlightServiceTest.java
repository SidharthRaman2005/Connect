package com.connect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.connect.dto.HighlightDto;
import com.connect.dto.SaveHighlightRequest;
import com.connect.entity.Highlight;
import com.connect.entity.HighlightStatus;
import com.connect.entity.Student;
import com.connect.exception.ForbiddenException;
import com.connect.repository.HighlightRepository;
import com.connect.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class HighlightServiceTest {

    @Mock
    private HighlightRepository highlightRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private HighlightService highlightService;

    private Student student(Long id) {
        Student s = new Student();
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    private Highlight highlight(Long id, Student owner, String title) {
        Highlight h = new Highlight(owner);
        ReflectionTestUtils.setField(h, "id", id);
        h.setTitle(title);
        h.setStatus(HighlightStatus.COMPLETED);
        return h;
    }

    private SaveHighlightRequest request(String title) {
        return new SaveHighlightRequest(title, "desc",
                List.of("Badminton", " ", "Badminton", "National"), "  ", HighlightStatus.COMPLETED);
    }

    @Test
    void createSavesForOwnerAndCleansTags() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L)));
        when(highlightRepository.save(any(Highlight.class))).thenAnswer(inv -> inv.getArgument(0));

        HighlightDto dto = highlightService.create(1L, request("National Badminton Title"));

        assertThat(dto.title()).isEqualTo("National Badminton Title");
        assertThat(dto.ownerId()).isEqualTo(1L);
        assertThat(dto.tags()).containsExactly("Badminton", "National"); // trimmed, de-duped
        assertThat(dto.link()).isNull(); // blank -> null
    }

    @Test
    void updateByNonOwnerIsForbidden() {
        when(highlightRepository.findById(5L)).thenReturn(Optional.of(highlight(5L, student(1L), "Theirs")));

        assertThatThrownBy(() -> highlightService.update(2L, 5L, request("Hacked")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateByOwnerAppliesFields() {
        when(highlightRepository.findById(5L)).thenReturn(Optional.of(highlight(5L, student(1L), "Old")));
        when(highlightRepository.save(any(Highlight.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(highlightService.update(1L, 5L, request("New")).title()).isEqualTo("New");
    }

    @Test
    void deleteByNonOwnerIsForbidden() {
        when(highlightRepository.findById(5L)).thenReturn(Optional.of(highlight(5L, student(1L), "Theirs")));

        assertThatThrownBy(() -> highlightService.delete(2L, 5L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteByOwnerRemoves() {
        Highlight h = highlight(5L, student(1L), "Mine");
        when(highlightRepository.findById(5L)).thenReturn(Optional.of(h));

        highlightService.delete(1L, 5L);

        verify(highlightRepository).delete(h);
    }

    @Test
    void getMyHighlightsReturnsOwners() {
        when(highlightRepository.findByOwnerIdOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of(highlight(5L, student(1L), "A"), highlight(6L, student(1L), "B")));

        assertThat(highlightService.getMyHighlights(1L)).extracting(HighlightDto::title)
                .containsExactly("A", "B");
    }
}
