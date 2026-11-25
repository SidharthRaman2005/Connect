package com.connect.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connect.dto.BlogCommentDto;
import com.connect.dto.BlogPostDto;
import com.connect.dto.SaveBlogCommentRequest;
import com.connect.dto.SaveBlogRequest;
import com.connect.entity.BlogComment;
import com.connect.entity.BlogPost;
import com.connect.entity.Student;
import com.connect.exception.ForbiddenException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.BlogCommentRepository;
import com.connect.repository.BlogLikeRepository;
import com.connect.repository.BlogPostRepository;
import com.connect.repository.StudentRepository;

@Service
public class BlogService {
    private final BlogPostRepository blogPostRepository;
    private final BlogCommentRepository commentRepository;
    private final BlogLikeRepository likeRepository;
    private final StudentRepository studentRepository;

    public BlogService(BlogPostRepository blogPostRepository, BlogCommentRepository commentRepository,
                       BlogLikeRepository likeRepository, StudentRepository studentRepository) {
        this.blogPostRepository = blogPostRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<BlogPostDto> feed(Long viewerId, String query, String tag) {
        return mapPosts(blogPostRepository.search(null, normalize(query), normalize(tag), PageRequest.of(0, 50)), viewerId);
    }

    @Transactional(readOnly = true)
    public List<BlogPostDto> mine(Long authorId) {
        return mapPosts(blogPostRepository.search(authorId, "", "", PageRequest.of(0, 100)), authorId);
    }

    @Transactional
    public BlogPostDto create(Long authorId, SaveBlogRequest request) {
        Student author = loadStudent(authorId);
        BlogPost post = new BlogPost(author, request.title().trim(), request.content().trim());
        post.setTags(normalizeTags(request.tags()));
        return BlogPostDto.from(blogPostRepository.save(post), 0L, false);
    }

    @Transactional
    public BlogPostDto update(Long authorId, Long postId, SaveBlogRequest request) {
        BlogPost post = loadOwned(authorId, postId);
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.setTags(normalizeTags(request.tags()));
        return BlogPostDto.from(blogPostRepository.save(post), likeRepository.countByPostId(postId), likeRepository.existsByPostIdAndStudentId(postId, authorId));
    }

    @Transactional
    public void delete(Long authorId, Long postId) {
        BlogPost post = loadOwned(authorId, postId);
        commentRepository.deleteByPostId(postId);
        likeRepository.deleteByPostId(postId);
        blogPostRepository.delete(post);
    }

    @Transactional
    public void like(Long studentId, Long postId) {
        BlogPost post = loadPost(postId);
        if (!likeRepository.existsByPostIdAndStudentId(postId, studentId)) {
            likeRepository.save(new com.connect.entity.BlogLike(post, loadStudent(studentId)));
        }
    }

    @Transactional
    public void unlike(Long studentId, Long postId) {
        loadPost(postId);
        likeRepository.deleteByPostIdAndStudentId(postId, studentId);
    }

    @Transactional(readOnly = true)
    public List<BlogCommentDto> comments(Long postId) {
        loadPost(postId);
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream().map(BlogCommentDto::from).toList();
    }

    @Transactional
    public BlogCommentDto addComment(Long studentId, Long postId, SaveBlogCommentRequest request) {
        BlogPost post = loadPost(postId);
        BlogComment comment = commentRepository.save(new BlogComment(post, loadStudent(studentId), request.content().trim()));
        return BlogCommentDto.from(comment);
    }

    @Transactional
    public void deleteComment(Long studentId, Long postId, Long commentId) {
        BlogPost post = loadPost(postId);
        BlogComment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found."));
        if (!comment.getAuthor().getId().equals(studentId) && !post.getAuthor().getId().equals(studentId)) {
            throw new ForbiddenException("Only the comment author or blog author can remove this comment.");
        }
        commentRepository.delete(comment);
    }

    private List<BlogPostDto> mapPosts(List<BlogPost> posts, Long viewerId) {
        return posts.stream().map(post -> BlogPostDto.from(post,
                likeRepository.countByPostId(post.getId()),
                likeRepository.existsByPostIdAndStudentId(post.getId(), viewerId))).toList();
    }

    private BlogPost loadPost(Long postId) {
        return blogPostRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Blog post not found."));
    }

    private BlogPost loadOwned(Long authorId, Long postId) {
        BlogPost post = loadPost(postId);
        if (!post.getAuthor().getId().equals(authorId)) throw new ForbiddenException("You can only modify your own blog posts.");
        return post;
    }

    private Student loadStudent(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }

    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }

    private static List<String> normalizeTags(List<String> tags) {
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        if (tags == null) return List.of();
        for (String tag : tags) {
            if (tag == null) continue;
            String value = tag.trim().replaceFirst("^#", "").toLowerCase(Locale.ROOT);
            if (!value.isBlank()) cleaned.add(value);
        }
        return cleaned.stream().limit(8).toList();
    }
}
