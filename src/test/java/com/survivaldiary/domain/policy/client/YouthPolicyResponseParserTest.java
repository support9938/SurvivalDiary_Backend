package com.survivaldiary.domain.policy.client;

import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YouthPolicyResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final YouthPolicyResponseParser parser =
            new YouthPolicyResponseParser(objectMapper);

    @Test
    void 래퍼_위치와_상관없이_정책_객체를_찾는다() throws Exception {
        try (InputStream fixture = getClass()
                .getResourceAsStream("/fixtures/policy/policy-list-response.json")) {
            assertNotNull(fixture);
            JsonNode root = objectMapper.readTree(fixture);

            var items = parser.parseItems(root);

            assertThat(items).hasSize(2);
            assertThat(items.get(0).plcyNo()).isEqualTo("R202607310001");
            assertThat(items.get(1).plcyNo()).isEqualTo("R202607310002");
        }
    }

    @Test
    void 상세_응답에서_요청한_문자열_ID를_찾는다() throws Exception {
        JsonNode root = objectMapper.readTree(
                "{\"data\":{\"item\":{\"plcyNo\":\"POLICY-A\",\"plcyNm\":\"정책 A\"}}}"
        );

        var item = parser.parseDetail(root, "POLICY-A");

        assertThat(item.plcyNm()).isEqualTo("정책 A");
    }

    @Test
    void 성공_HTTP에_포함된_제공처_오류도_안전하게_거절한다() throws Exception {
        JsonNode root = objectMapper.readTree(
                "{\"errorCode\":\"e001\",\"errorMsg\":\"invalid api key.\"}"
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> parser.parseItems(root)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.POLICY_PROVIDER_BAD_RESPONSE);
        assertThat(exception.getMessage()).doesNotContain("invalid api key");
    }

    @Test
    void 상세_정책이_없으면_정책_없음으로_변환한다() throws Exception {
        JsonNode root = objectMapper.readTree("{\"data\":{\"items\":[]}}");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> parser.parseDetail(root, "missing")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_NOT_FOUND);
    }
}
