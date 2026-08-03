package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "맞춤 정책 기본 조건 저장 요청")
public record PolicyPreferenceRequest(

        @Schema(description = "법정동 시도 코드 앞 2자리", example = "11")
        @NotBlank(message = "시도 코드는 필수입니다.")
        @Pattern(regexp = "\\d{2}", message = "시도 코드는 숫자 2자리여야 합니다.")
        String regionCode,

        @Schema(description = "법정동 시군구 코드 앞 5자리. 시도 전체면 생략", example = "11680")
        @Pattern(regexp = "\\d{5}", message = "시군구 코드는 숫자 5자리여야 합니다.")
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
                description = "소득 구간. 소득 전체면 생략: BELOW_50 / BELOW_100 / BELOW_150 / NO_LIMIT",
                example = "BELOW_100"
        )
        @Pattern(
                regexp = "BELOW_50|BELOW_100|BELOW_150|NO_LIMIT",
                message = "소득 구간 값이 올바르지 않습니다."
        )
        String incomeRange,

        @Schema(
                description = "정책 분야. 전체면 생략: HOUSING / EMPLOYMENT / ASSET / CULTURE / TRANSPORT",
                example = "HOUSING"
        )
        @Pattern(
                regexp = "HOUSING|EMPLOYMENT|ASSET|CULTURE|TRANSPORT",
                message = "정책 분야 값이 올바르지 않습니다."
        )
        String category
) {
}
