package com.survivaldiary.domain.map.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class NaverGeocodingClient {

    private static final String GEOCODING_PATH = "/map-geocode/v2/geocode";
    private static final String REVERSE_GEOCODING_PATH = "/map-reversegeocode/v2/gc";
    private static final String API_KEY_ID_HEADER = "x-ncp-apigw-api-key-id";
    private static final String API_KEY_HEADER = "x-ncp-apigw-api-key";

    private final RestClient restClient;
    private final NaverGeocodingProperties properties;
    private final ConcurrentMap<String, Optional<Coordinates>> cache =
            new ConcurrentHashMap<>();

    public NaverGeocodingClient(
            @Qualifier("naverGeocodingRestClient") RestClient restClient,
            NaverGeocodingProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public Optional<Coordinates> findCoordinates(String address) {
        if (!properties.isConfigured() || address == null || address.isBlank()) {
            return Optional.empty();
        }
        return cache.computeIfAbsent(address.trim(), this::requestCoordinates);
    }

    public Optional<Region> findRegion(double latitude, double longitude) {
        if (!properties.isConfigured() || !Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            return Optional.empty();
        }
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(REVERSE_GEOCODING_PATH)
                            .queryParam("coords", longitude + "," + latitude)
                            .queryParam("orders", "legalcode,admcode")
                            .queryParam("output", "json")
                            .build())
                    .header(API_KEY_ID_HEADER, properties.getApiKeyId().trim())
                    .header(API_KEY_HEADER, properties.getApiKey().trim())
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode results = response == null ? null : response.path("results");
            if (results == null || !results.isArray() || results.isEmpty()) return Optional.empty();
            JsonNode regionResult = results.get(0);
            for (JsonNode result : results) {
                if ("legalcode".equals(result.path("name").asText())) {
                    regionResult = result;
                    break;
                }
            }
            String province = regionResult.path("region").path("area1").path("name").asText("").trim();
            String district = regionResult.path("region").path("area2").path("name").asText("").trim();
            String code = regionResult.path("code").path("id").asText("").trim();
            if (province.isEmpty() || district.isEmpty()) return Optional.empty();
            return Optional.of(new Region(province, district, code.length() < 5 ? null : code.substring(0, 5)));
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    public Optional<Region> findRegionByAddress(String address) {
        if (!properties.isConfigured() || address == null || address.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(GEOCODING_PATH)
                            .queryParam("query", address.trim())
                            .build())
                    .header(API_KEY_ID_HEADER, properties.getApiKeyId().trim())
                    .header(API_KEY_HEADER, properties.getApiKey().trim())
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode firstAddress = response == null ? null : response.path("addresses").path(0);
            if (firstAddress == null || firstAddress.isMissingNode()) return Optional.empty();
            String province = "";
            String district = "";
            String lawdCode = null;
            for (JsonNode element : firstAddress.path("addressElements")) {
                String type = element.path("types").path(0).asText("");
                String name = element.path("longName").asText("").trim();
                String code = element.path("code").asText("").trim();
                if ("SIDO".equals(type)) province = name;
                if ("SIGUGUN".equals(type)) {
                    district = name;
                    lawdCode = code.length() < 5 ? null : code.substring(0, 5);
                }
            }
            if (province.isEmpty() || district.isEmpty()) return Optional.empty();
            if (lawdCode == null) {
                double longitude = firstAddress.path("x").asDouble(Double.NaN);
                double latitude = firstAddress.path("y").asDouble(Double.NaN);
                if (Double.isFinite(latitude) && Double.isFinite(longitude)) {
                    String resolvedProvince = province;
                    String resolvedDistrict = district;
                    return findRegion(latitude, longitude)
                            .or(() -> Optional.of(new Region(resolvedProvince, resolvedDistrict, null)));
                }
            }
            return Optional.of(new Region(province, district, lawdCode));
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    private Optional<Coordinates> requestCoordinates(String address) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(GEOCODING_PATH)
                            .queryParam("query", address)
                            .build())
                    .header(API_KEY_ID_HEADER, properties.getApiKeyId().trim())
                    .header(API_KEY_HEADER, properties.getApiKey().trim())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return Optional.empty();
            }
            JsonNode addresses = response.path("addresses");
            if (!addresses.isArray() || addresses.isEmpty()) {
                return Optional.empty();
            }
            JsonNode first = addresses.get(0);
            double longitude = first.path("x").asDouble(Double.NaN);
            double latitude = first.path("y").asDouble(Double.NaN);
            if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
                return Optional.empty();
            }
            return Optional.of(new Coordinates(latitude, longitude));
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    public record Coordinates(double latitude, double longitude) {
    }

    public record Region(String province, String district, String lawdCode) {
    }
}
