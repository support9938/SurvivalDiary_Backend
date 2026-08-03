package com.survivaldiary.domain.map.client;

import com.survivaldiary.domain.map.dto.GoodPriceStoreResponse;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public class GoodPriceStoreClient {

    private static final String PATH =
            "/3045247/v1/uddi:afd3af75-a7d4-403d-b6e0-823c848d935d";

    private final RestClient restClient;
    private final GoodPriceStoreProperties properties;

    public GoodPriceStoreClient(
            @Qualifier("goodPriceStoreRestClient") RestClient restClient,
            GoodPriceStoreProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public GoodPriceStoreResponse fetchStores(
            int page,
            int perPage,
            String province,
            String district
    ) {
        try {
            GoodPriceStoreResponse response = restClient.get()
                    .uri(uriBuilder -> buildUri(
                            uriBuilder,
                            page,
                            perPage,
                            province,
                            district
                    ))
                    .header(HttpHeaders.AUTHORIZATION, "Infuser " + decodedApiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, responseError) -> {
                        throw providerError(responseError.getStatusCode());
                    })
                    .body(GoodPriceStoreResponse.class);
            if (response == null || response.data() == null) {
                throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
            }
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
        } catch (RestClientException | HttpMessageConversionException exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }
    }

    private String decodedApiKey() {
        String apiKey = properties.requireApiKey();

        if (apiKey.contains("%")) {
            return URLDecoder.decode(apiKey, StandardCharsets.UTF_8);
        }

        return apiKey;
    }

    private java.net.URI buildUri(
            UriBuilder uriBuilder,
            int page,
            int perPage,
            String province,
            String district
    ) {
        uriBuilder
                .path(PATH)
                .queryParam("page", page)
                .queryParam("perPage", perPage)
                .queryParam("returnType", "JSON");

        if (province != null) {
            uriBuilder.queryParam("cond[시도::EQ]", province);
        }
        if (district != null) {
            uriBuilder.queryParam("cond[시군::EQ]", district);
        }
        return uriBuilder.build();
    }

    private BusinessException providerError(HttpStatusCode status) {
        if (status.value() == 401 || status.value() == 403 || status.is5xxServerError()) {
            return new BusinessException(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
        }
        return new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
    }
}
