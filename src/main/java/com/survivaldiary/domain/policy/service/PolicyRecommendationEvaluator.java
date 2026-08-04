package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.dto.PolicyCategory;
import com.survivaldiary.domain.policy.dto.PolicyEligibilityStatus;
import com.survivaldiary.domain.policy.dto.PolicyRecommendationStatus;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class PolicyRecommendationEvaluator {

    private static final int RECOMMENDED_PRIORITY = 300;
    private static final int CHECK_REQUIRED_PRIORITY = 200;
    private static final int DISCOVER_PRIORITY = 100;

    public PolicyRecommendationResult evaluate(
            YouthPolicyItem item,
            PolicySearchRequest request,
            PolicyMatchResult matchResult
    ) {
        InterestMatch interestMatch = interestMatch(item, request.requestedInterests());
        List<String> positiveReasons = new ArrayList<>();

        if (interestMatch.matched()) {
            positiveReasons.add("관심 주제인 %s 분야와 관련된 정책이에요."
                    .formatted(interestMatch.label()));
        }
        if (Boolean.TRUE.equals(request.jobSeeking())
                && categoryType(item) == PolicyCategory.EMPLOYMENT) {
            positiveReasons.add("구직 중인 사용자에게 관련된 일자리 정책이에요.");
        }
        if (request.requestedEducationStatus() != null
                && categoryType(item) == PolicyCategory.EDUCATION) {
            positiveReasons.add("현재 교육 상태와 관련된 정책이에요.");
        }

        String regionReason = regionReason(item.zipCd(), request);
        if (regionReason != null) {
            positiveReasons.add(regionReason);
        }

        String ageReason = ageReason(item, request.age());
        if (ageReason != null) {
            positiveReasons.add(ageReason);
        }

        int signalCount = Math.min(positiveReasons.size(), 3);
        if (matchResult.status() == PolicyEligibilityStatus.CHECK_REQUIRED) {
            List<String> reasons = new ArrayList<>(matchResult.reasons());
            positiveReasons.stream()
                    .filter(reason -> reasons.size() < 3)
                    .forEach(reasons::add);
            return new PolicyRecommendationResult(
                    PolicyRecommendationStatus.CHECK_REQUIRED,
                    reasons,
                    CHECK_REQUIRED_PRIORITY + signalCount
            );
        }

        if (interestMatch.matched()
                || Boolean.TRUE.equals(request.jobSeeking())
                        && categoryType(item) == PolicyCategory.EMPLOYMENT
                || request.requestedEducationStatus() != null
                        && categoryType(item) == PolicyCategory.EDUCATION) {
            return new PolicyRecommendationResult(
                    PolicyRecommendationStatus.RECOMMENDED,
                    positiveReasons.stream().limit(3).toList(),
                    RECOMMENDED_PRIORITY + signalCount
            );
        }

        List<String> reasons = positiveReasons.stream().limit(2).toList();
        if (reasons.isEmpty()) {
            reasons = List.of("입력한 기본 조건 범위에서 함께 살펴볼 수 있어요.");
        }
        return new PolicyRecommendationResult(
                PolicyRecommendationStatus.DISCOVER,
                reasons,
                DISCOVER_PRIORITY + signalCount
        );
    }

    private InterestMatch interestMatch(YouthPolicyItem item, Set<String> interests) {
        PolicyCategory category = categoryType(item);
        if (category != null && interests.contains(category.name())) {
            return new InterestMatch(true, categoryLabel(category));
        }

        String text = searchableText(item);
        if (interests.contains("ASSET_BUILDING")
                && containsAny(text, "자산", "금융", "저축", "목돈", "재무")) {
            return new InterestMatch(true, "자산 형성");
        }
        if (interests.contains("TRANSPORT")
                && containsAny(text, "교통", "대중교통", "통학", "통근")) {
            return new InterestMatch(true, "교통");
        }
        return new InterestMatch(false, null);
    }

    private String regionReason(String zipCodes, PolicySearchRequest request) {
        if (isBlank(zipCodes)) {
            return null;
        }
        if (zipCodes.contains("전국")) {
            return "전국에서 신청할 수 있는 정책이에요.";
        }

        Set<String> codes = tokens(zipCodes);
        if (request.districtCode() != null && codes.contains(request.districtCode())) {
            return "선택한 시·군·구 거주 조건과 일치해요.";
        }
        if (codes.stream().anyMatch(code -> code.startsWith(request.regionCode()))) {
            return "선택한 시·도 거주 조건과 일치해요.";
        }
        return null;
    }

    private String ageReason(YouthPolicyItem item, int age) {
        if ("N".equalsIgnoreCase(normalize(item.sprtTrgtAgeLmtYn()))) {
            return "연령 제한 없이 신청할 수 있어요.";
        }
        Integer minAge = parseInteger(item.sprtTrgtMinAge());
        Integer maxAge = parseInteger(item.sprtTrgtMaxAge());
        if (minAge != null && maxAge != null && age >= minAge && age <= maxAge) {
            return "만 %d세 연령 조건과 일치해요.".formatted(age);
        }
        return null;
    }

    private PolicyCategory categoryType(YouthPolicyItem item) {
        String category = normalize(item.lclsfNm());
        if (category == null) {
            return null;
        }
        return switch (category) {
            case "일자리" -> PolicyCategory.EMPLOYMENT;
            case "주거" -> PolicyCategory.HOUSING;
            case "교육" -> PolicyCategory.EDUCATION;
            case "복지문화" -> PolicyCategory.WELFARE_CULTURE;
            case "참여권리" -> PolicyCategory.PARTICIPATION_RIGHTS;
            default -> null;
        };
    }

    private String categoryLabel(PolicyCategory category) {
        return switch (category) {
            case EMPLOYMENT -> "취업";
            case HOUSING -> "주거";
            case EDUCATION -> "교육";
            case WELFARE_CULTURE -> "복지·문화";
            case PARTICIPATION_RIGHTS -> "참여·권리";
        };
    }

    private String searchableText(YouthPolicyItem item) {
        return String.join(
                " ",
                nullToEmpty(item.plcyNm()),
                nullToEmpty(item.plcyKywdNm()),
                nullToEmpty(item.plcyExplnCn()),
                nullToEmpty(item.plcySprtCn()),
                nullToEmpty(item.mclsfNm())
        ).toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
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

    private record InterestMatch(boolean matched, String label) {
    }
}
