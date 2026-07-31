package com.survivaldiary.domain.policy.controller;

import com.survivaldiary.domain.policy.dto.PolicyDetail;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicySearchResponse;
import com.survivaldiary.domain.policy.service.PolicyService;
import com.survivaldiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
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

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @Operation(
            summary = "맞춤 정책 실시간 검색",
            description = "로그인 사용자의 조건으로 온통청년 정책을 최대 3페이지 조회하고, "
                    + "확정할 수 없는 조건은 CHECK_REQUIRED로 반환한다."
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
