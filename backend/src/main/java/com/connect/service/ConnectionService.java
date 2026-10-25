package com.connect.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connect.dto.ConnectionDto;
import com.connect.dto.RelationState;
import com.connect.entity.Connection;
import com.connect.entity.ConnectionStatus;
import com.connect.entity.Student;
import com.connect.exception.BadRequestException;
import com.connect.exception.ForbiddenException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.ConnectionRepository;
import com.connect.repository.StudentRepository;

/**
 * Manages connection requests between students (M3.2): sending, responding,
 * removing, and listing, with duplicate/self/state validation. Also exposes the
 * viewer-relative {@link RelationState} used by discovery and profiles.
 */
@Service
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final StudentRepository studentRepository;

    public ConnectionService(ConnectionRepository connectionRepository,
                             StudentRepository studentRepository) {
        this.connectionRepository = connectionRepository;
        this.studentRepository = studentRepository;
    }

    /** Sends a connection request from the caller to another student. */
    @Transactional
    public ConnectionDto send(Long meId, Long addresseeId) {
        if (meId.equals(addresseeId)) {
            throw new BadRequestException("You cannot connect with yourself.");
        }
        Student me = load(meId);
        Student other = load(addresseeId);

        List<Connection> between = connectionRepository.findBetween(meId, addresseeId);
        for (Connection c : between) {
            if (c.getStatus() == ConnectionStatus.ACCEPTED) {
                throw new BadRequestException("You are already connected.");
            }
            if (c.getStatus() == ConnectionStatus.PENDING) {
                throw new BadRequestException(c.getRequester().getId().equals(meId)
                        ? "You have already sent this student a request."
                        : "This student already sent you a request — respond to it instead.");
            }
        }
        // Only rejected rows may remain; clear them so a fresh request is clean.
        if (!between.isEmpty()) {
            connectionRepository.deleteAll(between);
            connectionRepository.flush();
        }

        Connection saved = connectionRepository.save(
                new Connection(me, other, ConnectionStatus.PENDING));
        return ConnectionDto.forViewer(saved, meId, RelationState.PENDING_OUTGOING);
    }

    /** Accepts or rejects a pending request; only the addressee may respond. */
    @Transactional
    public ConnectionDto respond(Long meId, Long connectionId, boolean accept) {
        Connection c = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found."));
        if (!c.getAddressee().getId().equals(meId)) {
            throw new ForbiddenException("Only the addressee can respond to this request.");
        }
        if (c.getStatus() != ConnectionStatus.PENDING) {
            throw new BadRequestException("This request is no longer pending.");
        }
        c.setStatus(accept ? ConnectionStatus.ACCEPTED : ConnectionStatus.REJECTED);
        Connection saved = connectionRepository.save(c);
        return ConnectionDto.forViewer(saved, meId,
                accept ? RelationState.CONNECTED : RelationState.NONE);
    }

    /** Removes a connection or cancels a request; either party may do so. */
    @Transactional
    public void remove(Long meId, Long connectionId) {
        Connection c = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found."));
        boolean involved = c.getRequester().getId().equals(meId)
                || c.getAddressee().getId().equals(meId);
        if (!involved) {
            throw new ForbiddenException("You are not part of this connection.");
        }
        connectionRepository.delete(c);
    }

    @Transactional(readOnly = true)
    public List<ConnectionDto> incoming(Long meId) {
        return connectionRepository
                .findByAddresseeIdAndStatusOrderByUpdatedAtDesc(meId, ConnectionStatus.PENDING)
                .stream()
                .map(c -> ConnectionDto.forViewer(c, meId, RelationState.PENDING_INCOMING))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConnectionDto> outgoing(Long meId) {
        return connectionRepository
                .findByRequesterIdAndStatusOrderByUpdatedAtDesc(meId, ConnectionStatus.PENDING)
                .stream()
                .map(c -> ConnectionDto.forViewer(c, meId, RelationState.PENDING_OUTGOING))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConnectionDto> accepted(Long meId) {
        return connectionRepository
                .findByStudentAndStatus(meId, ConnectionStatus.ACCEPTED)
                .stream()
                .map(c -> ConnectionDto.forViewer(c, meId, RelationState.CONNECTED))
                .toList();
    }

    /** Map of other-student-id to the caller's relationship with them. */
    @Transactional(readOnly = true)
    public Map<Long, RelationState> relationsFor(Long meId) {
        Map<Long, RelationState> relations = new HashMap<>();
        for (Connection c : connectionRepository.findActiveForStudent(meId)) {
            boolean iAmRequester = c.getRequester().getId().equals(meId);
            Long otherId = iAmRequester ? c.getAddressee().getId() : c.getRequester().getId();
            relations.put(otherId, relationOf(c, iAmRequester));
        }
        return relations;
    }

    /**
     * Dev helper: makes up to {@code limit} seeded dummy students send the caller
     * a pending request, so incoming-request flows can be tested without a second
     * real account. Skips dummies already connected/pending with the caller.
     *
     * @return the number of requests created
     */
    @Transactional
    public int seedIncomingFor(Long meId, int limit) {
        Student me = load(meId);
        Map<Long, RelationState> existing = relationsFor(meId);
        int created = 0;
        for (Student dummy : studentRepository.findByGoogleIdStartingWith("dummy-")) {
            if (created >= limit) {
                break;
            }
            if (dummy.getId().equals(meId) || existing.containsKey(dummy.getId())) {
                continue;
            }
            connectionRepository.save(new Connection(dummy, me, ConnectionStatus.PENDING));
            created++;
        }
        return created;
    }

    private static RelationState relationOf(Connection c, boolean iAmRequester) {
        if (c.getStatus() == ConnectionStatus.ACCEPTED) {
            return RelationState.CONNECTED;
        }
        return iAmRequester ? RelationState.PENDING_OUTGOING : RelationState.PENDING_INCOMING;
    }

    private Student load(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }
}
