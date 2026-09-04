package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.YouthPolicyClient;
import com.survivaldiary.domain.policy.client.YouthPolicyResponseParser;
import com.survivaldiary.domain.policy.client.dto.YouthPolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicyApplicationPeriodType;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.argThat;

class PolicyDuplicateDetailTest {
    private final List<PolicyService> services = new ArrayList<>();

    @AfterEach
    void close() {
        services.forEach(PolicyService::closeSourceVersionVerifier);
    }

    @Test
    void 지역필터가_최신본을_누락해도_상시본을_전국_재조회하여_최신조건으로_판정한다() {
        var items = PolicyDuplicateResolverTest.mentoringItems();
        var client = mock(YouthPolicyClient.class);
        var json = new ObjectMapper();
        when(client.search(any(YouthPolicySearchRequest.class))).thenAnswer(invocation -> {
            YouthPolicySearchRequest sourceRequest = invocation.getArgument(0);
            return json.valueToTree(sourceRequest.zipCode() == null ? items : List.of(items.get(0)));
        });
        var matcher = mock(PolicyMatcher.class);
        var request = new PolicySearchRequest(29, "26", null, null, null, null,
                null, 1, 20, null, null, null, Set.of());
        when(matcher.match(items.get(1), request)).thenReturn(PolicyMatchResult.excluded());
        var service = new PolicyService(client, new YouthPolicyResponseParser(json),
                matcher, new PolicyMapper(), mock(PolicyRecommendationEvaluator.class));
        services.add(service);

        assertThat(service.recommend(request).items()).isEmpty();
        verify(matcher).match(items.get(1), request);
        verify(client).search(argThat(sourceRequest -> sourceRequest.zipCode() == null
                && items.get(0).plcyNm().equals(sourceRequest.policyName())));
    }

    @Test
    void 이전_상시_정책번호로_상세를_열어도_같은_신청공고의_최신_마감을_반환한다() {
        var items = PolicyDuplicateResolverTest.mentoringItems();
        var client = mock(YouthPolicyClient.class);
        var json = new ObjectMapper();
        when(client.findDetail(items.get(0).plcyNo())).thenReturn(json.valueToTree(items.get(0)));
        when(client.search(any(YouthPolicySearchRequest.class))).thenReturn(json.valueToTree(items));

        var detail = service(client, json).findDetail(items.get(0).plcyNo());

        assertThat(detail.policyId()).isEqualTo(items.get(1).plcyNo());
        assertThat(detail.applicationPeriodType()).isEqualTo(PolicyApplicationPeriodType.CLOSED);
    }

    @Test
    void 추가_중복_조회_실패가_이미_성공한_상세를_막지는_않는다() {
        var item = PolicyDuplicateResolverTest.mentoringItems().get(0);
        var client = mock(YouthPolicyClient.class);
        var json = new ObjectMapper();
        when(client.findDetail(item.plcyNo())).thenReturn(json.valueToTree(item));
        when(client.search(any(YouthPolicySearchRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.POLICY_PROVIDER_UNAVAILABLE));

        var detail = service(client, json).findDetail(item.plcyNo());

        assertThat(detail.policyId()).isEqualTo(item.plcyNo());
        assertThat(detail.applicationPeriodType()).isEqualTo(PolicyApplicationPeriodType.ALWAYS);
    }

    private PolicyService service(YouthPolicyClient client, ObjectMapper json) {
        var service = new PolicyService(client, new YouthPolicyResponseParser(json),
                mock(PolicyMatcher.class), new PolicyMapper(), mock(PolicyRecommendationEvaluator.class));
        services.add(service);
        return service;
    }
}
