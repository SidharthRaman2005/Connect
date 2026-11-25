package com.connect.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connect.dto.PageResponse;
import com.connect.dto.RelationState;
import com.connect.dto.StudentCardDto;
import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;
import com.connect.entity.Tag;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.StudentRepository;

/** Database-paged student recommendations and search. */
@Service
public class DiscoveryService {
    private static final Long NO_TAG_SENTINEL = -1L;
    private final StudentRepository studentRepository;
    private final ConnectionService connectionService;
    private final UserSafetyService safetyService;

    public DiscoveryService(StudentRepository studentRepository, ConnectionService connectionService, UserSafetyService safetyService) {
        this.studentRepository = studentRepository;
        this.connectionService = connectionService;
        this.safetyService = safetyService;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentCardDto> discover(Long meId, int page, int size) {
        return findCards(meId, "", "", null, List.of(), page, size);
    }

    /** Kept for internal callers that deliberately need a single bounded page. */
    @Transactional(readOnly = true)
    public List<StudentCardDto> discover(Long meId) {
        return discover(meId, 0, 50).content();
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentCardDto> search(Long meId, String query, String branch,
                                               Integer batch, List<Long> tagIds,
                                               int page, int size) {
        return findCards(meId, query, branch, batch, tagIds, page, size);
    }

    private PageResponse<StudentCardDto> findCards(Long meId, String query, String branch,
                                                   Integer batch, List<Long> tagIds,
                                                   int page, int size) {
        Student me = studentRepository.findById(meId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        Set<Long> myTags = me.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
        Set<Long> wantedTags = tagIds == null ? Set.of() : tagIds.stream().filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> queryTags = wantedTags.isEmpty() ? Set.of(NO_TAG_SENTINEL) : wantedTags;
        Set<Long> rankTags = myTags.isEmpty() ? Set.of(NO_TAG_SENTINEL) : myTags;
        Set<Long> excludedIds = safetyService.blockedIds(meId).stream().collect(Collectors.toSet());
        if (excludedIds.isEmpty()) excludedIds = Set.of(NO_TAG_SENTINEL);

        Page<Long> idPage = studentRepository.findDiscoveryIds(meId, ProfileVisibility.PUBLIC,
                normalize(query), normalize(branch), batch, !wantedTags.isEmpty(), queryTags, excludedIds, rankTags,
                PageRequest.of(page, size));
        List<Long> ids = idPage.getContent();
        Map<Long, Student> byId = new HashMap<>();
        if (!ids.isEmpty()) studentRepository.findAllWithTagsByIdIn(ids).forEach(student -> byId.put(student.getId(), student));
        Map<Long, RelationState> relations = connectionService.relationsFor(meId, ids);
        List<StudentCardDto> cards = ids.stream().map(byId::get).filter(student -> student != null)
                .map(student -> {
                    int shared = (int) student.getTags().stream().filter(tag -> myTags.contains(tag.getId())).count();
                    return StudentCardDto.of(student, shared, relations.getOrDefault(student.getId(), RelationState.NONE));
                }).toList();
        return PageResponse.of(cards, page, size, idPage.getTotalElements());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
