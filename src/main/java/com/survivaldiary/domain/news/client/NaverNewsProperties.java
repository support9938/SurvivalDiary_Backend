package com.survivaldiary.domain.news.client;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "news.provider")
public class NaverNewsProperties {

    private URI baseUrl = URI.create("https://naverapihub.apigw.ntruss.com");
    private String clientId = "";
    private String clientSecret = "";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(5);
    private Duration cacheTtl = Duration.ofHours(1);
    private int display = 10;
    private int pagesPerTopic = 1;

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
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

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public int getDisplay() {
        return display;
    }

    public void setDisplay(int display) {
        this.display = display;
    }

    public int getPagesPerTopic() {
        return pagesPerTopic;
    }

    public void setPagesPerTopic(int pagesPerTopic) {
        this.pagesPerTopic = pagesPerTopic;
    }

    public int normalizedDisplay() {
        return Math.min(Math.max(display, 1), 100);
    }

    public int normalizedPagesPerTopic() {
        int maxPageCount = ((1000 - 1) / normalizedDisplay()) + 1;
        return Math.min(Math.max(pagesPerTopic, 1), maxPageCount);
    }

    public int startForPage(int pageIndex) {
        return pageIndex * normalizedDisplay() + 1;
    }
}
