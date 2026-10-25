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

import com.connect.dto.ConnectionDto;
import com.connect.entity.Connection;
import com.connect.entity.ConnectionStatus;
import com.connect.entity.Student;
import com.connect.exception.BadRequestException;
import com.connect.exception.ForbiddenException;
import com.connect.repository.ConnectionRepository;
import com.connect.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private ConnectionService connectionService;

    private Student student(Long id, String name) {
        Student s = new Student();
        ReflectionTestUtils.setField(s, "id", id);
        s.setName(name);
        s.setEmail(name + "@vitapstudent.ac.in");
        return s;
    }

    private Connection conn(Long id, Student requester, Student addressee, ConnectionStatus status) {
        Connection c = new Connection(requester, addressee, status);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    void sendToSelfIsRejected() {
        assertThatThrownBy(() -> connectionService.send(1L, 1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void sendCreatesPendingWhenNoneExists() {
        Student me = student(1L, "Me");
        Student other = student(2L, "Other");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(me));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(other));
        when(connectionRepository.findBetween(1L, 2L)).thenReturn(List.of());
        when(connectionRepository.save(any(Connection.class))).thenAnswer(inv -> inv.getArgument(0));

        ConnectionDto dto = connectionService.send(1L, 2L);

        assertThat(dto.status()).isEqualTo(ConnectionStatus.PENDING);
        assertThat(dto.direction()).isEqualTo("OUTGOING");
        assertThat(dto.other().id()).isEqualTo(2L);
    }

    @Test
    void sendIsRejectedWhenRequestAlreadyPending() {
        Student me = student(1L, "Me");
        Student other = student(2L, "Other");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(me));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(other));
        when(connectionRepository.findBetween(1L, 2L))
                .thenReturn(List.of(conn(5L, me, other, ConnectionStatus.PENDING)));

        assertThatThrownBy(() -> connectionService.send(1L, 2L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void sendIsRejectedWhenAlreadyConnected() {
        Student me = student(1L, "Me");
        Student other = student(2L, "Other");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(me));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(other));
        when(connectionRepository.findBetween(1L, 2L))
                .thenReturn(List.of(conn(5L, other, me, ConnectionStatus.ACCEPTED)));

        assertThatThrownBy(() -> connectionService.send(1L, 2L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void respondByNonAddresseeIsForbidden() {
        Student a = student(1L, "A");
        Student b = student(2L, "B");
        when(connectionRepository.findById(5L))
                .thenReturn(Optional.of(conn(5L, a, b, ConnectionStatus.PENDING)));

        // Caller 3 is neither requester nor addressee.
        assertThatThrownBy(() -> connectionService.respond(3L, 5L, true))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void acceptSetsStatusAccepted() {
        Student a = student(1L, "A");
        Student me = student(2L, "Me");
        when(connectionRepository.findById(5L))
                .thenReturn(Optional.of(conn(5L, a, me, ConnectionStatus.PENDING)));
        when(connectionRepository.save(any(Connection.class))).thenAnswer(inv -> inv.getArgument(0));

        ConnectionDto dto = connectionService.respond(2L, 5L, true);

        assertThat(dto.status()).isEqualTo(ConnectionStatus.ACCEPTED);
    }

    @Test
    void respondToNonPendingIsRejected() {
        Student a = student(1L, "A");
        Student me = student(2L, "Me");
        when(connectionRepository.findById(5L))
                .thenReturn(Optional.of(conn(5L, a, me, ConnectionStatus.ACCEPTED)));

        assertThatThrownBy(() -> connectionService.respond(2L, 5L, false))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void removeByOutsiderIsForbidden() {
        Student a = student(1L, "A");
        Student b = student(2L, "B");
        when(connectionRepository.findById(5L))
                .thenReturn(Optional.of(conn(5L, a, b, ConnectionStatus.ACCEPTED)));

        assertThatThrownBy(() -> connectionService.remove(3L, 5L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removeByParticipantDeletes() {
        Student a = student(1L, "A");
        Student b = student(2L, "B");
        Connection c = conn(5L, a, b, ConnectionStatus.ACCEPTED);
        when(connectionRepository.findById(5L)).thenReturn(Optional.of(c));

        connectionService.remove(1L, 5L);

        verify(connectionRepository).delete(c);
    }
}
