package com.survivaldiary.domain.community.controller;

import com.survivaldiary.domain.community.dto.CreatePostRequest;
import com.survivaldiary.domain.community.dto.CommentResponse;
import com.survivaldiary.domain.community.dto.CreateCommentRequest;
import com.survivaldiary.domain.community.dto.PostResponse;
import com.survivaldiary.domain.community.service.CommunityService;
import com.survivaldiary.global.common.ApiResponse;
import com.survivaldiary.global.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/community/posts")
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService communityService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PostResponse> result = communityService.list(userId, category, Math.max(page, 0), Math.min(Math.max(size, 1), 60));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> popular(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "5") int size) {
        Page<PostResponse> result = communityService.popular(userId, Math.min(Math.max(size, 1), 10));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/faqs")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> faqs(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PostResponse> result = communityService.faqs(
                userId, Math.max(page, 0), Math.min(Math.max(size, 1), 60));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> mine(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "질문") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PostResponse> result = communityService.myPosts(
                userId, category, Math.max(page, 0), Math.min(Math.max(size, 1), 60));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> get(
            @AuthenticationPrincipal Long userId, @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.ok(communityService.get(postId, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(communityService.create(userId, request)));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<PostResponse>> like(
            @AuthenticationPrincipal Long userId, @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.ok(communityService.toggleLike(postId, userId)));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> update(
            @AuthenticationPrincipal Long userId, @PathVariable Long postId,
            @Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(communityService.update(postId, userId, request)));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId, @PathVariable Long postId) {
        communityService.delete(postId, userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/{postId}/bookmark")
    public ResponseEntity<ApiResponse<PostResponse>> bookmark(
            @AuthenticationPrincipal Long userId, @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.ok(communityService.toggleBookmark(postId, userId)));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> comments(
            @AuthenticationPrincipal Long userId, @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.ok(communityService.comments(postId, userId)));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(communityService.createComment(postId, userId, request)));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal Long userId, @PathVariable Long commentId) {
        communityService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
