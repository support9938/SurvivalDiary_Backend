package com.survivaldiary.domain.map.client;

import com.survivaldiary.domain.news.client.NaverNewsCredentials;
import com.survivaldiary.global.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
public class NaverPlaceSearchClient {

    private static final String SEARCH_PATH = "/search/v1/local";
    private static final String CLIENT_ID_HEADER = "X-NCP-APIGW-API-KEY-ID";
    private static final String CLIENT_SECRET_HEADER = "X-NCP-APIGW-API-KEY";

    private final RestClient restClient;
    private final NaverNewsCredentials credentials;
    private final String naverClientId;
    private final String naverClientSecret;

    public NaverPlaceSearchClient(
            @Qualifier("naverNewsRestClient") RestClient restClient,
            NaverNewsCredentials credentials,
            @Value("${oauth.naver.client-id:}") String naverClientId,
            @Value("${oauth.naver.client-secret:}") String naverClientSecret
    ) {
        this.restClient = restClient;
        this.credentials = credentials;
        this.naverClientId = naverClientId == null ? "" : naverClientId.trim();
        this.naverClientSecret = naverClientSecret == null ? "" : naverClientSecret.trim();
    }

    public List<Place> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<Place> apiHubPlaces = searchApiHub(query, limit);
        if (!apiHubPlaces.isEmpty()) {
            return apiHubPlaces;
        }
        return searchOpenApi(query, limit);
    }

    private List<Place> searchApiHub(String query, int limit) {
        if (!credentials.isConfigured()) {
            return List.of();
        }
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SEARCH_PATH)
                            .queryParam("query", query.trim())
                            .queryParam("display", Math.min(Math.max(limit, 1), 5))
                            .queryParam("sort", "random")
                            .build())
                    .header(CLIENT_ID_HEADER, credentials.requireClientId())
                    .header(CLIENT_SECRET_HEADER, credentials.requireClientSecret())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, responseError) -> {
                        throw new IllegalStateException("Naver local search is unavailable");
                    })
                    .body(JsonNode.class);
            return parsePlaces(response);
        } catch (BusinessException | RestClientException | IllegalStateException exception) {
            return List.of();
        }
    }

    private List<Place> searchOpenApi(String query, int limit) {
        if (naverClientId.isBlank() || naverClientSecret.isBlank()) {
            return List.of();
        }
        try {
            JsonNode response = RestClient.create("https://openapi.naver.com").get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search/local.json")
                            .queryParam("query", query.trim())
                            .queryParam("display", Math.min(Math.max(limit, 1), 5))
                            .queryParam("sort", "random")
                            .build())
                    .header("X-Naver-Client-Id", naverClientId)
                    .header("X-Naver-Client-Secret", naverClientSecret)
                    .retrieve()
                    .body(JsonNode.class);
            return parsePlaces(response);
        } catch (RestClientException exception) {
            return List.of();
        }
    }

    private static List<Place> parsePlaces(JsonNode response) {
        if (response == null || !response.path("items").isArray()) {
            return List.of();
        }
        List<Place> places = new ArrayList<>();
        for (JsonNode item : response.path("items")) {
            double longitude = item.path("mapx").asDouble(Double.NaN);
            double latitude = item.path("mapy").asDouble(Double.NaN);
            if (!isCoordinate(latitude, longitude)) {
                continue;
            }
            places.add(new Place(
                    stripTags(item.path("title").asText("")),
                    item.path("roadAddress").asText(item.path("address").asText("")),
                    latitude,
                    longitude
            ));
        }
        return List.copyOf(places);
    }

    private static boolean isCoordinate(double latitude, double longitude) {
        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    private static String stripTags(String value) {
        return value == null ? "" : value.replaceAll("<[^>]*>", "").trim();
    }

    public record Place(String name, String address, double latitude, double longitude) {
    }
}
