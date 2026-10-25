package com.connect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.connect.entity.Connection;
import com.connect.entity.ConnectionStatus;

/**
 * Data access for {@link Connection}s.
 */
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    Optional<Connection> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    /** Any connection row between two students, in either direction. */
    @Query("""
            select c from Connection c
            where (c.requester.id = :a and c.addressee.id = :b)
               or (c.requester.id = :b and c.addressee.id = :a)
            """)
    List<Connection> findBetween(@Param("a") Long a, @Param("b") Long b);

    /** Requests addressed to this student in the given state (incoming). */
    List<Connection> findByAddresseeIdAndStatusOrderByUpdatedAtDesc(Long addresseeId, ConnectionStatus status);

    /** Requests this student sent in the given state (outgoing). */
    List<Connection> findByRequesterIdAndStatusOrderByUpdatedAtDesc(Long requesterId, ConnectionStatus status);

    /** Every connection involving this student (either side) in the given state. */
    @Query("""
            select c from Connection c
            where c.status = :status and (c.requester.id = :id or c.addressee.id = :id)
            order by c.updatedAt desc
            """)
    List<Connection> findByStudentAndStatus(@Param("id") Long studentId,
                                            @Param("status") ConnectionStatus status);

    /** Every non-rejected connection involving this student, for relation lookups. */
    @Query("""
            select c from Connection c
            where c.status <> com.connect.entity.ConnectionStatus.REJECTED
              and (c.requester.id = :id or c.addressee.id = :id)
            """)
    List<Connection> findActiveForStudent(@Param("id") Long studentId);
}
