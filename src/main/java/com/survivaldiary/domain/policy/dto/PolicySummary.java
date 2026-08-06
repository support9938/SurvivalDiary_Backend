package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "맞춤 정책 목록 항목")
public record PolicySummary(
        @Schema(description = "온통청년 정책 번호") String policyId,
        @Schema(description = "제공처 정책 분류") String category,
        @Schema(description = "앱 정책 카테고리") PolicyCategory categoryType,
        @Schema(description = "정책명") String title,
        @Schema(description = "정책 요약") String summary,
        @Schema(description = "목록 표시용 기호 없는 한 줄 요약") String shortSummary,
        @Schema(description = "구조화된 지원금. 확인 불가 시 null") Long supportAmount,
        @Schema(description = "지원 금액 유형. 확인 불가 시 null") PolicySupportAmountType supportAmountType,
        @Schema(description = "지원 내용 원문") String supportText,
        @Schema(description = "신청 기간 원문") String applicationPeriodText,
        @Schema(description = "신청 기간 유형") PolicyApplicationPeriodType applicationPeriodType,
        @Schema(description = "안전하게 해석된 신청 시작일. 확인 불가 시 null") LocalDate applicationStartDate,
        @Schema(description = "안전하게 해석된 신청 종료일. 상시·마감·해석 불가 시 null") LocalDate applicationEndDate,
        @Schema(description = "지원 대상 요약") String target,
        @Schema(description = "주관 기관") String agency,
        @Schema(description = "조건 일치 판정") PolicyEligibilityStatus eligibilityStatus,
        @Schema(description = "직접 확인이 필요한 이유") List<String> eligibilityReasons,
        @Schema(description = "맞춤 추천 표시 상태") PolicyRecommendationStatus recommendationStatus,
        @Schema(description = "추천 또는 확인 필요 이유") List<String> recommendationReasons
) {
}
