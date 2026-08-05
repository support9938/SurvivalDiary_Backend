package com.survivaldiary.domain.map.client;

import com.survivaldiary.domain.map.dto.PublicFacilityProviderPage;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class PublicFacilityClient {

    private static final String PATH =
            "/openapi/tn_pubr_public_pblfclt_opn_info_api";

    private final RestClient restClient;
    private final PublicFacilityProperties properties;

    public PublicFacilityClient(
            @Qualifier("publicFacilityRestClient") RestClient restClient,
            PublicFacilityProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public PublicFacilityProviderPage fetchPage(int page, int pageSize) {
        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> buildUri(uriBuilder, page, pageSize))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw providerError(response.getStatusCode());
                    })
                    .body(JsonNode.class);
            return parse(root, page, pageSize);
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
        } catch (RestClientException | HttpMessageConversionException exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }
    }

    private java.net.URI buildUri(UriBuilder uriBuilder, int page, int pageSize) {
        return uriBuilder
                .path(PATH)
                .queryParam("serviceKey", decodedApiKey())
                .queryParam("pageNo", page)
                .queryParam("numOfRows", pageSize)
                .queryParam("type", "json")
                .build();
    }

    private String decodedApiKey() {
        String apiKey = properties.requireApiKey();
        return apiKey.contains("%")
                ? URLDecoder.decode(apiKey, StandardCharsets.UTF_8)
                : apiKey;
    }

    private PublicFacilityProviderPage parse(JsonNode root, int page, int pageSize) {
        if (root == null || root.isNull()) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }
        rejectProviderError(root);

        List<PublicFacilityProviderPage.Item> items = new ArrayList<>();
        collectItems(root, items);
        int totalCount = intValue(root.findValue("totalCount"), items.size());
        return new PublicFacilityProviderPage(
                page,
                pageSize,
                totalCount,
                List.copyOf(items)
        );
    }

    private void rejectProviderError(JsonNode root) {
        JsonNode resultCodeNode = root.findValue("resultCode");
        if (resultCodeNode == null) {
            return;
        }
        String resultCode = resultCodeNode.asText("").trim();
        if (resultCode.isEmpty() || "00".equals(resultCode) || "0".equals(resultCode)) {
            return;
        }
        if (List.of("20", "21", "22", "30", "31", "32", "33")
                .contains(resultCode)) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
        }
        throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
    }

    private void collectItems(
            JsonNode node,
            List<PublicFacilityProviderPage.Item> items
    ) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject() && node.get("openFcltyNm") != null) {
            items.add(toItem(node));
            return;
        }
        node.forEach(child -> collectItems(child, items));
    }

    private PublicFacilityProviderPage.Item toItem(JsonNode node) {
        return new PublicFacilityProviderPage.Item(
                text(node, "openFcltyNm"),
                text(node, "openLcNm"),
                text(node, "openFcltyType"),
                text(node, "rstde"),
                text(node, "weekdayOperOpenHhmm"),
                text(node, "weekdayOperColseHhmm"),
                text(node, "wkendOperOpenHhmm"),
                text(node, "wkendOperCloseHhmm"),
                text(node, "pchrgUseYn"),
                text(node, "useStdrTime"),
                text(node, "rntfee"),
                text(node, "excessUseUnitTime"),
                text(node, "excessRntfee"),
                text(node, "aceptncPosblCo"),
                text(node, "ar"),
                text(node, "etcFclty"),
                text(node, "sbscrptnMthSe"),
                text(node, "fcltyPicInfo"),
                text(node, "rdnmadr"),
                text(node, "lnmadr"),
                text(node, "institutionNm"),
                text(node, "chrgDeptNm"),
                text(node, "phoneNumber"),
                text(node, "homepageUrl"),
                doubleValue(node.get("latitude")),
                doubleValue(node.get("longitude")),
                text(node, "referenceDate"),
                text(node, "instt_code")
        );
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText("").trim();
    }

    private static Double doubleValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return Double.valueOf(node.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static int intValue(JsonNode node, int fallback) {
        if (node == null || node.isNull()) {
            return fallback;
        }
        try {
            return Integer.parseInt(node.asText());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private BusinessException providerError(HttpStatusCode status) {
        if (status.value() == 401 || status.value() == 403 || status.is5xxServerError()) {
            return new BusinessException(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
        }
        return new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
    }
}
