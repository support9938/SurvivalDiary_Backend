package com.survivaldiary.domain.user.social;

import tools.jackson.databind.JsonNode;
import com.survivaldiary.domain.user.entity.SocialAccount;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
        String name = nullableText(account.path("profile").path("nickname"));
        return new SocialProfile(body.path("id").asText(), email, name);
    }

    private static String nullableText(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
    }
}
