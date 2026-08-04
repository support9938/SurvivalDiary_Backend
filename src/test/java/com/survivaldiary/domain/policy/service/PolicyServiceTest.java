package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.YouthPolicyClient;
import com.survivaldiary.domain.policy.client.YouthPolicyResponseParser;
import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.client.dto.YouthPolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicyCategory;
import com.survivaldiary.domain.policy.dto.PolicyDetail;
import com.survivaldiary.domain.policy.dto.PolicyEligibilityStatus;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicySummary;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.stream.IntStream;

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
    private PolicyService service;

    @BeforeEach
    void setUp() {
        client = mock(YouthPolicyClient.class);
        parser = mock(YouthPolicyResponseParser.class);
        matcher = mock(PolicyMatcher.class);
        mapper = mock(PolicyMapper.class);
        service = new PolicyService(client, parser, matcher, mapper);
    }

    @Test
    void 결과가_부족하면_제공처를_최대_3페이지까지만_확인한다() {
        JsonNode root = mock(JsonNode.class);
        List<YouthPolicyItem> fullPage = IntStream.range(0, 20)
                .mapToObj(index -> item("POLICY-" + index))
                .toList();
        when(client.search(any(YouthPolicySearchRequest.class))).thenReturn(root);
        when(parser.parseItems(root)).thenReturn(fullPage);
        when(matcher.match(any(YouthPolicyItem.class), any(PolicySearchRequest.class)))
                .thenReturn(PolicyMatchResult.excluded());

        var response = service.search(request(20, "11680"));

        assertThat(response.items()).isEmpty();
        assertThat(response.checkedProviderPages()).isEqualTo(3);
        assertThat(response.partialResult()).isTrue();
        verify(client, times(3)).search(any(YouthPolicySearchRequest.class));
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
        when(mapper.toSummary(eq(item), any(PolicyMatchResult.class))).thenReturn(summary);

        var response = service.search(request(20, "11680"));

        assertThat(response.items()).containsExactly(summary);
        assertThat(response.checkedProviderPages()).isEqualTo(1);
        assertThat(response.partialResult()).isFalse();
        verify(client, times(1)).search(any(YouthPolicySearchRequest.class));
    }

    @Test
    void 요청한_개수보다_일치_결과가_많으면_부분_결과로_표시한다() {
        JsonNode root = mock(JsonNode.class);
        List<YouthPolicyItem> providerItems = List.of(item("POLICY-1"), item("POLICY-2"));
        when(client.search(any(YouthPolicySearchRequest.class))).thenReturn(root);
        when(parser.parseItems(root)).thenReturn(providerItems);
        when(matcher.match(any(YouthPolicyItem.class), any(PolicySearchRequest.class)))
                .thenReturn(PolicyMatchResult.matched());
        when(mapper.toSummary(any(YouthPolicyItem.class), any(PolicyMatchResult.class)))
                .thenAnswer(invocation -> summary(
                        invocation.<YouthPolicyItem>getArgument(0).plcyNo()
                ));

        var response = service.search(request(1, "11680"));

        assertThat(response.items()).hasSize(1);
        assertThat(response.partialResult()).isTrue();
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
        return new PolicySearchRequest(
                27,
                "11",
                districtCode,
                "JOB_SEEKING",
                "NO_LIMIT",
                "HOUSING",
                size
        );
    }

    private PolicySummary summary(String policyId) {
        return new PolicySummary(
                policyId,
                "주거",
                PolicyCategory.HOUSING,
                "정책명",
                "요약",
                null,
                "지원 내용",
                null,
                "지원 대상",
                "주관 기관",
                PolicyEligibilityStatus.MATCHED,
                List.of()
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
