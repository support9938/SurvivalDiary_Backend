package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.dto.PolicyRecommendationStatus;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyRecommendationEvaluatorTest {

    private final PolicyRecommendationEvaluator evaluator =
            new PolicyRecommendationEvaluator();

    @Test
    void 관심_주제와_정책_분야가_일치하면_추천으로_판정한다() {
        PolicyRecommendationResult result = evaluator.evaluate(
                item("주거", "청년 월세 지원", "청년,주거", "11680"),
                request(Set.of("HOUSING"), null, null),
                PolicyMatchResult.matched()
        );

        assertThat(result.status()).isEqualTo(PolicyRecommendationStatus.RECOMMENDED);
        assertThat(result.reasons())
                .contains("관심 주제인 주거 분야와 관련된 정책이에요.")
                .contains("선택한 시·군·구 거주 조건과 일치해요.");
    }

    @Test
    void 자산_형성_관심사는_정책_텍스트로_관련성을_판정한다() {
        PolicyRecommendationResult result = evaluator.evaluate(
                item("복지문화", "청년 자산 형성 통장", "저축,목돈", "전국"),
                request(Set.of("ASSET_BUILDING"), null, null),
                PolicyMatchResult.matched()
        );

        assertThat(result.status()).isEqualTo(PolicyRecommendationStatus.RECOMMENDED);
        assertThat(result.reasons())
                .contains("관심 주제인 자산 형성 분야와 관련된 정책이에요.");
    }

    @Test
    void 확인할_자격_조건이_있으면_관심_분야여도_확인_필요가_우선한다() {
        PolicyRecommendationResult result = evaluator.evaluate(
                item("교육", "청년 교육 지원", "대학생,교육", "11680"),
                request(Set.of("EDUCATION"), null, "STUDENT"),
                PolicyMatchResult.checkRequired(List.of("재학 조건을 확인해야 합니다."))
        );

        assertThat(result.status()).isEqualTo(PolicyRecommendationStatus.CHECK_REQUIRED);
        assertThat(result.reasons().get(0)).isEqualTo("재학 조건을 확인해야 합니다.");
    }

    @Test
    void 추가_추천_신호가_없으면_함께_보기로_분류한다() {
        PolicyRecommendationResult result = evaluator.evaluate(
                item("참여권리", "청년 참여 위원 모집", "참여", "11680"),
                request(Set.of(), null, null),
                PolicyMatchResult.matched()
        );

        assertThat(result.status()).isEqualTo(PolicyRecommendationStatus.DISCOVER);
        assertThat(result.reasons()).isNotEmpty();
    }

    @Test
    void 구직_중인_사용자의_일자리_정책은_추천으로_판정한다() {
        PolicyRecommendationResult result = evaluator.evaluate(
                item("일자리", "청년 취업 지원", "구직,채용", "11680"),
                request(Set.of(), true, null),
                PolicyMatchResult.matched()
        );

        assertThat(result.status()).isEqualTo(PolicyRecommendationStatus.RECOMMENDED);
        assertThat(result.reasons())
                .contains("구직 중인 사용자에게 관련된 일자리 정책이에요.");
    }

    private PolicySearchRequest request(
            Set<String> interests,
            Boolean jobSeeking,
            String educationStatus
    ) {
        return new PolicySearchRequest(
                27,
                "11",
                "11680",
                null,
                null,
                null,
                null,
                1,
                20,
                "UNEMPLOYED",
                jobSeeking,
                educationStatus,
                interests
        );
    }

    private YouthPolicyItem item(
            String largeCategory,
            String title,
            String keywords,
            String zipCode
    ) {
        return new YouthPolicyItem(
                "POLICY-1",
                title,
                "정책 설명",
                largeCategory,
                "정책 중분류",
                keywords,
                "지원 내용",
                zipCode,
                "19",
                "34",
                "Y",
                "0013010",
                null,
                "0043001",
                null,
                null,
                null,
                "0057001",
                null,
                null,
                null,
                null,
                null,
                null,
                "주관 기관",
                "운영 기관",
                null
        );
    }
}
