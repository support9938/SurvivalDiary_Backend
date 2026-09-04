package com.survivaldiary.domain.policy.client;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.client.dto.YouthPolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicyApplicationPeriodType;
import com.survivaldiary.domain.policy.dto.PolicyDetail;
import com.survivaldiary.domain.policy.dto.PolicyOfficialLinkType;
import com.survivaldiary.domain.policy.service.PolicyMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 제공처 정책을 내부 품질 필드로 변환해 원문 보존과 필드 간 불변식을 확인한다.
 * 인증키, 정책 식별자, 정책 원문과 URL은 출력하지 않고 유형별 개수만 출력한다.
 */
@EnabledIfEnvironmentVariable(named = "YOUTH_POLICY_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "RUN_YOUTH_POLICY_LIVE_TEST", matches = "(?i)true")
class YouthPolicyDataQualityLiveContractTest {

    private static final int MAX_PAGES = 3;
    private static final int PAGE_SIZE = 20;

    @Test
    void 실제_정책의_품질_필드는_원문과_모순되지_않는다() {
        YouthPolicyProperties properties = new YouthPolicyProperties();
        properties.setApiKey(System.getenv("YOUTH_POLICY_API_KEY"));

        YouthPolicyClientConfig config = new YouthPolicyClientConfig();
        YouthPolicyClient client = new YouthPolicyClient(
                config.youthPolicyRestClient(properties),
                properties
        );
        YouthPolicyResponseParser parser = new YouthPolicyResponseParser(new ObjectMapper());
        PolicyMapper mapper = new PolicyMapper();

        List<YouthPolicyItem> items = fetchItems(client, parser);
        List<PolicyDetail> details = items.stream().map(mapper::toDetail).toList();

        assertThat(items).isNotEmpty();
        for (int index = 0; index < items.size(); index++) {
            YouthPolicyItem item = items.get(index);
            PolicyDetail detail = details.get(index);

            assertRawTextPreserved(item, detail);
            assertAmountContract(detail);
            assertPeriodContract(detail);
            assertLinkContract(detail);
        }

        System.out.printf(
                "온통청년 품질 계약 결과: policyCount=%d, amountTypes=%s, periodTypes=%s, linkTypes=%s%n",
                details.size(),
                summarizeAmountTypes(details),
                summarizePeriodTypes(details),
                summarizeLinkTypes(details)
        );
    }

    private List<YouthPolicyItem> fetchItems(
            YouthPolicyClient client,
            YouthPolicyResponseParser parser
    ) {
        Map<String, YouthPolicyItem> uniqueItems = new LinkedHashMap<>();
        for (int pageNumber = 1; pageNumber <= MAX_PAGES; pageNumber++) {
            var root = client.search(new YouthPolicySearchRequest(
                    pageNumber,
                    PAGE_SIZE,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
            List<YouthPolicyItem> pageItems = parser.parseItems(root);
            pageItems.forEach(item -> uniqueItems.putIfAbsent(item.plcyNo(), item));
            if (pageItems.size() < PAGE_SIZE) {
                break;
            }
        }
        return List.copyOf(uniqueItems.values());
    }

    private void assertRawTextPreserved(YouthPolicyItem item, PolicyDetail detail) {
        if (item.plcySprtCn() != null && !item.plcySprtCn().isBlank()) {
            assertThat(detail.supportText()).isEqualTo(item.plcySprtCn().trim());
        }
        if (item.aplyYmd() == null || item.aplyYmd().isBlank()) {
            assertThat(detail.applicationPeriodText()).isNull();
        } else {
            assertThat(detail.applicationPeriodText()).isEqualTo(item.aplyYmd().trim());
        }
    }

    private void assertAmountContract(PolicyDetail detail) {
        if (detail.supportAmount() == null) {
            assertThat(detail.supportAmountType()).isNull();
            return;
        }
        assertThat(detail.supportAmount()).isPositive();
        assertThat(detail.supportAmountType()).isNotNull();
    }

    private void assertPeriodContract(PolicyDetail detail) {
        if (detail.applicationPeriodType() == PolicyApplicationPeriodType.FIXED) {
            assertThat(detail.applicationEndDate()).isNotNull();
            if (detail.applicationStartDate() != null) {
                assertThat(detail.applicationEndDate()).isAfterOrEqualTo(detail.applicationStartDate());
            }
            return;
        }
        assertThat(detail.applicationStartDate()).isNull();
        assertThat(detail.applicationEndDate()).isNull();
    }

    private void assertLinkContract(PolicyDetail detail) {
        if (detail.officialUrl() == null) {
            assertThat(detail.officialLinkType()).isEqualTo(PolicyOfficialLinkType.UNAVAILABLE);
            return;
        }
        assertThat(detail.officialLinkType()).isNotEqualTo(PolicyOfficialLinkType.UNAVAILABLE);
    }

    private Map<String, Long> summarizeAmountTypes(List<PolicyDetail> details) {
        return details.stream().collect(Collectors.groupingBy(
                detail -> detail.supportAmountType() == null
                        ? "UNSTRUCTURED"
                        : detail.supportAmountType().name(),
                LinkedHashMap::new,
                Collectors.counting()
        ));
    }

    private Map<String, Long> summarizePeriodTypes(List<PolicyDetail> details) {
        return details.stream().collect(Collectors.groupingBy(
                detail -> detail.applicationPeriodType().name(),
                LinkedHashMap::new,
                Collectors.counting()
        ));
    }

    private Map<String, Long> summarizeLinkTypes(List<PolicyDetail> details) {
        return details.stream().collect(Collectors.groupingBy(
                detail -> detail.officialLinkType().name(),
                LinkedHashMap::new,
                Collectors.counting()
        ));
    }
}
