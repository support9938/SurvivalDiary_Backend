package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.Set;

@Schema(description = "맞춤 정책 기본 조건 저장 요청")
public record PolicyPreferenceRequest(

        @Schema(description = "사용자가 입력한 현재 만 나이. 회원 생년월일이 없을 때만 사용", example = "27")
        @Min(value = 18, message = "나이는 만 18세 이상이어야 합니다.")
        @Max(value = 39, message = "나이는 만 39세 이하여야 합니다.")
        Integer age,

        @Schema(description = "법정동 시도 코드 앞 2자리", example = "11")
        @NotBlank(message = "시도 코드는 필수입니다.")
        @Pattern(regexp = "\\d{2}", message = "시도 코드는 숫자 2자리여야 합니다.")
        String regionCode,

        @Schema(description = "법정동 시군구 코드 앞 5자리. 시도 전체면 생략", example = "11680")
        @Pattern(regexp = "\\d{5}", message = "시군구 코드는 숫자 5자리여야 합니다.")
        String districtCode,

        @Schema(description = "이전 앱 호환용 취업 상태")
        @Pattern(
                regexp = "EMPLOYED|JOB_SEEKING|UNEMPLOYED|STUDENT",
                message = "이전 취업 상태 값이 올바르지 않습니다."
        )
        String employmentStatus,

        @Schema(description = "이전 앱 호환용 소득 구간")
        @Pattern(
                regexp = "BELOW_50|BELOW_100|BELOW_150|NO_LIMIT",
                message = "소득 구간 값이 올바르지 않습니다."
        )
        String incomeRange,

        @Schema(description = "이전 앱 호환용 단일 관심 분야")
        @Pattern(
                regexp = "HOUSING|EMPLOYMENT|ASSET|CULTURE|TRANSPORT",
                message = "이전 정책 분야 값이 올바르지 않습니다."
        )
        String category,

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
        String educationStatus,

        @Schema(description = "복수 선택 관심 주제. 빈 배열이면 관심 주제 없음")
        @Size(max = 10, message = "관심 주제는 10개 이하로 선택해야 합니다.")
        Set<@Pattern(
                regexp = "EMPLOYMENT|HOUSING|EDUCATION|WELFARE_CULTURE|"
                        + "PARTICIPATION_RIGHTS|ASSET_BUILDING|TRANSPORT",
                message = "관심 주제 값이 올바르지 않습니다."
        ) String> interests
) {

    public PolicyPreferenceRequest {
        interests = interests == null ? null : Set.copyOf(new LinkedHashSet<>(interests));
    }

    public boolean usesExpandedProfile() {
        return workStatus != null
                || jobSeeking != null
                || educationStatus != null
                || interests != null;
    }

    public String resolvedWorkStatus() {
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

    public Boolean resolvedJobSeeking() {
        return jobSeeking != null
                ? jobSeeking
                : "JOB_SEEKING".equals(employmentStatus) ? Boolean.TRUE : null;
    }

    public String resolvedEducationStatus() {
        return educationStatus != null
                ? educationStatus
                : "STUDENT".equals(employmentStatus) ? "STUDENT" : null;
    }

    public Set<String> resolvedInterests() {
        if (interests != null) {
            return interests;
        }
        if (category == null) {
            return Set.of();
        }
        String legacyInterest = switch (category) {
            case "HOUSING" -> "HOUSING";
            case "EMPLOYMENT" -> "EMPLOYMENT";
            case "CULTURE" -> "WELFARE_CULTURE";
            case "ASSET" -> "ASSET_BUILDING";
            case "TRANSPORT" -> "TRANSPORT";
            default -> null;
        };
        return legacyInterest == null ? Set.of() : Set.of(legacyInterest);
    }
}
