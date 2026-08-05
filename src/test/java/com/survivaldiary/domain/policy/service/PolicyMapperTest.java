package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.dto.PolicyEligibilityStatus;
import com.survivaldiary.domain.policy.dto.PolicyRecommendationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyMapperTest {

    private final PolicyMapper mapper = new PolicyMapper();

    @Test
    void 목록에는_지원금_null과_조건_확인_상태를_안전하게_반환한다() {
        var summary = mapper.toSummary(
                item("not-a-url"),
                PolicyMatchResult.checkRequired(List.of("소득 조건 확인 필요")),
                new PolicyRecommendationResult(
                        PolicyRecommendationStatus.CHECK_REQUIRED,
                        List.of("소득 조건 확인 필요"),
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
        assertThat(summary.applicationEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    void 특정_기간의_정확한_날짜_범위만_신청_종료일로_변환한다() {
        YouthPolicyItem source = item("https://example.org/apply");

        var fixedPeriod = mapper.toDetail(source);
        var alwaysOpen = mapper.toDetail(withPeriod(source, "0057002", "상시"));
        var invalidRange = mapper.toDetail(withPeriod(source, "0057001", "20260731~20260701"));
        var explanatoryText = mapper.toDetail(
                withPeriod(source, "0057001", "20260701~20260731 예정")
        );

        assertThat(fixedPeriod.applicationEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(alwaysOpen.applicationEndDate()).isNull();
        assertThat(invalidRange.applicationEndDate()).isNull();
        assertThat(explanatoryText.applicationEndDate()).isNull();
    }

    @Test
    void 상세는_유효한_HTTP_URL만_외부_링크로_반환한다() {
        var detail = mapper.toDetail(item("javascript:alert(1)"));

        assertThat(detail.officialUrl()).isNull();
        assertThat(detail.referenceUrls())
                .containsExactly("https://example.org/reference");
        assertThat(detail.documents()).containsExactly("주민등록등본");
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
        return new YouthPolicyItem(
                item.plcyNo(),
                item.plcyNm(),
                item.plcyExplnCn(),
                item.lclsfNm(),
                item.mclsfNm(),
                item.plcyKywdNm(),
                item.plcySprtCn(),
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
