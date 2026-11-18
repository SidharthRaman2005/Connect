package com.connect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connect.dto.BlogPostDto;
import com.connect.dto.SaveBlogRequest;
import com.connect.entity.BlogPost;
import com.connect.entity.Student;
import com.connect.exception.ForbiddenException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.BlogPostRepository;
import com.connect.repository.StudentRepository;

@Service
public class BlogService {
    private final BlogPostRepository blogPostRepository;
    private final StudentRepository studentRepository;

    public BlogService(BlogPostRepository blogPostRepository, StudentRepository studentRepository) {
        this.blogPostRepository = blogPostRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<BlogPostDto> feed() {
        return blogPostRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(BlogPostDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BlogPostDto> mine(Long authorId) {
        return blogPostRepository.findByAuthorIdOrderByCreatedAtDesc(authorId).stream()
                .map(BlogPostDto::from)
                .toList();
    }

    @Transactional
    public BlogPostDto create(Long authorId, SaveBlogRequest request) {
        Student author = studentRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        BlogPost post = new BlogPost(author, request.title().trim(), request.content().trim());
        return BlogPostDto.from(blogPostRepository.save(post));
    }

    @Transactional
    public BlogPostDto update(Long authorId, Long postId, SaveBlogRequest request) {
        BlogPost post = loadOwned(authorId, postId);
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        return BlogPostDto.from(blogPostRepository.save(post));
    }

    @Transactional
    public void delete(Long authorId, Long postId) {
        blogPostRepository.delete(loadOwned(authorId, postId));
    }

    private BlogPost loadOwned(Long authorId, Long postId) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found."));
        if (!post.getAuthor().getId().equals(authorId)) {
            throw new ForbiddenException("You can only modify your own blog posts.");
        }
        return post;
    }
}
