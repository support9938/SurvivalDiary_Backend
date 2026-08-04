package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

        @Schema(description = "이전 앱 호환용 취업 상태")
        @Pattern(
                regexp = "EMPLOYED|JOB_SEEKING|UNEMPLOYED|STUDENT",
                message = "이전 취업 상태 값이 올바르지 않습니다."
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
                description = "공식 정책 분야. 이전 앱 분야 코드도 한시적으로 허용",
                example = "HOUSING"
        )
        @Pattern(
                regexp = "EMPLOYMENT|HOUSING|EDUCATION|WELFARE_CULTURE|"
                        + "PARTICIPATION_RIGHTS|ASSET|CULTURE|TRANSPORT",
                message = "관심 분야 값이 올바르지 않습니다."
        )
        String category,

        @Schema(description = "정책명 검색어. 공백은 검색하지 않음", example = "월세")
        @Size(max = 50, message = "정책 검색어는 50자 이하여야 합니다.")
        String keyword,

        @Schema(description = "온통청년 조회 페이지. 생략 시 1", example = "1")
        @Min(value = 1, message = "조회 페이지는 1 이상이어야 합니다.")
        @Max(value = 1000, message = "조회 페이지는 1000 이하여야 합니다.")
        Integer page,

        @Schema(description = "반환할 최대 정책 수. 생략 시 20", example = "20")
        @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.")
        @Max(value = 20, message = "조회 개수는 20 이하여야 합니다.")
        Integer size,

        @Schema(description = "근로 상태. 모르면 생략")
        @Pattern(
                regexp = "EMPLOYED|SELF_EMPLOYED|UNEMPLOYED|FREELANCER|DAILY_WORKER|"
                        + "PROSPECTIVE_FOUNDER|SHORT_TERM_WORKER|FARMER|OTHER",
                message = "근로 상태 값이 올바르지 않습니다."
        )
        String workStatus,

        @Schema(description = "현재 구직 여부. 모르면 생략")
        Boolean jobSeeking,

        @Schema(description = "교육 상태. 모르면 생략")
        @Pattern(
                regexp = "STUDENT|ON_LEAVE|GRADUATED|NOT_STUDENT|OTHER",
                message = "교육 상태 값이 올바르지 않습니다."
        )
        String educationStatus
) {

    public PolicySearchRequest {
        keyword = normalize(keyword);
    }

    public int requestedSize() {
        return size == null ? 20 : size;
    }

    public PolicyCategory requestedCategory() {
        if (category == null || "ASSET".equals(category) || "TRANSPORT".equals(category)) {
            return null;
        }
        if ("CULTURE".equals(category)) {
            return PolicyCategory.WELFARE_CULTURE;
        }
        return PolicyCategory.valueOf(category);
    }

    public String requestedWorkStatus() {
        if (workStatus != null) {
            return workStatus;
        }
        if (employmentStatus == null) {
            return null;
        }
        return switch (employmentStatus) {
            case "EMPLOYED" -> "EMPLOYED";
            case "JOB_SEEKING", "UNEMPLOYED" -> "UNEMPLOYED";
            default -> null;
        };
    }

    public String requestedEducationStatus() {
        if (educationStatus != null) {
            return educationStatus;
        }
        return "STUDENT".equals(employmentStatus) ? "STUDENT" : null;
    }

    public int requestedPage() {
        return page == null ? 1 : page;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
