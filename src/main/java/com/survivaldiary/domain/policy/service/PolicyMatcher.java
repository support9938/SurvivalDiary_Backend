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
        addEmploymentCheck(item, request.requestedWorkStatus(), checkReasons);
        addEducationCheck(item, request.requestedEducationStatus(), checkReasons);
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

    private void addEmploymentCheck(
            YouthPolicyItem item,
            String workStatus,
            List<String> checkReasons
    ) {
        if (workStatus == null) {
            return;
        }

        Set<String> jobCodes = tokens(item.jobCd());
        if (jobCodes.contains(JOB_UNLIMITED)) {
            return;
        }

        String expectedCode = switch (workStatus) {
            case "EMPLOYED" -> "0013001";
            case "SELF_EMPLOYED" -> "0013002";
            case "UNEMPLOYED" -> "0013003";
            case "FREELANCER" -> "0013004";
            case "DAILY_WORKER" -> "0013005";
            case "PROSPECTIVE_FOUNDER" -> "0013006";
            case "SHORT_TERM_WORKER" -> "0013007";
            case "FARMER" -> "0013008";
            case "OTHER" -> "0013009";
            default -> null;
        };

        if (expectedCode == null || jobCodes.isEmpty()) {
            checkReasons.add("근로 상태 조건을 공고문에서 확인해야 합니다.");
            return;
        }
        if (!jobCodes.contains(expectedCode)) {
            checkReasons.add("현재 근로 상태로 신청할 수 있는지 확인해야 합니다.");
        }
    }

    private void addEducationCheck(
            YouthPolicyItem item,
            String educationStatus,
            List<String> checkReasons
    ) {
        if (educationStatus != null && !isBlank(item.schoolCd())) {
            checkReasons.add("재학·학력 조건을 공고문에서 확인해야 합니다.");
        }
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
