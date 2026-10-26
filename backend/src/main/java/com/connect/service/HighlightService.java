package com.connect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connect.dto.HighlightDto;
import com.connect.dto.SaveHighlightRequest;
import com.connect.entity.Highlight;
import com.connect.entity.Student;
import com.connect.exception.ForbiddenException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.HighlightRepository;
import com.connect.repository.StudentRepository;

/**
 * Manages a student's highlights (M4.1). A student may only create, edit, and
 * delete their own; anyone signed in may view a student's highlights.
 */
@Service
public class HighlightService {

    private static final int MAX_TAGS = 20;

    private final HighlightRepository highlightRepository;
    private final StudentRepository studentRepository;

    public HighlightService(HighlightRepository highlightRepository,
                            StudentRepository studentRepository) {
        this.highlightRepository = highlightRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<HighlightDto> getMyHighlights(Long ownerId) {
        return highlightRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId)
                .stream().map(HighlightDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<HighlightDto> getHighlightsForStudent(Long studentId) {
        return highlightRepository.findByOwnerIdOrderByUpdatedAtDesc(studentId)
                .stream().map(HighlightDto::from).toList();
    }

    @Transactional
    public HighlightDto create(Long ownerId, SaveHighlightRequest request) {
        Student owner = studentRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        Highlight highlight = new Highlight(owner);
        apply(highlight, request);
        return HighlightDto.from(highlightRepository.save(highlight));
    }

    @Transactional
    public HighlightDto update(Long ownerId, Long highlightId, SaveHighlightRequest request) {
        Highlight highlight = loadOwned(ownerId, highlightId);
        apply(highlight, request);
        return HighlightDto.from(highlightRepository.save(highlight));
    }

    @Transactional
    public void delete(Long ownerId, Long highlightId) {
        highlightRepository.delete(loadOwned(ownerId, highlightId));
    }

    private Highlight loadOwned(Long ownerId, Long highlightId) {
        Highlight highlight = highlightRepository.findById(highlightId)
                .orElseThrow(() -> new ResourceNotFoundException("Highlight not found."));
        if (!highlight.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You can only modify your own highlights.");
        }
        return highlight;
    }

    private void apply(Highlight highlight, SaveHighlightRequest request) {
        highlight.setTitle(request.title().trim());
        highlight.setDescription(blankToNull(request.description()));
        highlight.setLink(blankToNull(request.link()));
        highlight.setStatus(request.status());
        highlight.setTags(cleanTags(request.tags()));
    }

    private static List<String> cleanTags(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .map(t -> t.length() > 60 ? t.substring(0, 60) : t)
                .distinct()
                .limit(MAX_TAGS)
                .toList();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
