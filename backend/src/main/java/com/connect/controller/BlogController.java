package com.connect.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;

import com.connect.dto.BlogPostDto;
import com.connect.dto.SaveBlogRequest;
import com.connect.dto.BlogCommentDto;
import com.connect.dto.SaveBlogCommentRequest;
import com.connect.security.AuthenticatedStudent;
import com.connect.service.BlogService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/blogs")
public class BlogController {
    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    public List<BlogPostDto> feed(@AuthenticationPrincipal AuthenticatedStudent principal,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(required = false) String tag) {
        return blogService.feed(principal.id(), q, tag);
    }

    @GetMapping("/me")
    public List<BlogPostDto> mine(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return blogService.mine(principal.id());
    }

    @PostMapping
    public BlogPostDto create(@AuthenticationPrincipal AuthenticatedStudent principal,
                              @Valid @RequestBody SaveBlogRequest request) {
        return blogService.create(principal.id(), request);
    }

    @PutMapping("/{id}")
    public BlogPostDto update(@AuthenticationPrincipal AuthenticatedStudent principal,
                              @PathVariable Long id,
                              @Valid @RequestBody SaveBlogRequest request) {
        return blogService.update(principal.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedStudent principal,
                                       @PathVariable Long id) {
        blogService.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/comments")
    public List<BlogCommentDto> comments(@PathVariable Long id) { return blogService.comments(id); }

    @PostMapping("/{id}/comments")
    public BlogCommentDto addComment(@AuthenticationPrincipal AuthenticatedStudent principal,
                                     @PathVariable Long id,
                                     @Valid @RequestBody SaveBlogCommentRequest request) {
        return blogService.addComment(principal.id(), id, request);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@AuthenticationPrincipal AuthenticatedStudent principal,
                                              @PathVariable Long id, @PathVariable Long commentId) {
        blogService.deleteComment(principal.id(), id, commentId);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/like")
    public ResponseEntity<Void> like(@AuthenticationPrincipal AuthenticatedStudent principal, @PathVariable Long id) {
        blogService.like(principal.id(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Void> unlike(@AuthenticationPrincipal AuthenticatedStudent principal, @PathVariable Long id) {
        blogService.unlike(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
