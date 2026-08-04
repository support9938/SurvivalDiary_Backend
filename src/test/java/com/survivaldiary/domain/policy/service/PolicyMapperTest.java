package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.dto.PolicyEligibilityStatus;
import com.survivaldiary.domain.policy.dto.PolicyRecommendationStatus;
import org.junit.jupiter.api.Test;

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
}
