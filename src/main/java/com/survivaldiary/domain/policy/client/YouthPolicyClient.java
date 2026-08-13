package com.survivaldiary.domain.policy.client;

import com.survivaldiary.domain.policy.client.dto.YouthPolicySearchRequest;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
public class YouthPolicyClient {

    private static final String POLICY_PATH = "/go/ythip/getPlcy";

    private final RestClient restClient;
    private final YouthPolicyProperties properties;
    private final Map<YouthPolicySearchRequest, CachedResponse> searchCache =
            new LinkedHashMap<>(16, 0.75f, true);

    public YouthPolicyClient(RestClient youthPolicyRestClient, YouthPolicyProperties properties) {
        this.restClient = youthPolicyRestClient;
        this.properties = properties;
    }

    public JsonNode search(YouthPolicySearchRequest request) {
        ProviderOperation operation = ProviderOperation.SEARCH;
        String apiKey = requireApiKey(operation);
        JsonNode freshCache = cachedResponse(request, properties.getCacheTtl());
        if (freshCache != null) {
            return freshCache;
        }

        ProviderResult result = execute(operation, () -> restClient.get()
                .uri(uriBuilder -> buildSearchUri(uriBuilder, apiKey, request))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw providerError(response.getStatusCode(), false, operation);
                })
                .body(JsonNode.class), () -> cachedResponse(request, properties.getStaleCacheTtl()));
        if (!result.fromFallback()) {
            cacheResponse(request, result.body());
        }
        return result.body();
    }

    public JsonNode findDetail(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_POLICY_FILTER);
        }
        ProviderOperation operation = ProviderOperation.DETAIL;
        String apiKey = requireApiKey(operation);

        return execute(operation, () -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(POLICY_PATH)
                        .queryParam("apiKeyNm", apiKey)
                        .queryParam("pageType", "2")
                        .queryParam("plcyNo", policyId.trim())
                        .queryParam("rtnType", "json")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw providerError(response.getStatusCode(), true, operation);
                })
                .body(JsonNode.class), () -> null).body();
    }

    private String requireApiKey(ProviderOperation operation) {
        try {
            return properties.requireApiKey();
        } catch (BusinessException exception) {
            log.error(
                    "온통청년 정책 제공처 호출 실패: operation={}, reason=API_KEY_MISSING",
                    operation
            );
            throw exception;
        }
    }

    private ProviderResult execute(
            ProviderOperation operation,
            ProviderCall call,
            Supplier<JsonNode> staleFallback
    ) {
        int maxAttempts = properties.getRetryCount() + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return new ProviderResult(executeOnce(operation, call), false);
            } catch (TransientProviderException exception) {
                if (attempt < maxAttempts) {
                    log.warn(
                            "온통청년 정책 제공처 일시 오류 재시도: operation={}, attempt={}/{}",
                            operation,
                            attempt + 1,
                            maxAttempts
                    );
                    waitBeforeRetry();
                    continue;
                }

                JsonNode cached = staleFallback.get();
                if (cached != null) {
                    log.warn(
                            "온통청년 정책 제공처 장애로 최근 성공 응답 사용: operation={}",
                            operation
                    );
                    return new ProviderResult(cached, true);
                }
                throw new BusinessException(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
            }
        }
        throw new BusinessException(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
    }

    private JsonNode executeOnce(ProviderOperation operation, ProviderCall call) {
        try {
            JsonNode response = call.execute();
            if (response == null) {
                log.warn(
                        "온통청년 정책 제공처 응답 오류: operation={}, reason=NULL_RESPONSE",
                        operation
                );
                throw new BusinessException(ErrorCode.POLICY_PROVIDER_BAD_RESPONSE);
            }
            return response;
        } catch (BusinessException | TransientProviderException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            log.warn(
                    "온통청년 정책 제공처 연결 실패: operation={}, reason={}",
                    operation,
                    classifyResourceAccess(exception)
            );
            throw new TransientProviderException(exception);
        } catch (RestClientException | HttpMessageConversionException exception) {
            log.warn(
                    "온통청년 정책 제공처 응답 오류: operation={}, reason=RESPONSE_PROCESSING_FAILURE, exceptionType={}",
                    operation,
                    exception.getClass().getSimpleName()
            );
            throw new BusinessException(ErrorCode.POLICY_PROVIDER_BAD_RESPONSE);
        }
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(properties.getRetryDelay().toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
        }
    }

    private JsonNode cachedResponse(YouthPolicySearchRequest request, Duration maxAge) {
        synchronized (searchCache) {
            CachedResponse cached = searchCache.get(request);
            if (cached == null) {
                return null;
            }
            Duration age = Duration.between(cached.savedAt(), Instant.now());
            if (age.compareTo(maxAge) > 0) {
                if (age.compareTo(properties.getStaleCacheTtl()) > 0) {
                    searchCache.remove(request);
                }
                return null;
            }
            return cached.body().deepCopy();
        }
    }

    private void cacheResponse(YouthPolicySearchRequest request, JsonNode response) {
        synchronized (searchCache) {
            searchCache.put(request, new CachedResponse(response.deepCopy(), Instant.now()));
            while (searchCache.size() > properties.getCacheMaxEntries()) {
                Iterator<YouthPolicySearchRequest> iterator = searchCache.keySet().iterator();
                iterator.next();
                iterator.remove();
            }
        }
    }

    private RuntimeException providerError(
            HttpStatusCode status,
            boolean detailRequest,
            ProviderOperation operation
    ) {
        if (detailRequest && status.value() == 404) {
            return new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }
        if (status.value() == 401 || status.value() == 403) {
            log.warn(
                    "온통청년 정책 제공처 HTTP 오류: operation={}, reason={}, status={}",
                    operation,
                    "AUTH_REJECTED",
                    status.value()
            );
            return new BusinessException(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
        }
        if (status.is5xxServerError()) {
            log.warn(
                    "온통청년 정책 제공처 HTTP 오류: operation={}, reason={}, status={}",
                    operation,
                    "PROVIDER_SERVER_ERROR",
                    status.value()
            );
            return new TransientProviderException();
        }
        log.warn(
                "온통청년 정책 제공처 HTTP 오류: operation={}, reason=UNEXPECTED_HTTP_STATUS, status={}",
                operation,
                status.value()
        );
        return new BusinessException(ErrorCode.POLICY_PROVIDER_BAD_RESPONSE);
    }

    private String classifyResourceAccess(ResourceAccessException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof UnknownHostException) {
                return "DNS_FAILURE";
            }
            if (cause instanceof ConnectException) {
                return "CONNECTION_FAILURE";
            }
            if (cause instanceof SocketTimeoutException) {
                return classifyTimeout(cause.getMessage());
            }
            cause = cause.getCause();
        }
        return "RESOURCE_ACCESS_FAILURE";
    }

    private String classifyTimeout(String message) {
        if (message != null) {
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("connect")) {
                return "CONNECT_TIMEOUT";
            }
            if (normalized.contains("read")) {
                return "READ_TIMEOUT";
            }
        }
        return "NETWORK_TIMEOUT";
    }

    private java.net.URI buildSearchUri(
            UriBuilder uriBuilder,
            String apiKey,
            YouthPolicySearchRequest request
    ) {
        uriBuilder
                .path(POLICY_PATH)
                .queryParam("apiKeyNm", apiKey)
                .queryParam("pageNum", request.pageNumber())
                .queryParam("pageSize", request.pageSize())
                .queryParam("pageType", "1")
                .queryParam("rtnType", "json");

        addQueryParam(uriBuilder, "zipCd", request.zipCode());
        addQueryParam(uriBuilder, "lclsfNm", request.largeCategoryName());
        addQueryParam(uriBuilder, "mclsfNm", request.middleCategoryName());
        addQueryParam(uriBuilder, "plcyKywdNm", request.policyKeyword());
        addQueryParam(uriBuilder, "plcyNm", request.policyName());
        return uriBuilder.build();
    }

    private void addQueryParam(UriBuilder uriBuilder, String name, String value) {
        if (value != null) {
            uriBuilder.queryParam(name, value);
        }
    }

    @FunctionalInterface
    private interface ProviderCall {
        JsonNode execute();
    }

    private enum ProviderOperation {
        SEARCH,
        DETAIL
    }

    private record CachedResponse(JsonNode body, Instant savedAt) {
    }

    private record ProviderResult(JsonNode body, boolean fromFallback) {
    }

    private static final class TransientProviderException extends RuntimeException {
        private TransientProviderException() {
        }

        private TransientProviderException(Throwable cause) {
            super(cause);
        }
    }
}
