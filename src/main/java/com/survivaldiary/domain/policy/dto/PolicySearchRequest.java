package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "맞춤 정책 검색 요청")
public record PolicySearchRequest(

        @Schema(description = "만 나이", example = "27")
        @NotNull(message = "나이는 필수입니다.")
        @Min(value = 18, message = "나이는 18세 이상이어야 합니다.")
        @Max(value = 39, message = "나이는 39세 이하여야 합니다.")
        Integer age,

        @Schema(description = "법정동 시·도 코드 앞 2자리", example = "11")
        @NotBlank(message = "시·도 코드는 필수입니다.")
        @Pattern(regexp = "\\d{2}", message = "시·도 코드는 숫자 2자리여야 합니다.")
        String regionCode,

        @Schema(description = "법정동 시·군·구 코드 앞 5자리", example = "11680")
        @Pattern(regexp = "\\d{5}", message = "시·군·구 코드는 숫자 5자리여야 합니다.")
        String districtCode,

        @Schema(
                description = "취업 상태: EMPLOYED / JOB_SEEKING / UNEMPLOYED / STUDENT",
                example = "JOB_SEEKING"
        )
        @NotBlank(message = "취업 상태는 필수입니다.")
        @Pattern(
                regexp = "EMPLOYED|JOB_SEEKING|UNEMPLOYED|STUDENT",
                message = "취업 상태 값이 올바르지 않습니다."
        )
        String employmentStatus,

        @Schema(
                description = "소득 구간: BELOW_50 / BELOW_100 / BELOW_150 / NO_LIMIT",
                example = "BELOW_100"
        )
        @Pattern(
                regexp = "BELOW_50|BELOW_100|BELOW_150|NO_LIMIT",
                message = "소득 구간 값이 올바르지 않습니다."
        )
        String incomeRange,

        @Schema(
                description = "관심 분야: HOUSING / EMPLOYMENT / ASSET / CULTURE / TRANSPORT",
                example = "HOUSING"
        )
        @Pattern(
                regexp = "HOUSING|EMPLOYMENT|ASSET|CULTURE|TRANSPORT",
                message = "관심 분야 값이 올바르지 않습니다."
        )
        String category,

        @Schema(description = "반환할 최대 정책 수. 생략 시 20", example = "20")
        @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.")
        @Max(value = 20, message = "조회 개수는 20 이하여야 합니다.")
        Integer size
) {

    public int requestedSize() {
        return size == null ? 20 : size;
    }

    public PolicyCategory requestedCategory() {
        return category == null ? null : PolicyCategory.valueOf(category);
    }
}
