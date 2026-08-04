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

    private static final String JOB_UNLIMITED = "0013010";
    private static final String INCOME_UNLIMITED = "0043001";

    public PolicyMatchResult match(YouthPolicyItem item, PolicySearchRequest request) {
        List<String> checkReasons = new ArrayList<>();

        if (!matchesAge(item, request.age(), checkReasons)) {
            return PolicyMatchResult.excluded();
        }
        if (!matchesRegion(item.zipCd(), request, checkReasons)) {
            return PolicyMatchResult.excluded();
        }
        if (!matchesEmployment(item, request.employmentStatus(), checkReasons)) {
            return PolicyMatchResult.excluded();
        }
        if (!matchesIncome(item, request.incomeRange(), checkReasons)) {
            return PolicyMatchResult.excluded();
        }
        if (!matchesCategory(item, request.requestedCategory())) {
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
            String employmentStatus,
            List<String> checkReasons
    ) {
        Set<String> jobCodes = tokens(item.jobCd());
        if (jobCodes.contains(JOB_UNLIMITED)) {
            return true;
        }

        if ("STUDENT".equals(employmentStatus)) {
            if (!isBlank(item.schoolCd())) {
                checkReasons.add("재학·학력 조건을 공고문에서 확인해야 합니다.");
                return true;
            }
            checkReasons.add("학생 지원 가능 여부를 공고문에서 확인해야 합니다.");
            return true;
        }

        String expectedCode = switch (employmentStatus) {
            case "EMPLOYED" -> "0013001";
            case "JOB_SEEKING", "UNEMPLOYED" -> "0013003";
            default -> null;
        };

        if (expectedCode == null || jobCodes.isEmpty()) {
            checkReasons.add("취업 상태 조건을 공고문에서 확인해야 합니다.");
            return true;
        }
        return jobCodes.contains(expectedCode);
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

    private boolean matchesCategory(YouthPolicyItem item, PolicyCategory category) {
        if (category == null) {
            return true;
        }

        String large = normalize(item.lclsfNm());
        String middle = normalize(item.mclsfNm());
        String searchableText = String.join(
                " ",
                nullToEmpty(item.plcyNm()),
                nullToEmpty(item.plcyKywdNm()),
                nullToEmpty(item.plcyExplnCn()),
                nullToEmpty(middle)
        ).toLowerCase(Locale.ROOT);

        return switch (category) {
            case HOUSING -> "주거".equals(large);
            case EMPLOYMENT -> "일자리".equals(large);
            case CULTURE -> searchableText.contains("문화")
                    || searchableText.contains("예술");
            case ASSET -> searchableText.contains("자산")
                    || searchableText.contains("금융");
            case TRANSPORT -> searchableText.contains("교통")
                    || searchableText.contains("대중교통");
        };
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
