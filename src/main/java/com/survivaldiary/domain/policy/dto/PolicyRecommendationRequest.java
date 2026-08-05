package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "저장된 사용자 조건 기반 정책 추천 요청")
public record PolicyRecommendationRequest(

        @Schema(description = "목록 탐색용 정책 분야. 전체이면 생략", example = "HOUSING")
        @Pattern(
                regexp = "EMPLOYMENT|HOUSING|EDUCATION|WELFARE_CULTURE|PARTICIPATION_RIGHTS",
                message = "정책 분야 값이 올바르지 않습니다."
        )
        String category,

        @Schema(description = "정책명 검색어. 공백은 검색하지 않음", example = "월세")
        @Size(max = 50, message = "정책 검색어는 50자 이하여야 합니다.")
        String keyword,

        @Schema(description = "조회 페이지. 생략 시 1", example = "1")
        @Min(value = 1, message = "조회 페이지는 1 이상이어야 합니다.")
        @Max(value = 1000, message = "조회 페이지는 1000 이하여야 합니다.")
        Integer page,

        @Schema(description = "반환할 최대 정책 수. 생략 시 20", example = "20")
        @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.")
        @Max(value = 20, message = "조회 개수는 20 이하여야 합니다.")
        Integer size
) {

    public PolicyRecommendationRequest {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
