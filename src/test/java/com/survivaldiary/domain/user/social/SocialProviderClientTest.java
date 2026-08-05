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
                    "phone_number": "+82 10-1234-5678",
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
        assertThat(profile.phone()).isEqualTo("+82 10-1234-5678");
        assertThat(profile.name()).isEqualTo("Kakao User");
        assertThat(profile.gender()).isEqualTo(User.Gender.FEMALE);
        assertThat(profile.birthDate()).isEqualTo(LocalDate.of(1995, 2, 14));
    }

    @Test
    void fallsBackToKakaoPropertiesNickname() throws Exception {
        var body = objectMapper.readTree("""
                {
                  "id": 123456789,
                  "properties": {"nickname": "Kakao Properties User"},
                  "kakao_account": {}
                }
                """);

        SocialProfile profile = KakaoSocialProviderClient.parse(body);

        assertThat(profile.name()).isEqualTo("Kakao Properties User");
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
                    "mobile": "010-1234-5678",
                    "gender": "M",
                    "birthyear": "1990",
                    "birthday": "01-02",
                    "nickname": "Naver Nickname",
                    "name": "Naver User"
                  }
                }
                """);

        SocialProfile profile = NaverSocialProviderClient.parse(body);

        assertThat(profile.providerUserId()).isEqualTo("naver-user-id");
        assertThat(profile.email()).isEqualTo("naver@example.com");
        assertThat(profile.phone()).isEqualTo("010-1234-5678");
        assertThat(profile.name()).isEqualTo("Naver User");
        assertThat(profile.nickname()).isEqualTo("Naver Nickname");
        assertThat(profile.gender()).isEqualTo(User.Gender.MALE);
        assertThat(profile.birthDate()).isEqualTo(LocalDate.of(1990, 1, 2));
    }

    @Test
    void leavesOptionalNaverProfileFieldsEmptyWhenNotProvided() throws Exception {
        var body = objectMapper.readTree("""
                {
                  "resultcode": "00",
                  "response": {"id": "naver-user-id"}
                }
                """);

        SocialProfile profile = NaverSocialProviderClient.parse(body);

        assertThat(profile.nickname()).isNull();
        assertThat(profile.birthYear()).isNull();
        assertThat(profile.birthDate()).isNull();
    }
}
