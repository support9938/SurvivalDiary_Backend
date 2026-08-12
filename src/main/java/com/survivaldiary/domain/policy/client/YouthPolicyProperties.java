package com.survivaldiary.domain.policy.client;

import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "policy.provider")
public class YouthPolicyProperties {

    private URI baseUrl = URI.create("https://www.youthcenter.go.kr");
    private String apiKey;
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(5);
    private int retryCount = 1;
    private Duration retryDelay = Duration.ofMillis(300);
    private Duration cacheTtl = Duration.ofMinutes(10);
    private Duration staleCacheTtl = Duration.ofHours(1);
    private int cacheMaxEntries = 128;

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = Math.max(0, retryCount);
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay == null || retryDelay.isNegative()
                ? Duration.ZERO
                : retryDelay;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl == null || cacheTtl.isNegative()
                ? Duration.ZERO
                : cacheTtl;
    }

    public Duration getStaleCacheTtl() {
        return staleCacheTtl;
    }

    public void setStaleCacheTtl(Duration staleCacheTtl) {
        this.staleCacheTtl = staleCacheTtl == null || staleCacheTtl.isNegative()
                ? Duration.ZERO
                : staleCacheTtl;
    }

    public int getCacheMaxEntries() {
        return cacheMaxEntries;
    }

    public void setCacheMaxEntries(int cacheMaxEntries) {
        this.cacheMaxEntries = Math.max(1, cacheMaxEntries);
    }

    public String requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
        }
        return apiKey.trim();
    }
}
