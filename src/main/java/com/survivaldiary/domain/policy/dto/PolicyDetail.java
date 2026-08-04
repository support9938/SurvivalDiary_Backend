package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "정책 상세")
public record PolicyDetail(
        @Schema(description = "온통청년 정책 번호") String policyId,
        @Schema(description = "제공처 정책 분류") String category,
        @Schema(description = "앱 정책 카테고리") PolicyCategory categoryType,
        @Schema(description = "정책명") String title,
        @Schema(description = "정책 설명") String description,
        @Schema(description = "구조화된 지원금. 확인 불가 시 null") Long supportAmount,
        @Schema(description = "지원 내용 원문") String supportText,
        @Schema(description = "신청 기간 원문") String applicationPeriodText,
        @Schema(description = "지원 대상 요약") String target,
        @Schema(description = "주관 기관") String agency,
        @Schema(description = "운영 기관") String operatingAgency,
        @Schema(description = "신청 방법") String applicationMethod,
        @Schema(description = "제출 서류") List<String> documents,
        @Schema(description = "공식 신청 URL") String officialUrl,
        @Schema(description = "참고 URL") List<String> referenceUrls
) {
}
