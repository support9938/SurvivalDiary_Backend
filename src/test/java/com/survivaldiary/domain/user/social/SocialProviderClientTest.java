package com.survivaldiary.domain.user.social;

import static org.assertj.core.api.Assertions.assertThat;

import com.survivaldiary.domain.user.entity.User;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SocialProviderClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesKakaoVerifiedProfile() throws Exception {
        var body = objectMapper.readTree("""
                {
                  "id": 123456789,
                  "kakao_account": {
                    "email": "kakao@example.com",
                    "gender": "female",
                    "birthyear": "1995",
                    "birthday": "0214",
                    "profile": {"nickname": "Kakao User"}
                  }
                }
                """);

        SocialProfile profile = KakaoSocialProviderClient.parse(body);

        assertThat(profile.providerUserId()).isEqualTo("123456789");
        assertThat(profile.email()).isEqualTo("kakao@example.com");
        assertThat(profile.name()).isEqualTo("Kakao User");
        assertThat(profile.gender()).isEqualTo(User.Gender.FEMALE);
        assertThat(profile.birthDate()).isEqualTo(LocalDate.of(1995, 2, 14));
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
                    "gender": "M",
                    "birthyear": "1990",
                    "birthday": "01-02",
                    "name": "Naver User"
                  }
                }
                """);

        SocialProfile profile = NaverSocialProviderClient.parse(body);

        assertThat(profile.providerUserId()).isEqualTo("naver-user-id");
        assertThat(profile.email()).isEqualTo("naver@example.com");
        assertThat(profile.name()).isEqualTo("Naver User");
        assertThat(profile.gender()).isEqualTo(User.Gender.MALE);
        assertThat(profile.birthDate()).isEqualTo(LocalDate.of(1990, 1, 2));
    }
}
