package com.survivaldiary.domain.user.social;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SocialProviderClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesKakaoVerifiedProfile() throws Exception {
        var body = objectMapper.readTree("""
                {
                  "id": 123456789,
                  "kakao_account": {
                    "email": "kakao@example.com",
                    "profile": {"nickname": "카카오 사용자"}
                  }
                }
                """);

        SocialProfile profile = KakaoSocialProviderClient.parse(body);

        assertThat(profile.providerUserId()).isEqualTo("123456789");
        assertThat(profile.email()).isEqualTo("kakao@example.com");
        assertThat(profile.name()).isEqualTo("카카오 사용자");
    }

    @Test
    void parsesNaverVerifiedProfile() throws Exception {
        var body = objectMapper.readTree("""
                {
                  "resultcode": "00",
                  "message": "success",
                  "response": {
                    "id": "naver-user-id",
                    "email": "naver@example.com",
                    "name": "네이버 사용자"
                  }
                }
                """);

        SocialProfile profile = NaverSocialProviderClient.parse(body);

        assertThat(profile.providerUserId()).isEqualTo("naver-user-id");
        assertThat(profile.email()).isEqualTo("naver@example.com");
        assertThat(profile.name()).isEqualTo("네이버 사용자");
    }
}
