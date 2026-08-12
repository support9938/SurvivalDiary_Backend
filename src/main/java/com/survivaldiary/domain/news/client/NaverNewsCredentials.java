package com.survivaldiary.domain.news.client;

import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class NaverNewsCredentials {

    private static final String CLIENT_ID_PROPERTY = "NAVER_API_HUB_CLIENT_ID";
    private static final String CLIENT_SECRET_PROPERTY = "NAVER_API_HUB_CLIENT_SECRET";

    private final NaverNewsProperties properties;
    private final Environment environment;

    public NaverNewsCredentials(
            NaverNewsProperties properties,
            Environment environment
    ) {
        this.properties = properties;
        this.environment = environment;
    }

    public boolean isConfigured() {
        return !clientId().isBlank() && !clientSecret().isBlank();
    }

    public String requireClientId() {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.NEWS_PROVIDER_UNAVAILABLE);
        }
        return clientId();
    }

    public String requireClientSecret() {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.NEWS_PROVIDER_UNAVAILABLE);
        }
        return clientSecret();
    }

    private String clientId() {
        return resolve(properties.getClientId(), CLIENT_ID_PROPERTY);
    }

    private String clientSecret() {
        return resolve(properties.getClientSecret(), CLIENT_SECRET_PROPERTY);
    }

    private String resolve(String configuredValue, String rootPropertyName) {
        if (configuredValue != null && !configuredValue.isBlank()) {
            return configuredValue.trim();
        }
        String rootValue = environment.getProperty(rootPropertyName, "");
        return rootValue == null ? "" : rootValue.trim();
    }
}
