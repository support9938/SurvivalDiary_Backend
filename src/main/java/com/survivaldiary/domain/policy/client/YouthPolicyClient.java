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
import java.util.Locale;

@Slf4j
@Component
public class YouthPolicyClient {

    private static final String POLICY_PATH = "/go/ythip/getPlcy";

    private final RestClient restClient;
    private final YouthPolicyProperties properties;

    public YouthPolicyClient(RestClient youthPolicyRestClient, YouthPolicyProperties properties) {
        this.restClient = youthPolicyRestClient;
        this.properties = properties;
    }

    public JsonNode search(YouthPolicySearchRequest request) {
        ProviderOperation operation = ProviderOperation.SEARCH;
        String apiKey = requireApiKey(operation);

        return execute(operation, () -> restClient.get()
                .uri(uriBuilder -> buildSearchUri(uriBuilder, apiKey, request))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw providerError(response.getStatusCode(), false, operation);
                })
                .body(JsonNode.class));
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
                .body(JsonNode.class));
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

    private JsonNode execute(ProviderOperation operation, ProviderCall call) {
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
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            log.warn(
                    "온통청년 정책 제공처 연결 실패: operation={}, reason={}",
                    operation,
                    classifyResourceAccess(exception)
            );
            throw new BusinessException(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
        } catch (RestClientException | HttpMessageConversionException exception) {
            log.warn(
                    "온통청년 정책 제공처 응답 오류: operation={}, reason=RESPONSE_PROCESSING_FAILURE, exceptionType={}",
                    operation,
                    exception.getClass().getSimpleName()
            );
            throw new BusinessException(ErrorCode.POLICY_PROVIDER_BAD_RESPONSE);
        }
    }

    private BusinessException providerError(
            HttpStatusCode status,
            boolean detailRequest,
            ProviderOperation operation
    ) {
        if (detailRequest && status.value() == 404) {
            return new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }
        if (status.value() == 401 || status.value() == 403 || status.is5xxServerError()) {
            String reason = status.value() == 401 || status.value() == 403
                    ? "AUTH_REJECTED"
                    : "PROVIDER_SERVER_ERROR";
            log.warn(
                    "온통청년 정책 제공처 HTTP 오류: operation={}, reason={}, status={}",
                    operation,
                    reason,
                    status.value()
            );
            return new BusinessException(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
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
}
