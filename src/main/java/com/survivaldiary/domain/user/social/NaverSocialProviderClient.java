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
public class NaverSocialProviderClient implements SocialProviderClient {

    private final RestClient restClient;

    public NaverSocialProviderClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://openapi.naver.com")
                .build();
    }

    NaverSocialProviderClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public SocialAccount.Provider provider() {
        return SocialAccount.Provider.NAVER;
    }

    @Override
    public SocialProfile verify(String accessToken) {
        try {
            JsonNode body = restClient.get()
                    .uri("/v1/nid/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            return parse(body);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.SOCIAL_AUTH_FAILED);
        }
    }

    static SocialProfile parse(JsonNode body) {
        if (body == null || !"00".equals(body.path("resultcode").asText())) {
            throw new IllegalArgumentException("Naver user response failed");
        }
        JsonNode response = body.path("response");
        String providerUserId = nullableText(response.path("id"));
        if (providerUserId == null) {
            throw new IllegalArgumentException("Naver user id is missing");
        }
        String nickname = nullableText(response.path("nickname"));
        String name = nullableText(response.path("name"));
        if (name == null) name = nickname;
        String profileImageUrl = nullableText(response.path("profile_image"));
        String genderValue = nullableText(response.path("gender"));
        User.Gender gender = "M".equals(genderValue)
                ? User.Gender.MALE
                : "F".equals(genderValue) ? User.Gender.FEMALE : null;
        Integer birthYear = parseBirthYear(nullableText(response.path("birthyear")));
        return new SocialProfile(
                providerUserId,
                nullableText(response.path("email")),
                name,
                nickname,
                profileImageUrl,
                nullableText(response.path("mobile")),
                gender,
                birthYear,
                parseBirthDate(birthYear,
                        nullableText(response.path("birthday")))
        );
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
        String[] parts = birthday.split("-");
        if (parts.length != 2) return null;
        try {
            return LocalDate.of(birthYear, Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]));
        } catch (DateTimeException | NumberFormatException exception) {
            return null;
        }
    }

    private static String nullableText(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
    }
}
