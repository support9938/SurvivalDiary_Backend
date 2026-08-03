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
}
