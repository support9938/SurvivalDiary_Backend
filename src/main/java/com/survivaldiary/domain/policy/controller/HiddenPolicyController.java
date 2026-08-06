package com.survivaldiary.domain.policy.controller;

import com.survivaldiary.domain.policy.dto.HiddenPolicyRequest;
import com.survivaldiary.domain.policy.dto.HiddenPolicyResponse;
import com.survivaldiary.domain.policy.service.HiddenPolicyService;
import com.survivaldiary.global.common.ApiResponse;
import com.survivaldiary.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Policy", description = "온통청년 기반 맞춤 정책")
@RestController
@RequestMapping("/api/users/me/hidden-policies")
@RequiredArgsConstructor
public class HiddenPolicyController {

    private final HiddenPolicyService hiddenPolicyService;

    @Operation(summary = "내 관심 없음 정책 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<HiddenPolicyResponse>>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(PageResponse.from(hiddenPolicyService.list(
                        userId,
                        Math.max(page, 0),
                        Math.min(Math.max(size, 1), 100)
                ))));
    }

    @Operation(
            summary = "정책을 관심 없음으로 설정",
            description = "같은 정책을 다시 요청하면 저장된 목록 표시 정보를 갱신한다."
    )
    @PutMapping("/{policyId}")
    public ResponseEntity<ApiResponse<HiddenPolicyResponse>> hide(
            @AuthenticationPrincipal Long userId,
            @PathVariable String policyId,
            @Valid @RequestBody HiddenPolicyRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(hiddenPolicyService.hide(userId, policyId, request)));
    }

    @Operation(
            summary = "관심 없음 정책 복구",
            description = "이미 복구된 정책을 다시 요청해도 성공으로 처리한다."
    )
    @DeleteMapping("/{policyId}")
    public ResponseEntity<ApiResponse<Void>> restore(
            @AuthenticationPrincipal Long userId,
            @PathVariable String policyId
    ) {
        hiddenPolicyService.restore(userId, policyId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok());
    }
}
