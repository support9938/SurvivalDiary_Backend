package com.survivaldiary.domain.user.social;

import tools.jackson.databind.JsonNode;
import com.survivaldiary.domain.user.entity.SocialAccount;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.time.DateTimeException;
import java.time.LocalDate;

@Component
public class KakaoSocialProviderClient implements SocialProviderClient {

    private final RestClient restClient;

    public KakaoSocialProviderClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build();
    }

    KakaoSocialProviderClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public SocialAccount.Provider provider() {
        return SocialAccount.Provider.KAKAO;
    }

    @Override
    public SocialProfile verify(String accessToken) {
        try {
            JsonNode body = restClient.get()
                    .uri("/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            return parse(body);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.SOCIAL_AUTH_FAILED);
        }
    }

    static SocialProfile parse(JsonNode body) {
        if (body == null || body.path("id").isMissingNode()) {
            throw new IllegalArgumentException("Kakao user id is missing");
        }
        JsonNode account = body.path("kakao_account");
        String email = nullableText(account.path("email"));
        String nickname = nullableText(account.path("profile").path("nickname"));
        if (nickname == null) nickname = nullableText(body.path("properties").path("nickname"));
        String name = nullableText(account.path("name"));
        if (name == null) name = nickname;
        String genderValue = nullableText(account.path("gender"));
        User.Gender gender = "male".equals(genderValue)
                ? User.Gender.MALE
                : "female".equals(genderValue) ? User.Gender.FEMALE : null;
        Integer birthYear = parseBirthYear(nullableText(account.path("birthyear")));
        return new SocialProfile(body.path("id").asText(), email, name, nickname,
                nullableText(account.path("phone_number")), gender,
                birthYear,
                parseBirthDate(birthYear,
                        nullableText(account.path("birthday"))));
    }

    private static Integer parseBirthYear(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static LocalDate parseBirthDate(Integer birthYear, String birthday) {
        if (birthYear == null || birthday == null) return null;
        String normalized = birthday.replace("-", "");
        if (normalized.length() != 4) return null;
        try {
            return LocalDate.of(birthYear, Integer.parseInt(normalized.substring(0, 2)),
                    Integer.parseInt(normalized.substring(2, 4)));
        } catch (DateTimeException | NumberFormatException exception) {
            return null;
        }
    }

    private static String nullableText(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
    }
}
