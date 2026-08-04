package com.survivaldiary.domain.policy.client;

import com.survivaldiary.domain.policy.client.dto.YouthPolicySearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 서버 담당자가 명시적으로 요청할 때만 실행하는 실제 제공처 계약 테스트.
 * 인증키와 정책 원문 값은 출력하지 않고 응답 구조와 정책 개수만 확인한다.
 */
@EnabledIfEnvironmentVariable(named = "YOUTH_POLICY_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "RUN_YOUTH_POLICY_LIVE_TEST", matches = "(?i)true")
class YouthPolicyLiveContractTest {

    @Test
    void 실제_성공_응답의_구조와_정책_식별자를_확인한다() {
        YouthPolicyProperties properties = new YouthPolicyProperties();
        properties.setApiKey(System.getenv("YOUTH_POLICY_API_KEY"));

        YouthPolicyClientConfig config = new YouthPolicyClientConfig();
        YouthPolicyClient client = new YouthPolicyClient(
                config.youthPolicyRestClient(properties),
                properties
        );
        YouthPolicyResponseParser parser =
                new YouthPolicyResponseParser(new ObjectMapper());

        JsonNode root = client.search(new YouthPolicySearchRequest(
                1,
                20,
                null,
                null,
                null,
                null,
                null
        ));
        var items = parser.parseItems(root);
        List<String> topLevelFields = root.isObject()
                ? root.properties().stream().map(entry -> entry.getKey()).sorted().toList()
                : List.of();

        System.out.printf(
                "온통청년 성공 응답 구조: rootType=%s, topLevelFields=%s, policyCount=%d%n",
                root.getNodeType(),
                topLevelFields,
                items.size()
        );

        assertThat(items).isNotEmpty();
        assertThat(items).allMatch(item ->
                item.plcyNo() != null && !item.plcyNo().isBlank()
        );
    }
}
