package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.dto.PolicyCategory;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class PolicyMatcher {

    private static final String INCOME_UNLIMITED = "0043001";

    public PolicyMatchResult match(YouthPolicyItem item, PolicySearchRequest request) {
        List<String> checkReasons = new ArrayList<>();

        if (!matchesAge(item, request.age(), checkReasons)) {
            return PolicyMatchResult.excluded();
        }
        if (!matchesRegion(item.zipCd(), request, checkReasons)) {
            return PolicyMatchResult.excluded();
        }
        if (!matchesEmployment(item, request.requestedWorkStatus(), checkReasons)) {
            return PolicyMatchResult.excluded();
        }
        if (!matchesEducation(item, request, checkReasons)) {
            return PolicyMatchResult.excluded();
        }
        if (PolicyInstitutionClassifier.isUniversitySpecific(item)) {
            checkReasons.add("특정 대학 운영 프로그램으로 이용 가능 대상을 확인해야 합니다.");
        }
        if (!matchesIncome(item, request.incomeRange(), checkReasons)) {
            return PolicyMatchResult.excluded();
        }
        if (!matchesCategory(item, request)) {
            return PolicyMatchResult.excluded();
        }

        return checkReasons.isEmpty()
                ? PolicyMatchResult.matched()
                : PolicyMatchResult.checkRequired(checkReasons);
    }

    private boolean matchesAge(
            YouthPolicyItem item,
            int age,
            List<String> checkReasons
    ) {
        if ("N".equalsIgnoreCase(normalize(item.sprtTrgtAgeLmtYn()))) {
            return true;
        }

        Integer minAge = parseInteger(item.sprtTrgtMinAge());
        Integer maxAge = parseInteger(item.sprtTrgtMaxAge());
        if (minAge == null || maxAge == null) {
            checkReasons.add("연령 조건을 공고문에서 확인해야 합니다.");
            return true;
        }
        return age >= minAge && age <= maxAge;
    }

    private boolean matchesRegion(
            String zipCodes,
            PolicySearchRequest request,
            List<String> checkReasons
    ) {
        if (isBlank(zipCodes)) {
            checkReasons.add("거주지역 조건을 공고문에서 확인해야 합니다.");
            return true;
        }
        if (zipCodes.contains("전국")) {
            return true;
        }

        Set<String> codes = tokens(zipCodes);
        String districtCode = request.districtCode();
        if (districtCode == null) {
            return codes.stream().anyMatch(code -> code.startsWith(request.regionCode()));
        }
        if (codes.contains(districtCode)) {
            return true;
        }
        return codes.contains(request.regionCode() + "000");
    }

    private boolean matchesEmployment(
            YouthPolicyItem item,
            String workStatus,
            List<String> checkReasons
    ) {
        return switch (PolicyTargetClassifier.classifyJob(item.jobCd(), workStatus)) {
            case NOT_REQUESTED, UNRESTRICTED, MATCHED -> true;
            case MISMATCHED -> false;
            case UNKNOWN -> {
                checkReasons.add("근로 상태 조건을 공고문에서 확인해야 합니다.");
                yield true;
            }
        };
    }

    private boolean matchesEducation(
            YouthPolicyItem item,
            PolicySearchRequest request,
            List<String> checkReasons
    ) {
        return switch (PolicyEducationClassifier.classify(
                item.schoolCd(),
                request.educationLevel(),
                request.requestedEnrollmentStatus()
        )) {
            case NOT_REQUESTED, UNRESTRICTED, MATCHED -> true;
            case MISMATCHED -> false;
            case UNKNOWN -> {
                checkReasons.add("교육 단계·학적 조건을 공고문에서 확인해야 합니다.");
                yield true;
            }
        };
    }

    private boolean matchesIncome(
            YouthPolicyItem item,
            String incomeRange,
            List<String> checkReasons
    ) {
        if (incomeRange == null || "NO_LIMIT".equals(incomeRange)) {
            return true;
        }
        if (INCOME_UNLIMITED.equals(normalize(item.earnCndSeCd()))) {
            return true;
        }

        checkReasons.add("중위소득 조건을 공고문에서 확인해야 합니다.");
        return true;
    }

    private boolean matchesCategory(YouthPolicyItem item, PolicySearchRequest request) {
        String requestedCode = request.category();
        if ("ASSET".equals(requestedCode)) {
            return searchableText(item).contains("자산")
                    || searchableText(item).contains("금융");
        }
        if ("TRANSPORT".equals(requestedCode)) {
            return searchableText(item).contains("교통")
                    || searchableText(item).contains("대중교통");
        }

        PolicyCategory category = request.requestedCategory();
        if (category == null) {
            return true;
        }

        String large = normalize(item.lclsfNm());
        return switch (category) {
            case EMPLOYMENT -> "일자리".equals(large);
            case HOUSING -> "주거".equals(large);
            case EDUCATION -> "교육".equals(large);
            case WELFARE_CULTURE -> "복지문화".equals(large);
            case PARTICIPATION_RIGHTS -> "참여권리".equals(large);
        };
    }

    private String searchableText(YouthPolicyItem item) {
        return String.join(
                " ",
                nullToEmpty(item.plcyNm()),
                nullToEmpty(item.plcyKywdNm()),
                nullToEmpty(item.plcyExplnCn()),
                nullToEmpty(item.mclsfNm())
        ).toLowerCase(Locale.ROOT);
    }

    private Set<String> tokens(String value) {
        if (isBlank(value)) {
            return Set.of();
        }
        return Set.copyOf(Arrays.stream(value.split("[,|\\s]+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList());
    }

    private Integer parseInteger(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
