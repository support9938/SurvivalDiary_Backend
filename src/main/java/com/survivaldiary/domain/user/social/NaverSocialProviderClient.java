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
        String name = nullableText(response.path("name"));
        if (name == null) {
            name = nullableText(response.path("nickname"));
        }
        return new SocialProfile(
                providerUserId,
                nullableText(response.path("email")),
                name
        );
    }

    private static String nullableText(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
    }
}
