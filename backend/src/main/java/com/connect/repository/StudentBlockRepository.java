package com.connect.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.connect.entity.StudentBlock;

public interface StudentBlockRepository extends JpaRepository<StudentBlock, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    boolean existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(Long a1, Long b1, Long a2, Long b2);
    List<StudentBlock> findByBlockerIdOrderByCreatedAtDesc(Long blockerId);
    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    @Query("select case when b.blocker.id = :me then b.blocked.id else b.blocker.id end from StudentBlock b where b.blocker.id = :me or b.blocked.id = :me")
    List<Long> findBlockedEitherWay(@Param("me") Long studentId);
    @Query("select b from StudentBlock b where b.blocker.id = :first and b.blocked.id = :second or b.blocker.id = :second and b.blocked.id = :first")
    List<StudentBlock> findBetween(@Param("first") Long first, @Param("second") Long second);
}
