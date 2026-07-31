package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "맞춤 정책 실시간 검색 결과")
public record PolicySearchResponse(
        @Schema(description = "조건에 일치하거나 확인이 필요한 정책") List<PolicySummary> items,
        @Schema(description = "최대 조회 범위 때문에 일부 결과일 가능성") boolean partialResult,
        @Schema(description = "이번 요청에서 확인한 온통청년 페이지 수") int checkedProviderPages
) {
}
