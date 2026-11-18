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

import com.connect.dto.BlogPostDto;
import com.connect.dto.SaveBlogRequest;
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
    public List<BlogPostDto> feed() {
        return blogService.feed();
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
}
