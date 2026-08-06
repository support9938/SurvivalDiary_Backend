package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.dto.PolicyApplicationPeriodType;
import com.survivaldiary.domain.policy.dto.PolicyEligibilityStatus;
import com.survivaldiary.domain.policy.dto.PolicyMatchSignal;
import com.survivaldiary.domain.policy.dto.PolicyOfficialLinkType;
import com.survivaldiary.domain.policy.dto.PolicyRecommendationStatus;
import com.survivaldiary.domain.policy.dto.PolicySupportAmountType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyMapperTest {

    private final PolicyMapper mapper = new PolicyMapper();

    @Test
    void 목록에는_근거가_없는_지원금과_조건_확인_상태를_안전하게_반환한다() {
        var summary = mapper.toSummary(
                item("not-a-url"),
                PolicyMatchResult.checkRequired(List.of("소득 조건 확인 필요")),
                new PolicyRecommendationResult(
                        PolicyRecommendationStatus.CHECK_REQUIRED,
                        List.of("소득 조건 확인 필요"),
                        List.of(PolicyMatchSignal.DISTRICT),
                        200
                )
        );

        assertThat(summary.policyId()).isEqualTo("POLICY-1");
        assertThat(summary.supportAmount()).isNull();
        assertThat(summary.eligibilityStatus())
                .isEqualTo(PolicyEligibilityStatus.CHECK_REQUIRED);
        assertThat(summary.eligibilityReasons()).containsExactly("소득 조건 확인 필요");
        assertThat(summary.recommendationStatus())
                .isEqualTo(PolicyRecommendationStatus.CHECK_REQUIRED);
        assertThat(summary.matchSignals()).containsExactly(PolicyMatchSignal.DISTRICT);
        assertThat(summary.applicationPeriodType())
                .isEqualTo(PolicyApplicationPeriodType.FIXED);
        assertThat(summary.applicationStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(summary.applicationEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    void 근거가_하나인_지원금만_지급_단위와_함께_변환한다() {
        var monthlyMaximum = mapper.toDetail(
                withSupportText(item("https://example.org/apply"), "월 최대 20만 원, 최대 12개월 지원")
        );
        var maximum = mapper.toDetail(
                withSupportText(item("https://example.org/apply"), "최대 300만원을 지급합니다.")
        );
        var fixed = mapper.toDetail(
                withSupportText(item("https://example.org/apply"), "취업 준비금 500,000원 지급")
        );
        var monthlySuffix = mapper.toDetail(
                withSupportText(item("https://example.org/apply"), "주거비 200,000원/월 지원")
        );
        var maximumSuffix = mapper.toDetail(
                withSupportText(item("https://example.org/apply"), "자격증 응시료 10만 원 이내 지원")
        );

        assertThat(monthlyMaximum.supportAmount()).isEqualTo(200_000L);
        assertThat(monthlyMaximum.supportAmountType())
                .isEqualTo(PolicySupportAmountType.MONTHLY_MAXIMUM);
        assertThat(maximum.supportAmount()).isEqualTo(3_000_000L);
        assertThat(maximum.supportAmountType()).isEqualTo(PolicySupportAmountType.MAXIMUM);
        assertThat(fixed.supportAmount()).isEqualTo(500_000L);
        assertThat(fixed.supportAmountType()).isEqualTo(PolicySupportAmountType.FIXED);
        assertThat(monthlySuffix.supportAmount()).isEqualTo(200_000L);
        assertThat(monthlySuffix.supportAmountType()).isEqualTo(PolicySupportAmountType.MONTHLY);
        assertThat(maximumSuffix.supportAmount()).isEqualTo(100_000L);
        assertThat(maximumSuffix.supportAmountType()).isEqualTo(PolicySupportAmountType.MAXIMUM);
    }

    @Test
    void 목록용_요약은_지원_주제와_구조화_금액을_짧은_문장으로_조합한다() {
        var summary = mapper.toSummary(
                withSupportText(
                        item("https://example.org/apply"),
                        "□ 지원 기간은 최대 12개월\n○ 월세를 월 최대 20만 원 지원"
                ),
                PolicyMatchResult.matched(),
                new PolicyRecommendationResult(
                        PolicyRecommendationStatus.RECOMMENDED,
                        List.of(),
                        300
                )
        );

        assertThat(summary.shortSummary())
                .isEqualTo("청년의 월세와 주거비를 월 최대 20만원 지원해요")
                .doesNotContain("○", "□", "•");
    }

    @Test
    void 금액이_없어도_전체_지원_내용의_주제를_분석해_한_줄로_요약한다() {
        var summary = mapper.toSummary(
                withSupportText(
                        item("https://example.org/apply"),
                        "■ 참여자를 위한 종합 안내\n○ 면접 교육과 취업 상담 제공"
                ),
                PolicyMatchResult.matched(),
                new PolicyRecommendationResult(
                        PolicyRecommendationStatus.RECOMMENDED,
                        List.of(),
                        300
                )
        );

        assertThat(summary.shortSummary())
                .isEqualTo("청년의 취업과 일자리 준비를 지원해요")
                .doesNotContain("○", "■", "•");
    }

    @Test
    void 범위_복수_금액과_대출_금액은_지원금으로_추정하지_않는다() {
        var range = mapper.toDetail(
                withSupportText(item("https://example.org/apply"), "월 10~20만원 지원")
        );
        var multiple = mapper.toDetail(
                withSupportText(item("https://example.org/apply"), "월 20만원, 최대 240만원 지원")
        );
        var loan = mapper.toDetail(
                withSupportText(item("https://example.org/apply"), "대출 최대 1억원")
        );
        var yearly = mapper.toDetail(
                withSupportText(item("https://example.org/apply"), "연 최대 300만원 지원")
        );
        var unclearQualifier = mapper.toDetail(
                withSupportText(item("https://example.org/apply"), "월 최대 약 20만원 지원")
        );
        var budget = mapper.toDetail(
                withSupportText(item("https://example.org/apply"), "응시료의 50% 지원, 예산 100억 원 소진 시 마감")
        );
        var hourlyWage = mapper.toDetail(
                withSupportText(item("https://example.org/apply"), "근무 보수는 시급 10,000원입니다.")
        );

        assertThat(range.supportAmount()).isNull();
        assertThat(multiple.supportAmount()).isNull();
        assertThat(loan.supportAmount()).isNull();
        assertThat(yearly.supportAmount()).isNull();
        assertThat(unclearQualifier.supportAmount()).isNull();
        assertThat(budget.supportAmount()).isNull();
        assertThat(hourlyWage.supportAmount()).isNull();
    }

    @Test
    void 신청_기간은_명확한_유형과_날짜만_구조화한다() {
        YouthPolicyItem source = item("https://example.org/apply");

        var fixedPeriod = mapper.toDetail(source);
        var alwaysOpen = mapper.toDetail(withPeriod(source, "0057002", "상시"));
        var closed = mapper.toDetail(withPeriod(source, "0057003", "마감"));
        var untilBudget = mapper.toDetail(withPeriod(source, "0057002", "예산 소진 시까지"));
        var invalidRange = mapper.toDetail(withPeriod(source, "0057001", "20260731~20260701"));
        var explanatoryText = mapper.toDetail(
                withPeriod(source, "0057001", "20260701~20260731 예정")
        );

        assertThat(fixedPeriod.applicationPeriodType())
                .isEqualTo(PolicyApplicationPeriodType.FIXED);
        assertThat(fixedPeriod.applicationStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(fixedPeriod.applicationEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(alwaysOpen.applicationPeriodType())
                .isEqualTo(PolicyApplicationPeriodType.ALWAYS);
        assertThat(alwaysOpen.applicationEndDate()).isNull();
        assertThat(closed.applicationPeriodType())
                .isEqualTo(PolicyApplicationPeriodType.CLOSED);
        assertThat(untilBudget.applicationPeriodType())
                .isEqualTo(PolicyApplicationPeriodType.UNTIL_BUDGET);
        assertThat(invalidRange.applicationPeriodType())
                .isEqualTo(PolicyApplicationPeriodType.UNKNOWN);
        assertThat(invalidRange.applicationEndDate()).isNull();
        assertThat(explanatoryText.applicationPeriodType())
                .isEqualTo(PolicyApplicationPeriodType.UNKNOWN);
        assertThat(explanatoryText.applicationEndDate()).isNull();
    }

    @Test
    void 상세는_유효한_HTTP_URL만_외부_링크로_반환한다() {
        var detail = mapper.toDetail(item("javascript:alert(1)"));

        assertThat(detail.officialUrl()).isNull();
        assertThat(detail.officialLinkType()).isEqualTo(PolicyOfficialLinkType.UNAVAILABLE);
        assertThat(detail.referenceUrls())
                .containsExactly("https://example.org/reference");
        assertThat(detail.documents()).containsExactly("주민등록등본");
    }

    @Test
    void 공식_신청_URL의_이동_성격을_보수적으로_분류한다() {
        var application = mapper.toDetail(item("https://example.org/apply/form"));
        var login = mapper.toDetail(item("https://example.org/login?service=apply"));
        var homepage = mapper.toDetail(item("https://example.org/"));
        var queryApplication = mapper.toDetail(item("https://example.org/?policy=POLICY-1"));
        var accountRequired = mapper.toDetail(item("https://example.org/security/joinAgree"));

        assertThat(application.officialLinkType())
                .isEqualTo(PolicyOfficialLinkType.APPLICATION_CANDIDATE);
        assertThat(login.officialLinkType())
                .isEqualTo(PolicyOfficialLinkType.LOGIN_REQUIRED);
        assertThat(homepage.officialLinkType())
                .isEqualTo(PolicyOfficialLinkType.INSTITUTION_HOME);
        assertThat(queryApplication.officialLinkType())
                .isEqualTo(PolicyOfficialLinkType.APPLICATION_CANDIDATE);
        assertThat(accountRequired.officialLinkType())
                .isEqualTo(PolicyOfficialLinkType.LOGIN_REQUIRED);
    }

    private YouthPolicyItem item(String applicationUrl) {
        return new YouthPolicyItem(
                "POLICY-1",
                "청년 주거 정책",
                "정책 설명",
                "주거",
                "전월세 및 주거급여 지원",
                "청년,주거",
                "월세를 지원합니다.",
                "11680",
                "19",
                "34",
                "Y",
                "0013010",
                null,
                "0043001",
                null,
                null,
                "소득 무관",
                "0057001",
                "20260701~20260731",
                "온라인 신청",
                "주민등록등본",
                applicationUrl,
                "https://example.org/reference",
                "ftp://example.org/not-allowed",
                "주관 기관",
                "운영 기관",
                "20260730120000"
        );
    }

    private YouthPolicyItem withPeriod(YouthPolicyItem item, String periodCode, String periodText) {
        return copy(item, item.plcySprtCn(), periodCode, periodText);
    }

    private YouthPolicyItem withSupportText(YouthPolicyItem item, String supportText) {
        return copy(item, supportText, item.aplyPrdSeCd(), item.aplyYmd());
    }

    private YouthPolicyItem copy(
            YouthPolicyItem item,
            String supportText,
            String periodCode,
            String periodText
    ) {
        return new YouthPolicyItem(
                item.plcyNo(),
                item.plcyNm(),
                item.plcyExplnCn(),
                item.lclsfNm(),
                item.mclsfNm(),
                item.plcyKywdNm(),
                supportText,
                item.zipCd(),
                item.sprtTrgtMinAge(),
                item.sprtTrgtMaxAge(),
                item.sprtTrgtAgeLmtYn(),
                item.jobCd(),
                item.schoolCd(),
                item.earnCndSeCd(),
                item.earnMinAmt(),
                item.earnMaxAmt(),
                item.earnEtcCn(),
                periodCode,
                periodText,
                item.plcyAplyMthdCn(),
                item.sbmsnDcmntCn(),
                item.aplyUrlAddr(),
                item.refUrlAddr1(),
                item.refUrlAddr2(),
                item.sprvsnInstCdNm(),
                item.operInstCdNm(),
                item.lastMdfcnDt()
        );
    }
}
