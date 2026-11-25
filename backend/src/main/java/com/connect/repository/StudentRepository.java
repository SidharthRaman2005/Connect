package com.connect.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;

/**
 * Data access for {@link Student} accounts.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByGoogleId(String googleId);

    Optional<Student> findByEmail(String email);

    /** Discovery/search candidates: a given visibility, excluding one student. */
    List<Student> findByProfileVisibilityAndIdNot(ProfileVisibility visibility, Long id);

    /** Ranks and pages candidate ids in PostgreSQL before loading profile/tag data. */
    @Query(value = """
            select s.id
            from Student s left join s.tags sharedTag
            where s.profileVisibility = :visibility and s.id <> :meId
              and (:nameQuery = '' or lower(coalesce(s.name, '')) like concat('%', :nameQuery, '%'))
              and (:branchQuery = '' or lower(coalesce(s.branch, '')) = :branchQuery)
              and (:batchQuery is null or s.batch = :batchQuery)
              and (:filterTags = false or exists (
                    select candidate.id from Student candidate join candidate.tags candidateTag
                    where candidate.id = s.id and candidateTag.id in :wantedTagIds))
            group by s.id, s.name
            order by sum(case when sharedTag.id in :myTagIds then 1 else 0 end) desc,
                     lower(coalesce(s.name, '')) asc, s.id asc
            """,
            countQuery = """
            select count(s)
            from Student s
            where s.profileVisibility = :visibility and s.id <> :meId
              and (:nameQuery = '' or lower(coalesce(s.name, '')) like concat('%', :nameQuery, '%'))
              and (:branchQuery = '' or lower(coalesce(s.branch, '')) = :branchQuery)
              and (:batchQuery is null or s.batch = :batchQuery)
              and (:filterTags = false or exists (
                    select candidate.id from Student candidate join candidate.tags candidateTag
                    where candidate.id = s.id and candidateTag.id in :wantedTagIds))
            """)
    Page<Long> findDiscoveryIds(@Param("meId") Long meId,
                                @Param("visibility") ProfileVisibility visibility,
                                @Param("nameQuery") String nameQuery,
                                @Param("branchQuery") String branchQuery,
                                @Param("batchQuery") Integer batchQuery,
                                @Param("filterTags") boolean filterTags,
                                @Param("wantedTagIds") Collection<Long> wantedTagIds,
                                @Param("myTagIds") Collection<Long> myTagIds,
                                Pageable pageable);

    @EntityGraph(attributePaths = "tags")
    @Query("select distinct s from Student s where s.id in :ids")
    List<Student> findAllWithTagsByIdIn(@Param("ids") Collection<Long> ids);

    /** Dummy accounts seeded for development (googleId starts with "dummy-"). */
    List<Student> findByGoogleIdStartingWith(String prefix);
}
