package com.survivaldiary.domain.map.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

@Component
public class TmapReverseGeocodingClient {

    private static final String REVERSE_GEOCODING_PATH = "/tmap/geo/reversegeocoding";
    private static final String APP_KEY_HEADER = "appKey";
    private static final String COORDINATE_TYPE = "WGS84GEO";

    private final RestClient restClient;
    private final TmapDirectionsProperties properties;

    public TmapReverseGeocodingClient(
            @Qualifier("tmapDirectionsRestClient") RestClient restClient,
            TmapDirectionsProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public Optional<Region> findRegion(double latitude, double longitude) {
        if (!properties.isConfigured() || !isValidCoordinate(latitude, longitude)) {
            return Optional.empty();
        }

        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(REVERSE_GEOCODING_PATH)
                            .queryParam("version", "1")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("coordType", COORDINATE_TYPE)
                            .queryParam("addressType", "A10")
                            .queryParam("newAddressExtend", "Y")
                            .build())
                    .header(APP_KEY_HEADER, properties.getAppKey().trim())
                    .retrieve()
                    .body(JsonNode.class);
            return parseRegion(response);
        } catch (RestClientException | HttpMessageConversionException exception) {
            return Optional.empty();
        }
    }

    private Optional<Region> parseRegion(JsonNode response) {
        if (response == null) {
            return Optional.empty();
        }

        JsonNode addressInfo = response.path("addressInfo");
        String province = addressInfo.path("city_do").asText("").trim();
        String district = addressInfo.path("gu_gun").asText("").trim();
        String legalDongCode = addressInfo.path("legalDongCode").asText("").trim();
        if (district.isEmpty() && "세종특별자치시".equals(province)) {
            district = province;
        }
        if (province.isEmpty()
                || district.isEmpty()
                || !legalDongCode.matches("\\d{10}")) {
            return Optional.empty();
        }

        return Optional.of(new Region(
                province,
                district,
                legalDongCode.substring(0, 5)
        ));
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        return Double.isFinite(latitude)
                && latitude >= -90
                && latitude <= 90
                && Double.isFinite(longitude)
                && longitude >= -180
                && longitude <= 180;
    }

    public record Region(String province, String district, String lawdCode) {
    }
}
