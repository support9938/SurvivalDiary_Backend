package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "관심 없음 정책의 목록 표시 정보")
public record HiddenPolicyRequest(
        @Schema(description = "정책명")
        @NotBlank
        @Size(max = 200)
        String title,

        @Schema(description = "정책 분야")
        @Size(max = 100)
        String category,

        @Schema(description = "목록용 한 줄 요약")
        @Size(max = 500)
        String shortSummary
) {
}
