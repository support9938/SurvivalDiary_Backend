package com.survivaldiary.domain.policy.dto;

import com.survivaldiary.domain.policy.entity.HiddenPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "관심 없음 정책")
public record HiddenPolicyResponse(
        @Schema(description = "온통청년 정책 번호") String policyId,
        @Schema(description = "정책명") String title,
        @Schema(description = "정책 분야") String category,
        @Schema(description = "목록용 한 줄 요약") String shortSummary,
        @Schema(description = "관심 없음 설정 시각") LocalDateTime hiddenAt
) {
    public static HiddenPolicyResponse from(HiddenPolicy hiddenPolicy) {
        return new HiddenPolicyResponse(
                hiddenPolicy.getPolicyId(),
                hiddenPolicy.getTitle(),
                hiddenPolicy.getCategory(),
                hiddenPolicy.getShortSummary(),
                hiddenPolicy.getHiddenAt()
        );
    }
}
