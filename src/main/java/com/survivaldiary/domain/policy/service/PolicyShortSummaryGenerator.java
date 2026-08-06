package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.dto.PolicySupportAmountType;

import java.util.Arrays;
import java.util.Locale;

final class PolicyShortSummaryGenerator {

    String generate(
            YouthPolicyItem item,
            Long supportAmount,
            PolicySupportAmountType supportAmountType
    ) {
        BenefitTheme theme = detectTheme(item);
        if (supportAmount != null && supportAmountType != null) {
            String target = theme.amountTarget();
            return "%s%s %s 지원해요".formatted(
                    target,
                    objectParticle(target),
                    amountLabel(supportAmount, supportAmountType)
            );
        }
        return theme.defaultSummary();
    }

    private BenefitTheme detectTheme(YouthPolicyItem item) {
        String supportText = searchable(item.plcySprtCn());
        for (BenefitTheme theme : BenefitTheme.values()) {
            if (theme.matches(supportText)) {
                return theme;
            }
        }

        String fullText = String.join(
                " ",
                searchable(item.plcyNm()),
                searchable(item.plcyExplnCn()),
                searchable(item.mclsfNm()),
                searchable(item.plcyKywdNm())
        );
        for (BenefitTheme theme : BenefitTheme.values()) {
            if (theme.matches(fullText)) {
                return theme;
            }
        }

        return switch (searchable(item.lclsfNm())) {
            case "일자리" -> BenefitTheme.EMPLOYMENT;
            case "주거" -> BenefitTheme.HOUSING;
            case "교육" -> BenefitTheme.EDUCATION;
            case "복지문화" -> BenefitTheme.LIVING;
            case "참여권리" -> BenefitTheme.PARTICIPATION;
            default -> BenefitTheme.GENERAL;
        };
    }

    private String amountLabel(long amount, PolicySupportAmountType type) {
        String formatted = formatAmount(amount);
        return switch (type) {
            case FIXED -> formatted;
            case MAXIMUM -> "최대 " + formatted;
            case MONTHLY -> "월 " + formatted;
            case MONTHLY_MAXIMUM -> "월 최대 " + formatted;
        };
    }

    private String formatAmount(long amount) {
        if (amount >= 100_000_000L && amount % 100_000_000L == 0) {
            return amount / 100_000_000L + "억원";
        }
        if (amount >= 10_000L && amount % 10_000L == 0) {
            return amount / 10_000L + "만원";
        }
        if (amount >= 1_000L && amount % 1_000L == 0) {
            return amount / 1_000L + "천원";
        }
        return amount + "원";
    }

    private String objectParticle(String value) {
        if (value.isEmpty()) {
            return "을";
        }
        char last = value.charAt(value.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) {
            return "을";
        }
        return (last - 0xAC00) % 28 == 0 ? "를" : "을";
    }

    private String searchable(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private enum BenefitTheme {
        RENT(
                "청년의 월세와 주거비",
                "청년의 월세와 주거비 부담을 덜어줘요",
                "월세", "임차료", "임대료", "전월세", "주거비"
        ),
        TRANSPORT(
                "청년의 교통비",
                "청년의 교통비 부담을 덜어줘요",
                "교통비", "대중교통", "통학", "통근"
        ),
        CERTIFICATION(
                "청년의 자격증과 응시료",
                "청년의 자격증 취득과 응시료를 지원해요",
                "응시료", "자격증"
        ),
        STARTUP(
                "청년의 창업 비용",
                "청년의 창업 준비와 사업 운영을 지원해요",
                "창업", "사업화", "사업비"
        ),
        EMPLOYMENT(
                "청년의 취업 준비 비용",
                "청년의 취업과 일자리 준비를 지원해요",
                "취업", "구직", "면접", "채용", "일자리"
        ),
        EDUCATION(
                "청년의 교육과 훈련 비용",
                "청년의 교육과 훈련 참여를 지원해요",
                "교육", "훈련", "강의", "학습", "장학"
        ),
        CULTURE(
                "청년의 문화 활동 비용",
                "청년의 문화와 예술 활동 참여를 지원해요",
                "문화", "공연", "예술", "여가"
        ),
        ASSET(
                "청년의 자산 형성",
                "청년의 저축과 자산 형성을 지원해요",
                "저축", "자산", "목돈", "금융", "통장"
        ),
        HEALTH(
                "청년의 건강 관리 비용",
                "청년의 건강 관리와 의료비를 지원해요",
                "건강", "의료", "치료", "검진"
        ),
        LIVING(
                "청년의 생활비",
                "청년의 생활비 부담을 덜어줘요",
                "생활비", "활동비", "수당"
        ),
        HOUSING(
                "청년의 주거 비용",
                "청년의 안정적인 주거 생활을 지원해요",
                "주거", "임대", "이사"
        ),
        PARTICIPATION(
                "청년의 사회 참여 활동",
                "청년의 사회 참여와 권리 활동을 지원해요",
                "참여", "권리", "청년 활동"
        ),
        GENERAL(
                "청년의 정책 이용",
                "청년에게 필요한 정책 혜택을 지원해요"
        );

        private final String amountTarget;
        private final String defaultSummary;
        private final String[] keywords;

        BenefitTheme(String amountTarget, String defaultSummary, String... keywords) {
            this.amountTarget = amountTarget;
            this.defaultSummary = defaultSummary;
            this.keywords = keywords;
        }

        private boolean matches(String text) {
            return !text.isBlank() && Arrays.stream(keywords).anyMatch(text::contains);
        }

        private String amountTarget() {
            return amountTarget;
        }

        private String defaultSummary() {
            return defaultSummary;
        }
    }
}
