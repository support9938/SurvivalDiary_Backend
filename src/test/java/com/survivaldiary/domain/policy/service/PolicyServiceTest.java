package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.YouthPolicyClient;
import com.survivaldiary.domain.policy.client.YouthPolicyResponseParser;
import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.client.dto.YouthPolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicyCategory;
import com.survivaldiary.domain.policy.dto.PolicyApplicationPeriodType;
import com.survivaldiary.domain.policy.dto.PolicyDetail;
import com.survivaldiary.domain.policy.dto.PolicyEligibilityStatus;
import com.survivaldiary.domain.policy.dto.PolicyRecommendationStatus;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicySearchResponse;
import com.survivaldiary.domain.policy.dto.PolicySummary;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyServiceTest {

    private YouthPolicyClient client;
    private YouthPolicyResponseParser parser;
    private PolicyMatcher matcher;
    private PolicyMapper mapper;
    private PolicyRecommendationEvaluator recommendationEvaluator;
    private PolicyService service;

    @BeforeEach
    void setUp() {
        client = mock(YouthPolicyClient.class);
        parser = mock(YouthPolicyResponseParser.class);
        matcher = mock(PolicyMatcher.class);
        mapper = mock(PolicyMapper.class);
        recommendationEvaluator = mock(PolicyRecommendationEvaluator.class);
        service = new PolicyService(
                client,
                parser,
                matcher,
                mapper,
                recommendationEvaluator
        );
    }

    @Test
    void 외부_페이지가_가득_차면_다음_페이지를_반환한다() {
        JsonNode root = mock(JsonNode.class);
        List<YouthPolicyItem> fullPage = java.util.stream.IntStream.range(0, 20)
                .mapToObj(index -> item("POLICY-" + index)).toList();
        when(client.search(any(YouthPolicySearchRequest.class))).thenReturn(root);
        when(parser.parseItems(root)).thenReturn(fullPage);
        when(matcher.match(any(YouthPolicyItem.class), any(PolicySearchRequest.class)))
                .thenReturn(PolicyMatchResult.excluded());

        var response = service.search(request(20, "11680"));

        assertThat(response.items()).isEmpty();
        assertThat(response.checkedProviderPages()).isEqualTo(1);
        assertThat(response.partialResult()).isTrue();
        assertThat(response.nextPage()).isEqualTo(2);
        verify(client).search(any(YouthPolicySearchRequest.class));
    }

    @Test
    void 마지막_외부_페이지가_짧으면_전체_후보를_확인한_것으로_처리한다() {
        JsonNode root = mock(JsonNode.class);
        YouthPolicyItem item = item("POLICY-1");
        PolicySummary summary = summary("POLICY-1");
        when(client.search(any(YouthPolicySearchRequest.class))).thenReturn(root);
        when(parser.parseItems(root)).thenReturn(List.of(item));
        when(matcher.match(item, request(20, "11680")))
                .thenReturn(PolicyMatchResult.matched());
        when(recommendationEvaluator.evaluate(
                eq(item),
                any(PolicySearchRequest.class),
                any(PolicyMatchResult.class)
        )).thenReturn(recommended(300));
        when(mapper.toSummary(
                eq(item),
                any(PolicyMatchResult.class),
                any(PolicyRecommendationResult.class)
        )).thenReturn(summary);

        var response = service.search(request(20, "11680"));

        assertThat(response.items()).containsExactly(summary);
        assertThat(response.checkedProviderPages()).isEqualTo(1);
        assertThat(response.partialResult()).isFalse();
        assertThat(response.nextPage()).isNull();
        verify(client).search(any(YouthPolicySearchRequest.class));
    }

    @Test
    void 관심_없음_정책은_조건_판정과_정렬_전에_제외한다() {
        JsonNode root = mock(JsonNode.class);
        YouthPolicyItem hidden = item("HIDDEN-1");
        when(client.search(any(YouthPolicySearchRequest.class))).thenReturn(root);
        when(parser.parseItems(root)).thenReturn(List.of(hidden));

        PolicySearchResponse response = service.recommend(
                defaultRequest(),
                Set.of("HIDDEN-1")
        );

        assertThat(response.items()).isEmpty();
        verify(matcher, never()).match(eq(hidden), any(PolicySearchRequest.class));
    }

    @Test
    void 요청한_페이지와_정책명_검색어를_제공처에_전달한다() {
        JsonNode root = mock(JsonNode.class);
        when(client.search(any(YouthPolicySearchRequest.class))).thenReturn(root);
        when(parser.parseItems(root)).thenReturn(List.of());

        service.search(request(20, "11680", "  월세  ", 4));

        verify(client).search(argThat(providerRequest ->
                providerRequest.pageNumber() == 4
                        && providerRequest.pageSize() == 20
                        && "월세".equals(providerRequest.policyName())
        ));
    }

    @Test
    void 시도와_시군구_코드가_불일치하면_외부_호출_전에_거절한다() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.search(request(20, "26110"))
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_POLICY_FILTER);
        verify(client, never()).search(any(YouthPolicySearchRequest.class));
    }

    @Test
    void 시군구가_없으면_시도_전체_코드로_제공처를_조회한다() {
        JsonNode root = mock(JsonNode.class);
        when(client.search(any(YouthPolicySearchRequest.class))).thenReturn(root);
        when(parser.parseItems(root)).thenReturn(List.of());

        service.search(request(20, null));

        verify(client).search(argThat(providerRequest ->
                "11000".equals(providerRequest.zipCode())
        ));
    }

    @Test
    void 시군구가_있으면_선택한_코드로_제공처를_조회한다() {
        JsonNode root = mock(JsonNode.class);
        when(client.search(any(YouthPolicySearchRequest.class))).thenReturn(root);
        when(parser.parseItems(root)).thenReturn(List.of());

        service.search(request(20, "11680"));

        verify(client).search(argThat(providerRequest ->
                "11680".equals(providerRequest.zipCode())
        ));
    }

    @Test
    void 추천_우선순위가_높은_정책을_먼저_반환한다() {
        JsonNode root = mock(JsonNode.class);
        YouthPolicyItem discoverItem = item("POLICY-DISCOVER");
        YouthPolicyItem recommendedItem = item("POLICY-RECOMMENDED");
        PolicySummary discoverSummary = summary("POLICY-DISCOVER");
        PolicySummary recommendedSummary = summary("POLICY-RECOMMENDED");
        PolicyMatchResult matchResult = PolicyMatchResult.matched();

        when(client.search(any(YouthPolicySearchRequest.class))).thenReturn(root);
        when(parser.parseItems(root)).thenReturn(List.of(discoverItem, recommendedItem));
        when(matcher.match(any(YouthPolicyItem.class), any(PolicySearchRequest.class)))
                .thenReturn(matchResult);
        when(recommendationEvaluator.evaluate(discoverItem, request(20, "11680"), matchResult))
                .thenReturn(new PolicyRecommendationResult(
                        PolicyRecommendationStatus.DISCOVER,
                        List.of("함께 보기"),
                        100
                ));
        when(recommendationEvaluator.evaluate(
                recommendedItem,
                request(20, "11680"),
                matchResult
        )).thenReturn(recommended(300));
        when(mapper.toSummary(
                eq(discoverItem),
                eq(matchResult),
                any(PolicyRecommendationResult.class)
        )).thenReturn(discoverSummary);
        when(mapper.toSummary(
                eq(recommendedItem),
                eq(matchResult),
                any(PolicyRecommendationResult.class)
        )).thenReturn(recommendedSummary);

        var response = service.search(request(20, "11680"));

        assertThat(response.items())
                .containsExactly(recommendedSummary, discoverSummary);
    }

    @Test
    void 기본_추천은_세_페이지_후보를_비교해_뒤쪽의_맞춤_정책을_먼저_반환한다() {
        JsonNode firstRoot = mock(JsonNode.class);
        JsonNode secondRoot = mock(JsonNode.class);
        JsonNode thirdRoot = mock(JsonNode.class);
        YouthPolicyItem discoverItem = item("POLICY-DISCOVER");
        YouthPolicyItem recommendedItem = item("POLICY-RECOMMENDED");
        List<YouthPolicyItem> firstPage = java.util.stream.IntStream.range(0, 20)
                .mapToObj(index -> index == 0 ? discoverItem : item("FIRST-" + index))
                .toList();
        List<YouthPolicyItem> secondPage = java.util.stream.IntStream.range(0, 20)
                .mapToObj(index -> item("SECOND-" + index))
                .toList();
        PolicyMatchResult matchResult = PolicyMatchResult.matched();
        PolicySummary discoverSummary = summary("POLICY-DISCOVER");
        PolicySummary recommendedSummary = summary("POLICY-RECOMMENDED");

        when(client.search(argThat(request -> request != null && request.pageNumber() == 1)))
                .thenReturn(firstRoot);
        when(client.search(argThat(request -> request != null && request.pageNumber() == 2)))
                .thenReturn(secondRoot);
        when(client.search(argThat(request -> request != null && request.pageNumber() == 3)))
                .thenReturn(thirdRoot);
        when(parser.parseItems(firstRoot)).thenReturn(firstPage);
        when(parser.parseItems(secondRoot)).thenReturn(secondPage);
        when(parser.parseItems(thirdRoot)).thenReturn(List.of(recommendedItem));
        when(matcher.match(any(YouthPolicyItem.class), any(PolicySearchRequest.class)))
                .thenReturn(PolicyMatchResult.excluded());
        when(matcher.match(eq(discoverItem), any(PolicySearchRequest.class)))
                .thenReturn(matchResult);
        when(matcher.match(eq(recommendedItem), any(PolicySearchRequest.class)))
                .thenReturn(matchResult);
        when(recommendationEvaluator.evaluate(
                eq(discoverItem),
                any(PolicySearchRequest.class),
                eq(matchResult)
        )).thenReturn(new PolicyRecommendationResult(
                PolicyRecommendationStatus.DISCOVER,
                List.of("함께 보기"),
                100
        ));
        when(recommendationEvaluator.evaluate(
                eq(recommendedItem),
                any(PolicySearchRequest.class),
                eq(matchResult)
        )).thenReturn(recommended(1_100));
        when(mapper.toSummary(
                eq(discoverItem),
                eq(matchResult),
                any(PolicyRecommendationResult.class)
        )).thenReturn(discoverSummary);
        when(mapper.toSummary(
                eq(recommendedItem),
                eq(matchResult),
                any(PolicyRecommendationResult.class)
        )).thenReturn(recommendedSummary);

        PolicySearchResponse response = service.recommend(defaultRequest());

        assertThat(response.items()).containsExactly(recommendedSummary, discoverSummary);
        assertThat(response.checkedProviderPages()).isEqualTo(3);
        assertThat(response.partialResult()).isFalse();
        assertThat(response.nextPage()).isNull();
        verify(client, times(3)).search(any(YouthPolicySearchRequest.class));
    }

    @Test
    void 상세_응답을_파싱해_내부_DTO로_변환한다() {
        JsonNode root = mock(JsonNode.class);
        YouthPolicyItem item = item("POLICY-DETAIL");
        PolicyDetail detail = mock(PolicyDetail.class);
        when(client.findDetail("POLICY-DETAIL")).thenReturn(root);
        when(parser.parseDetail(root, "POLICY-DETAIL")).thenReturn(item);
        when(mapper.toDetail(item)).thenReturn(detail);

        assertThat(service.findDetail("POLICY-DETAIL")).isSameAs(detail);
    }

    private PolicySearchRequest request(int size, String districtCode) {
        return request(size, districtCode, null, 1);
    }

    private PolicySearchRequest request(
            int size,
            String districtCode,
            String keyword,
            int page
    ) {
        return new PolicySearchRequest(
                27,
                "11",
                districtCode,
                "JOB_SEEKING",
                "NO_LIMIT",
                "HOUSING",
                keyword,
                page,
                size,
                null,
                null,
                null,
                Set.of("HOUSING")
        );
    }

    private PolicySummary summary(String policyId) {
        return new PolicySummary(
                policyId,
                "주거",
                PolicyCategory.HOUSING,
                "정책명",
                "요약",
                "청년의 안정적인 주거 생활을 지원해요",
                null,
                null,
                "지원 내용",
                null,
                PolicyApplicationPeriodType.UNKNOWN,
                null,
                null,
                "지원 대상",
                "주관 기관",
                PolicyEligibilityStatus.MATCHED,
                List.of(),
                PolicyRecommendationStatus.RECOMMENDED,
                List.of("관심 주제인 주거 분야와 관련된 정책이에요."),
                List.of()
        );
    }

    private PolicySearchRequest defaultRequest() {
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
                true,
                null,
                Set.of("EMPLOYMENT", "HOUSING")
        );
    }

    private PolicyRecommendationResult recommended(int priority) {
        return new PolicyRecommendationResult(
                PolicyRecommendationStatus.RECOMMENDED,
                List.of("맞춤 추천 이유"),
                priority
        );
    }

    private YouthPolicyItem item(String policyId) {
        return new YouthPolicyItem(
                policyId,
                "정책명",
                "설명",
                "주거",
                "전월세 및 주거급여 지원",
                "청년,주거",
                "지원 내용",
                "11680",
                "19",
                "34",
                "Y",
                "0013003",
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
