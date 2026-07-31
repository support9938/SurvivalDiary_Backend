package com.survivaldiary.domain.policy.client;

import com.survivaldiary.domain.policy.client.dto.YouthPolicySearchRequest;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;

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
        String apiKey = properties.requireApiKey();

        return execute(() -> restClient.get()
                .uri(uriBuilder -> buildSearchUri(uriBuilder, apiKey, request))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw providerError(response.getStatusCode(), false);
                })
                .body(JsonNode.class));
    }

    public JsonNode findDetail(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_POLICY_FILTER);
        }
        String apiKey = properties.requireApiKey();

        return execute(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(POLICY_PATH)
                        .queryParam("apiKeyNm", apiKey)
                        .queryParam("pageType", "2")
                        .queryParam("plcyNo", policyId.trim())
                        .queryParam("rtnType", "json")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw providerError(response.getStatusCode(), true);
                })
                .body(JsonNode.class));
    }

    private JsonNode execute(ProviderCall call) {
        try {
            JsonNode response = call.execute();
            if (response == null) {
                throw new BusinessException(ErrorCode.POLICY_PROVIDER_BAD_RESPONSE);
            }
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new BusinessException(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
        } catch (RestClientException | HttpMessageConversionException exception) {
            throw new BusinessException(ErrorCode.POLICY_PROVIDER_BAD_RESPONSE);
        }
    }

    private BusinessException providerError(HttpStatusCode status, boolean detailRequest) {
        if (detailRequest && status.value() == 404) {
            return new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }
        if (status.value() == 401 || status.value() == 403 || status.is5xxServerError()) {
            return new BusinessException(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
        }
        return new BusinessException(ErrorCode.POLICY_PROVIDER_BAD_RESPONSE);
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
}
