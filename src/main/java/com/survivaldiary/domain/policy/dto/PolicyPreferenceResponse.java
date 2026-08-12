package com.survivaldiary.domain.policy.dto;

import com.survivaldiary.domain.policy.entity.PolicyPreference;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "로그인 사용자의 맞춤 정책 기본 조건")
public record PolicyPreferenceResponse(
        @Schema(description = "저장된 기본 조건 존재 여부") boolean saved,
        @Schema(description = "생년월일로 계산한 만 나이. 생년월일이 없으면 null") Integer age,
        @Schema(description = "법정동 시도 코드 앞 2자리") String regionCode,
        @Schema(description = "법정동 시군구 코드 앞 5자리") String districtCode,
        @Schema(description = "이전 앱 호환용 취업 상태") String employmentStatus,
        @Schema(description = "이전 앱 호환용 소득 구간") String incomeRange,
        @Schema(description = "이전 앱 호환용 정책 분야") String category,
        @Schema(description = "근로 상태. 미입력이면 null") String workStatus,
        @Schema(description = "구직 여부. 미입력이면 null") Boolean jobSeeking,
        @Schema(description = "교육 상태. 미입력이면 null") String educationStatus,
        @Schema(description = "복수 선택 관심 주제") Set<String> interests,
        @Schema(description = "교육 단계. 미입력이면 null") String educationLevel,
        @Schema(description = "현재 학적 상태. 미입력이면 null") String enrollmentStatus
) {
    public PolicyPreferenceResponse(
            boolean saved,
            Integer age,
            String regionCode,
            String districtCode,
            String employmentStatus,
            String incomeRange,
            String category,
            String workStatus,
            Boolean jobSeeking,
            String educationStatus,
            Set<String> interests
    ) {
        this(
                saved,
                age,
                regionCode,
                districtCode,
                employmentStatus,
                incomeRange,
                category,
                workStatus,
                jobSeeking,
                educationStatus,
                interests,
                null,
                null
        );
    }

    public static PolicyPreferenceResponse empty(Integer age) {
        return new PolicyPreferenceResponse(
                false,
                age,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Set.of(),
                null,
                null
        );
    }

    public static PolicyPreferenceResponse from(
            PolicyPreference preference,
            Integer age
    ) {
        String enrollmentStatus = normalizedEnrollmentStatus(preference.getEducationStatus());
        return new PolicyPreferenceResponse(
                true,
                age,
                preference.getRegionCode(),
                preference.getDistrictCode(),
                preference.getEmploymentStatus(),
                preference.getIncomeRange(),
                preference.getCategory(),
                preference.getWorkStatus(),
                preference.getJobSeeking(),
                legacyEducationStatus(enrollmentStatus),
                Set.copyOf(preference.getInterests()),
                preference.getEducationLevel(),
                enrollmentStatus
        );
    }

    private static String normalizedEnrollmentStatus(String storedStatus) {
        if (storedStatus == null) {
            return null;
        }
        return switch (storedStatus) {
            case "STUDENT" -> "ENROLLED";
            case "NOT_STUDENT", "OTHER" -> "NOT_APPLICABLE";
            default -> storedStatus;
        };
    }

    private static String legacyEducationStatus(String enrollmentStatus) {
        if (enrollmentStatus == null) {
            return null;
        }
        return switch (enrollmentStatus) {
            case "ENROLLED", "EXPECTED_GRADUATION" -> "STUDENT";
            case "ON_LEAVE" -> "ON_LEAVE";
            case "GRADUATED" -> "GRADUATED";
            case "DROPPED_OUT", "NOT_APPLICABLE" -> "NOT_STUDENT";
            default -> null;
        };
    }
}
