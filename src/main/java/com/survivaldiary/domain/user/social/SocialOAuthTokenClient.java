package com.survivaldiary.domain.user.social;

import tools.jackson.databind.JsonNode;
import com.survivaldiary.domain.user.dto.WebSocialLoginRequest;
import com.survivaldiary.domain.user.entity.SocialAccount;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class SocialOAuthTokenClient {

    private final String kakaoRestApiKey;
    private final String kakaoClientSecret;
    private final String naverClientId;
    private final String naverClientSecret;

    public SocialOAuthTokenClient(
            @Value("${oauth.kakao.rest-api-key:}") String kakaoRestApiKey,
            @Value("${oauth.kakao.client-secret:}") String kakaoClientSecret,
            @Value("${oauth.naver.client-id:}") String naverClientId,
            @Value("${oauth.naver.client-secret:}") String naverClientSecret
    ) {
        this.kakaoRestApiKey = kakaoRestApiKey;
        this.kakaoClientSecret = kakaoClientSecret;
        this.naverClientId = naverClientId;
        this.naverClientSecret = naverClientSecret;
    }

    public String exchange(
            SocialAccount.Provider provider,
            WebSocialLoginRequest request
    ) {
        try {
            JsonNode response = switch (provider) {
                case KAKAO -> exchangeKakao(request);
                case NAVER -> exchangeNaver(request);
            };
            String accessToken = response == null ? null : response.path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalArgumentException("OAuth access token is missing");
            }
            return accessToken;
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.SOCIAL_AUTH_FAILED);
        }
    }

    private JsonNode exchangeKakao(WebSocialLoginRequest request) {
        requireConfigured(kakaoRestApiKey);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakaoRestApiKey);
        form.add("redirect_uri", request.redirectUri());
        form.add("code", request.authorizationCode());
        if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
            form.add("client_secret", kakaoClientSecret);
        }
        return postForm("https://kauth.kakao.com/oauth/token", form);
    }

    private JsonNode exchangeNaver(WebSocialLoginRequest request) {
        requireConfigured(naverClientId);
        requireConfigured(naverClientSecret);
        requireConfigured(request.state());
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", naverClientId);
        form.add("client_secret", naverClientSecret);
        form.add("code", request.authorizationCode());
        form.add("state", request.state());
        return RestClient.create()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("nid.naver.com")
                        .path("/oauth2.0/token")
                        .queryParams(form)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    private JsonNode postForm(String url, MultiValueMap<String, String> form) {
        return RestClient.create()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
    }

    private void requireConfigured(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OAuth client is not configured");
        }
    }
}
