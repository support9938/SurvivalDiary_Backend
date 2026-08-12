package com.survivaldiary.domain.news.client;

import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class NaverNewsClient {

    private static final String SEARCH_PATH = "/search/v1/news";
    private static final String CLIENT_ID_HEADER = "X-NCP-APIGW-API-KEY-ID";
    private static final String CLIENT_SECRET_HEADER = "X-NCP-APIGW-API-KEY";

    private final RestClient restClient;
    private final NaverNewsProperties properties;
    private final NaverNewsCredentials credentials;
    private final ObjectMapper objectMapper;

    public NaverNewsClient(
            @Qualifier("naverNewsRestClient") RestClient restClient,
            NaverNewsProperties properties,
            NaverNewsCredentials credentials,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.credentials = credentials;
        this.objectMapper = objectMapper;
    }

    public List<NaverNewsItem> searchPage(String query, int pageIndex) {
        try {
            String body = restClient.get()
                    .uri(searchUri(query, pageIndex))
                    .accept(MediaType.APPLICATION_JSON)
                    .header(CLIENT_ID_HEADER, credentials.requireClientId())
                    .header(CLIENT_SECRET_HEADER, credentials.requireClientSecret())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw providerError(response.getStatusCode());
                    })
                    .body(String.class);
            return parse(body);
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            log.warn("네이버 뉴스 제공처 연결 실패: query={}", query);
            throw new BusinessException(ErrorCode.NEWS_PROVIDER_UNAVAILABLE);
        } catch (RestClientException exception) {
            log.warn(
                    "네이버 뉴스 제공처 응답 처리 실패: query={}, exceptionType={}",
                    query,
                    exception.getClass().getSimpleName()
            );
            throw new BusinessException(ErrorCode.NEWS_PROVIDER_BAD_RESPONSE);
        }
    }

    private URI searchUri(String query, int pageIndex) {
        return UriComponentsBuilder.fromUri(properties.getBaseUrl())
                .path(SEARCH_PATH)
                .queryParam("query", query)
                .queryParam("display", properties.normalizedDisplay())
                .queryParam("start", properties.startForPage(pageIndex))
                .queryParam("sort", "sim")
                .queryParam("format", "json")
                .build()
                .encode()
                .toUri();
    }

    private List<NaverNewsItem> parse(String body) {
        if (body == null || body.isBlank()) {
            throw new BusinessException(ErrorCode.NEWS_PROVIDER_BAD_RESPONSE);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JacksonException exception) {
            log.warn("네이버 뉴스 제공처 JSON 파싱 실패: exceptionType={}",
                    exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.NEWS_PROVIDER_BAD_RESPONSE);
        }

        JsonNode items = root.path("items");
        if (items == null || !items.isArray()) {
            throw new BusinessException(ErrorCode.NEWS_PROVIDER_BAD_RESPONSE);
        }

        List<NaverNewsItem> result = new ArrayList<>();
        for (JsonNode item : items) {
            result.add(new NaverNewsItem(
                    item.path("title").asText(""),
                    item.path("originallink").asText(""),
                    item.path("link").asText(""),
                    item.path("description").asText(""),
                    item.path("pubDate").asText("")
            ));
        }
        return List.copyOf(result);
    }

    private BusinessException providerError(HttpStatusCode status) {
        if (status.value() == 401
                || status.value() == 403
                || status.value() == 429
                || status.is5xxServerError()) {
            return new BusinessException(ErrorCode.NEWS_PROVIDER_UNAVAILABLE);
        }
        return new BusinessException(ErrorCode.NEWS_PROVIDER_BAD_RESPONSE);
    }
}
