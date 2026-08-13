package com.survivaldiary.domain.map.client;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
public class TmapPoiSearchClient {

    private static final String SEARCH_PATH = "/tmap/pois";
    private static final String APP_KEY_HEADER = "appKey";

    private final RestClient restClient;
    private final TmapDirectionsProperties properties;

    public TmapPoiSearchClient(
            @Qualifier("tmapDirectionsRestClient") RestClient restClient,
            TmapDirectionsProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public List<Place> search(String query, int limit) {
        if (!properties.isConfigured() || query == null || query.isBlank()) {
            return List.of();
        }
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SEARCH_PATH)
                            .queryParam("version", "1")
                            .queryParam("searchKeyword", query.trim())
                            .queryParam("count", Math.min(Math.max(limit, 1), 20))
                            .queryParam("page", 1)
                            .queryParam("reqCoordType", "WGS84GEO")
                            .queryParam("resCoordType", "WGS84GEO")
                            .build())
                    .header(APP_KEY_HEADER, properties.getAppKey().trim())
                    .retrieve()
                    .body(JsonNode.class);
            return parsePlaces(response);
        } catch (RestClientException | HttpMessageConversionException exception) {
            return List.of();
        }
    }

    static List<Place> parsePlaces(JsonNode response) {
        JsonNode pois = response == null
                ? null
                : response.path("searchPoiInfo").path("pois").path("poi");
        if (pois == null || !pois.isArray()) {
            return List.of();
        }

        List<Place> places = new ArrayList<>();
        for (JsonNode poi : pois) {
            double latitude = coordinate(poi, "frontLat", "noorLat");
            double longitude = coordinate(poi, "frontLon", "noorLon");
            if (!validCoordinates(latitude, longitude)) {
                continue;
            }
            String name = poi.path("name").asText("").trim();
            String address = joinAddress(
                    poi.path("upperAddrName").asText(""),
                    poi.path("middleAddrName").asText(""),
                    poi.path("lowerAddrName").asText(""),
                    poi.path("detailAddrName").asText("")
            );
            places.add(new Place(name, address, latitude, longitude));
        }
        return List.copyOf(places);
    }

    private static double coordinate(JsonNode poi, String preferredField, String fallbackField) {
        double preferred = poi.path(preferredField).asDouble(Double.NaN);
        return Double.isFinite(preferred)
                ? preferred
                : poi.path(fallbackField).asDouble(Double.NaN);
    }

    private static boolean validCoordinates(double latitude, double longitude) {
        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    private static String joinAddress(String... parts) {
        return String.join(" ", List.of(parts).stream()
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList());
    }

    public record Place(String name, String address, double latitude, double longitude) {
    }
}
