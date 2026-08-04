package com.survivaldiary.domain.policy.controller;

import com.survivaldiary.domain.policy.dto.PolicyPreferenceRequest;
import com.survivaldiary.domain.policy.dto.PolicyPreferenceResponse;
import com.survivaldiary.domain.policy.service.PolicyPreferenceService;
import com.survivaldiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Policy", description = "온통청년 기반 맞춤 정책")
@RestController
@RequestMapping("/api/users/me/policy-preferences")
@RequiredArgsConstructor
public class PolicyPreferenceController {

    private final PolicyPreferenceService policyPreferenceService;

    @Operation(
            summary = "내 맞춤 정책 기본 조건 조회",
            description = "저장된 조건이 없으면 오류 대신 saved=false를 반환한다. 나이는 생년월일로 계산한다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PolicyPreferenceResponse>> get(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(policyPreferenceService.get(userId)));
    }

    @Operation(
            summary = "내 맞춤 정책 기본 조건 저장",
            description = "기존 조건이 있으면 전체 교체한다. 선택 조건을 null로 보내면 해당 조건을 초기화한다."
    )
    @PutMapping
    public ResponseEntity<ApiResponse<PolicyPreferenceResponse>> save(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PolicyPreferenceRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(policyPreferenceService.save(userId, request)));
    }
}
