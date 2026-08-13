package com.survivaldiary.domain.admin.controller;

import com.survivaldiary.domain.admin.dto.AdminUserResponse;
import com.survivaldiary.domain.admin.service.AdminService;
import com.survivaldiary.domain.community.dto.CommentResponse;
import com.survivaldiary.domain.community.dto.CreateCommentRequest;
import com.survivaldiary.domain.community.dto.PostResponse;
import com.survivaldiary.domain.community.service.CommunityService;
import com.survivaldiary.domain.expense.dto.ExpenseResponse;
import com.survivaldiary.global.common.ApiResponse;
import com.survivaldiary.global.common.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final CommunityService communityService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> users(@RequestParam(defaultValue = "") String query, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<AdminUserResponse> result = adminService.users(query, Math.max(page, 0), Math.min(Math.max(size, 1), 60));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/users/{userId}/expenses")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> userExpenses(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.userExpenses(userId)));
    }

    @GetMapping("/community/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> posts(@AuthenticationPrincipal Long adminId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(communityService.adminPosts(adminId, Math.max(page, 0), Math.min(Math.max(size, 1), 60)))));
    }

    @PostMapping("/community/posts/{postId}/answer")
    public ResponseEntity<ApiResponse<CommentResponse>> answer(@AuthenticationPrincipal Long adminId, @PathVariable Long postId, @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(communityService.createAdminAnswer(postId, adminId, request)));
    }

    @DeleteMapping("/community/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@AuthenticationPrincipal Long adminId, @PathVariable Long postId) {
        communityService.delete(postId, adminId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
