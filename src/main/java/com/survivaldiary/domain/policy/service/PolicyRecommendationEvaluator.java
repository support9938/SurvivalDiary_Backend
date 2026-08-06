package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.dto.PolicyCategory;
import com.survivaldiary.domain.policy.dto.PolicyEligibilityStatus;
import com.survivaldiary.domain.policy.dto.PolicyMatchSignal;
import com.survivaldiary.domain.policy.dto.PolicyRecommendationStatus;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class PolicyRecommendationEvaluator {

    private static final int RECOMMENDED_PRIORITY = 1_000;
    private static final int CHECK_REQUIRED_PRIORITY = 500;
    private static final int DISCOVER_PRIORITY = 100;
    private static final int RECOMMENDATION_THRESHOLD = 30;

    public PolicyRecommendationResult evaluate(
            YouthPolicyItem item,
            PolicySearchRequest request,
            PolicyMatchResult matchResult
    ) {
        List<String> positiveReasons = new ArrayList<>();
        Set<PolicyMatchSignal> signals = new LinkedHashSet<>();
        int personalizationScore = 0;

        PolicyTargetClassifier.JobMatch jobMatch = PolicyTargetClassifier.classifyJob(
                item.jobCd(),
                request.requestedWorkStatus()
        );
        if (jobMatch == PolicyTargetClassifier.JobMatch.MATCHED) {
            positiveReasons.add("%s 대상 조건과 일치해요."
                    .formatted(workStatusLabel(request.requestedWorkStatus())));
            signals.add(PolicyMatchSignal.WORK_STATUS);
            personalizationScore += 40;
        }

        String text = searchableText(item);
        if (Boolean.TRUE.equals(request.jobSeeking())
                && containsAny(text, "구직", "미취업", "취업준비", "취업 준비", "채용", "면접", "실업")) {
            positiveReasons.add("현재 구직 상황에 직접 관련된 정책이에요.");
            signals.add(PolicyMatchSignal.JOB_SEEKING);
            personalizationScore += 35;
        }

        if (matchesEducationStatus(text, request.requestedEducationStatus())) {
            positiveReasons.add("%s 교육 상태와 관련된 정책이에요."
                    .formatted(educationStatusLabel(request.requestedEducationStatus())));
            signals.add(PolicyMatchSignal.EDUCATION_STATUS);
            personalizationScore += 30;
        }

        InterestMatch interestMatch = interestMatch(item, request.requestedInterests());
        if (interestMatch.matched()) {
            positiveReasons.add("관심 주제인 %s 분야와 관련된 정책이에요."
                    .formatted(interestMatch.label()));
            signals.add(interestMatch.signal());
            personalizationScore += 35;
        }

        RegionMatch regionMatch = regionMatch(item.zipCd(), request);
        if (regionMatch.reason() != null) {
            positiveReasons.add(regionMatch.reason());
        }
        if (regionMatch.signal() != null) {
            signals.add(regionMatch.signal());
            personalizationScore += regionMatch.score();
        }

        if (matchesSpecificAge(item, request.age())) {
            positiveReasons.add("만 %d세 연령 조건과 일치해요.".formatted(request.age()));
            signals.add(PolicyMatchSignal.AGE);
            personalizationScore += 8;
        }

        if (personalizationScore >= RECOMMENDATION_THRESHOLD) {
            int eligibilityBonus = matchResult.status() == PolicyEligibilityStatus.MATCHED ? 20 : 0;
            return new PolicyRecommendationResult(
                    PolicyRecommendationStatus.RECOMMENDED,
                    positiveReasons.stream().limit(3).toList(),
                    List.copyOf(signals),
                    RECOMMENDED_PRIORITY + personalizationScore + eligibilityBonus
            );
        }

        if (matchResult.status() == PolicyEligibilityStatus.CHECK_REQUIRED) {
            List<String> reasons = new ArrayList<>(matchResult.reasons());
            positiveReasons.stream()
                    .filter(reason -> reasons.size() < 3)
                    .forEach(reasons::add);
            return new PolicyRecommendationResult(
                    PolicyRecommendationStatus.CHECK_REQUIRED,
                    reasons,
                    List.copyOf(signals),
                    CHECK_REQUIRED_PRIORITY + personalizationScore
            );
        }

        List<String> reasons = positiveReasons.stream().limit(2).toList();
        if (reasons.isEmpty()) {
            reasons = List.of("입력한 기본 조건 범위에서 함께 살펴볼 수 있어요.");
        }
        return new PolicyRecommendationResult(
                PolicyRecommendationStatus.DISCOVER,
                reasons,
                List.copyOf(signals),
                DISCOVER_PRIORITY + personalizationScore
        );
    }

    private InterestMatch interestMatch(YouthPolicyItem item, Set<String> interests) {
        PolicyCategory category = categoryType(item);
        if (category != null && interests.contains(category.name())) {
            return new InterestMatch(
                    true,
                    categoryLabel(category),
                    interestSignal(category)
            );
        }

        String text = searchableText(item);
        if (interests.contains("ASSET_BUILDING")
                && containsAny(text, "자산", "금융", "저축", "목돈", "재무")) {
            return new InterestMatch(
                    true,
                    "자산 형성",
                    PolicyMatchSignal.INTEREST_ASSET_BUILDING
            );
        }
        if (interests.contains("TRANSPORT")
                && containsAny(text, "교통", "대중교통", "통학", "통근")) {
            return new InterestMatch(
                    true,
                    "교통",
                    PolicyMatchSignal.INTEREST_TRANSPORT
            );
        }
        return new InterestMatch(false, null, null);
    }

    private RegionMatch regionMatch(String zipCodes, PolicySearchRequest request) {
        if (isBlank(zipCodes)) {
            return RegionMatch.none();
        }
        if (zipCodes.contains("전국")) {
            return new RegionMatch("전국에서 신청할 수 있는 정책이에요.", null, 0);
        }

        Set<String> codes = tokens(zipCodes);
        if (request.districtCode() != null && codes.contains(request.districtCode())) {
            return new RegionMatch(
                    "선택한 시·군·구 거주 조건과 일치해요.",
                    PolicyMatchSignal.DISTRICT,
                    18
            );
        }
        if (codes.stream().anyMatch(code -> code.startsWith(request.regionCode()))) {
            return new RegionMatch(
                    "선택한 시·도 거주 조건과 일치해요.",
                    PolicyMatchSignal.REGION,
                    12
            );
        }
        return RegionMatch.none();
    }

    private boolean matchesSpecificAge(YouthPolicyItem item, int age) {
        if ("N".equalsIgnoreCase(normalize(item.sprtTrgtAgeLmtYn()))) {
            return false;
        }
        Integer minAge = parseInteger(item.sprtTrgtMinAge());
        Integer maxAge = parseInteger(item.sprtTrgtMaxAge());
        return minAge != null && maxAge != null && age >= minAge && age <= maxAge;
    }

    private boolean matchesEducationStatus(String text, String educationStatus) {
        if (educationStatus == null) {
            return false;
        }
        return switch (educationStatus) {
            case "STUDENT" -> containsAny(text, "재학생", "재학", "대학생", "학생");
            case "ON_LEAVE" -> containsAny(text, "휴학생", "휴학");
            case "GRADUATED" -> containsAny(text, "졸업생", "졸업자", "졸업");
            case "NOT_STUDENT" -> containsAny(text, "비진학", "학교 밖", "미진학");
            default -> false;
        };
    }

    private String workStatusLabel(String workStatus) {
        return switch (workStatus) {
            case "EMPLOYED" -> "재직자";
            case "SELF_EMPLOYED" -> "자영업자";
            case "UNEMPLOYED" -> "미취업자";
            case "FREELANCER" -> "프리랜서";
            case "DAILY_WORKER" -> "일용근로자";
            case "PROSPECTIVE_FOUNDER" -> "예비창업자";
            case "SHORT_TERM_WORKER" -> "단기근로자";
            case "FARMER" -> "영농종사자";
            default -> "현재 근로 상태";
        };
    }

    private String educationStatusLabel(String educationStatus) {
        return switch (educationStatus) {
            case "STUDENT" -> "재학 중인";
            case "ON_LEAVE" -> "휴학 중인";
            case "GRADUATED" -> "졸업한";
            case "NOT_STUDENT" -> "학생이 아닌";
            default -> "현재";
        };
    }

    private PolicyMatchSignal interestSignal(PolicyCategory category) {
        return switch (category) {
            case EMPLOYMENT -> PolicyMatchSignal.INTEREST_EMPLOYMENT;
            case HOUSING -> PolicyMatchSignal.INTEREST_HOUSING;
            case EDUCATION -> PolicyMatchSignal.INTEREST_EDUCATION;
            case WELFARE_CULTURE -> PolicyMatchSignal.INTEREST_WELFARE_CULTURE;
            case PARTICIPATION_RIGHTS -> PolicyMatchSignal.INTEREST_PARTICIPATION_RIGHTS;
        };
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

    private record InterestMatch(
            boolean matched,
            String label,
            PolicyMatchSignal signal
    ) {
    }

    private record RegionMatch(
            String reason,
            PolicyMatchSignal signal,
            int score
    ) {
        private static RegionMatch none() {
            return new RegionMatch(null, null, 0);
        }
    }
}
