package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.dto.PolicyEligibilityStatus;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyMatcherTest {

    private final PolicyMatcher matcher = new PolicyMatcher();

    @Test
    void 구조화_조건이_모두_일치하면_MATCHED로_판정한다() {
        PolicyMatchResult result = matcher.match(
                item("19", "34", "Y", "11680", "0013003", "0043001", "주거"),
                request("NO_LIMIT")
        );

        assertThat(result.included()).isTrue();
        assertThat(result.status()).isEqualTo(PolicyEligibilityStatus.MATCHED);
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void 중위소득으로_직접_변환할_수_없으면_CHECK_REQUIRED로_포함한다() {
        PolicyMatchResult result = matcher.match(
                item("19", "34", "Y", "11680", "0013003", "0043003", "주거"),
                request("BELOW_100")
        );

        assertThat(result.included()).isTrue();
        assertThat(result.status()).isEqualTo(PolicyEligibilityStatus.CHECK_REQUIRED);
        assertThat(result.reasons()).contains("중위소득 조건을 공고문에서 확인해야 합니다.");
    }

    @Test
    void 연령이_명확하게_벗어나면_결과에서_제외한다() {
        PolicyMatchResult result = matcher.match(
                item("30", "34", "Y", "11680", "0013003", "0043001", "주거"),
                request("NO_LIMIT")
        );

        assertThat(result.included()).isFalse();
    }

    @Test
    void 같은_시도라도_다른_시군구_전용_정책은_제외한다() {
        PolicyMatchResult result = matcher.match(
                item("19", "34", "Y", "11710", "0013003", "0043001", "주거"),
                request("NO_LIMIT")
        );

        assertThat(result.included()).isFalse();
    }

    @Test
    void 시도_공통_코드는_선택한_시군구에도_포함한다() {
        PolicyMatchResult result = matcher.match(
                item("19", "34", "Y", "11000", "0013003", "0043001", "주거"),
                request("NO_LIMIT")
        );

        assertThat(result.included()).isTrue();
        assertThat(result.status()).isEqualTo(PolicyEligibilityStatus.MATCHED);
    }

    private PolicySearchRequest request(String incomeRange) {
        return new PolicySearchRequest(
                27,
                "11",
                "11680",
                "JOB_SEEKING",
                incomeRange,
                "HOUSING",
                20
        );
    }

    private YouthPolicyItem item(
            String minAge,
            String maxAge,
            String ageLimit,
            String zipCode,
            String jobCode,
            String incomeCode,
            String largeCategory
    ) {
        return new YouthPolicyItem(
                "POLICY-1",
                "청년 주거 정책",
                "정책 설명",
                largeCategory,
                "전월세 및 주거급여 지원",
                "청년,주거",
                "월세를 지원합니다.",
                zipCode,
                minAge,
                maxAge,
                ageLimit,
                jobCode,
                null,
                incomeCode,
                null,
                null,
                null,
                "0057001",
                "20260701~20260731",
                "온라인 신청",
                "주민등록등본",
                "https://example.org/apply",
                "https://example.org/reference",
                null,
                "주관 기관",
                "운영 기관",
                "20260730120000"
        );
    }
}
