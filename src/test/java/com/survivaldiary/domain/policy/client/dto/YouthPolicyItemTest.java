package com.survivaldiary.domain.policy.client.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class YouthPolicyItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 공식_필드_fixture를_외부_DTO로_변환한다() throws Exception {
        try (InputStream fixture = getClass()
                .getResourceAsStream("/fixtures/policy/policy-item.json")) {
            assertNotNull(fixture);

            YouthPolicyItem item = objectMapper.readValue(fixture, YouthPolicyItem.class);

            assertThat(item.plcyNo()).isEqualTo("R202607310001");
            assertThat(item.plcyNm()).isEqualTo("청년 주거 지원");
            assertThat(item.lclsfNm()).isEqualTo("주거");
            assertThat(item.sprtTrgtMinAge()).isEqualTo("19");
            assertThat(item.aplyUrlAddr()).isEqualTo("https://example.org/apply");
        }
    }
}
