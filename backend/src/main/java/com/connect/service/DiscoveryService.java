package com.connect.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

/**
 * Student discovery and search (M3.1).
 *
 * <p>Only PUBLIC profiles (other than the caller) are surfaced; PRIVATE students
 * stay hidden. The discovery feed ranks candidates by how many tags they share
 * with the caller. Filtering and ranking run in-memory over the candidate set —
 * fine at current scale; move ranking into the query when the directory grows.
 */
@Service
public class DiscoveryService {

    /** Max students returned by the discovery feed. */
    private static final int FEED_LIMIT = 50;

    private final StudentRepository studentRepository;
    private final ConnectionService connectionService;

    public DiscoveryService(StudentRepository studentRepository,
                            ConnectionService connectionService) {
        this.studentRepository = studentRepository;
        this.connectionService = connectionService;
    }

    /** Suggested students, ranked by shared tags then name. */
    @Transactional(readOnly = true)
    public List<StudentCardDto> discover(Long meId) {
        Student me = load(meId);
        Set<Long> myTagIds = tagIds(me);
        Map<Long, RelationState> relations = connectionService.relationsFor(meId);

        return candidates(meId).stream()
                .map(s -> card(s, myTagIds, relations))
                .sorted(cardOrder())
                .limit(FEED_LIMIT)
                .toList();
    }

    /** Filtered, paginated people search. All filters are optional. */
    @Transactional(readOnly = true)
    public PageResponse<StudentCardDto> search(Long meId, String q, String branch,
                                               Integer batch, List<Long> tagIds,
                                               int page, int size) {
        Student me = load(meId);
        Set<Long> myTagIds = tagIds(me);
        Map<Long, RelationState> relations = connectionService.relationsFor(meId);

        String query = q == null ? null : q.trim().toLowerCase();
        Set<Long> wantedTags = tagIds == null ? Set.of() : Set.copyOf(tagIds);

        List<StudentCardDto> matches = candidates(meId).stream()
                .filter(s -> query == null || query.isEmpty()
                        || (s.getName() != null && s.getName().toLowerCase().contains(query)))
                .filter(s -> branch == null || branch.isBlank()
                        || (s.getBranch() != null && s.getBranch().equalsIgnoreCase(branch.trim())))
                .filter(s -> batch == null || batch.equals(s.getBatch()))
                .filter(s -> wantedTags.isEmpty()
                        || s.getTags().stream().anyMatch(t -> wantedTags.contains(t.getId())))
                .map(s -> card(s, myTagIds, relations))
                .sorted(cardOrder())
                .toList();

        int safeSize = size <= 0 ? 20 : size;
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, matches.size());
        int to = Math.min(from + safeSize, matches.size());
        return PageResponse.of(matches.subList(from, to), safePage, safeSize, matches.size());
    }

    private List<Student> candidates(Long meId) {
        return studentRepository.findByProfileVisibilityAndIdNot(ProfileVisibility.PUBLIC, meId);
    }

    private StudentCardDto card(Student s, Set<Long> myTagIds, Map<Long, RelationState> relations) {
        int shared = (int) s.getTags().stream().filter(t -> myTagIds.contains(t.getId())).count();
        return StudentCardDto.of(s, shared, relations.getOrDefault(s.getId(), RelationState.NONE));
    }

    private static Comparator<StudentCardDto> cardOrder() {
        return Comparator.comparingInt(StudentCardDto::sharedTagCount).reversed()
                .thenComparing(c -> c.name() == null ? "" : c.name(), String.CASE_INSENSITIVE_ORDER);
    }

    private static Set<Long> tagIds(Student s) {
        return s.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
    }

    private Student load(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }
}
