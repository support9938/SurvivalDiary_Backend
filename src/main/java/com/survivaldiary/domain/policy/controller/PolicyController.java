package com.survivaldiary.domain.policy.controller;

import com.survivaldiary.domain.policy.dto.PolicyDetail;
import com.survivaldiary.domain.policy.dto.PolicyRecommendationRequest;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicySearchResponse;
import com.survivaldiary.domain.policy.service.PolicyRecommendationService;
import com.survivaldiary.domain.policy.service.PolicyService;
import com.survivaldiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Policy", description = "온통청년 기반 맞춤 정책")
@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyRecommendationService policyRecommendationService;

    public PolicyController(
            PolicyService policyService,
            PolicyRecommendationService policyRecommendationService
    ) {
        this.policyService = policyService;
        this.policyRecommendationService = policyRecommendationService;
    }

    @Operation(
            summary = "저장 조건 기반 맞춤 정책 추천",
            description = "로그인 사용자의 저장된 나이·지역·현재 상황을 적용하고, 정책 분야와 검색어는 "
                    + "목록 탐색 조건으로만 사용한다."
    )
    @PostMapping("/recommendations")
    public ResponseEntity<ApiResponse<PolicySearchResponse>> recommendations(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PolicyRecommendationRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(policyRecommendationService.recommend(userId, request)));
    }

    @Operation(
            summary = "맞춤 정책 실시간 검색",
            description = "로그인 사용자의 조건과 선택적인 정책명 검색어로 온통청년 정책 한 페이지를 "
                    + "조회하고, 다음 페이지 번호와 함께 반환한다."
    )
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<PolicySearchResponse>> search(
            @Valid @RequestBody PolicySearchRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(policyService.search(request)));
    }

    @Operation(
            summary = "정책 상세 실시간 조회",
            description = "문자열 정책 번호로 온통청년 정책 상세를 조회해 내부 응답 형식으로 반환한다."
    )
    @GetMapping("/{policyId}")
    public ResponseEntity<ApiResponse<PolicyDetail>> detail(
            @PathVariable String policyId
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(policyService.findDetail(policyId)));
    }
}
