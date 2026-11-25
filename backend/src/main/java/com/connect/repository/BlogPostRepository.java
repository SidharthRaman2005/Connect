package com.connect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.connect.entity.BlogPost;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    List<BlogPost> findTop50ByOrderByCreatedAtDesc();
    List<BlogPost> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    @Query("""
            select distinct p from BlogPost p left join p.tags tag
            where (:authorId is null or p.author.id = :authorId)
              and (:query = '' or lower(p.title) like concat('%', :query, '%') or lower(p.content) like concat('%', :query, '%'))
              and (:tag = '' or lower(tag) = :tag)
            order by p.createdAt desc
            """)
    List<BlogPost> search(@Param("authorId") Long authorId, @Param("query") String query,
                          @Param("tag") String tag, Pageable pageable);
}
