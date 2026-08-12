package com.survivaldiary.domain.map.client;

import com.survivaldiary.domain.map.dto.DirectionsMode;
import com.survivaldiary.domain.map.dto.DirectionsRequest;
import com.survivaldiary.domain.map.dto.DirectionsResponse;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TmapDirectionsClient {

    private static final String WALKING_DIRECTIONS_PATH = "/tmap/routes/pedestrian";
    private static final String DRIVING_DIRECTIONS_PATH = "/tmap/routes";
    private static final String APP_KEY_HEADER = "appKey";
    private static final String COORDINATE_TYPE = "WGS84GEO";
    private static final String SEARCH_OPTION_RECOMMENDED = "0";

    private final RestClient restClient;
    private final TmapDirectionsProperties properties;

    public TmapDirectionsClient(
            @Qualifier("tmapDirectionsRestClient") RestClient restClient,
            TmapDirectionsProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public DirectionsResponse findOptimalRoute(DirectionsRequest request) {
        if (!properties.isConfigured()) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
        }

        try {
            DirectionsMode mode = request.resolvedMode();
            JsonNode response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(directionsPath(mode))
                            .queryParam("version", "1")
                            .build())
                    .header(APP_KEY_HEADER, properties.getAppKey().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody(request, mode))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, errorResponse) -> {
                        throw providerError(errorResponse.getStatusCode());
                    })
                    .body(JsonNode.class);
            return parseResponse(response);
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
        } catch (RestClientException | HttpMessageConversionException exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }
    }

    private String directionsPath(DirectionsMode mode) {
        return mode == DirectionsMode.DRIVING
                ? DRIVING_DIRECTIONS_PATH
                : WALKING_DIRECTIONS_PATH;
    }

    private Map<String, Object> requestBody(
            DirectionsRequest request,
            DirectionsMode mode
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("startX", request.startLongitude());
        body.put("startY", request.startLatitude());
        body.put("endX", request.goalLongitude());
        body.put("endY", request.goalLatitude());
        body.put("startName", encoded("현재 위치"));
        body.put("endName", encoded("목적지"));
        body.put("reqCoordType", COORDINATE_TYPE);
        body.put("resCoordType", COORDINATE_TYPE);
        body.put("searchOption", SEARCH_OPTION_RECOMMENDED);
        if (mode == DirectionsMode.DRIVING) {
            body.put("trafficInfo", "N");
        } else {
            body.put("sort", "index");
        }
        return body;
    }

    private DirectionsResponse parseResponse(JsonNode response) {
        if (response == null || !response.path("features").isArray()) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }

        int distanceMeters = -1;
        long durationSeconds = -1;
        int tollFare = 0;
        int taxiFare = 0;
        List<DirectionsResponse.Coordinate> path = new ArrayList<>();

        for (JsonNode feature : response.path("features")) {
            JsonNode featureProperties = feature.path("properties");
            if (distanceMeters < 0 && featureProperties.has("totalDistance")) {
                distanceMeters = featureProperties.path("totalDistance").asInt(-1);
                durationSeconds = featureProperties.path("totalTime").asLong(-1);
                tollFare = featureProperties.path("totalFare").asInt(0);
                taxiFare = featureProperties.path("taxiFare").asInt(0);
            }

            JsonNode geometry = feature.path("geometry");
            if (!"LineString".equals(geometry.path("type").asText())) {
                continue;
            }
            appendLineString(path, geometry.path("coordinates"));
        }

        if (distanceMeters < 0 || durationSeconds < 0 || path.size() < 2) {
            throw new BusinessException(ErrorCode.MAP_ROUTE_NOT_FOUND);
        }

        return new DirectionsResponse(
                distanceMeters,
                durationSeconds * 1000,
                tollFare,
                taxiFare,
                0,
                path
        );
    }

    private void appendLineString(
            List<DirectionsResponse.Coordinate> path,
            JsonNode coordinates
    ) {
        if (!coordinates.isArray()) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }
        for (JsonNode point : coordinates) {
            if (!point.isArray() || point.size() < 2) {
                throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
            }
            double longitude = point.get(0).asDouble(Double.NaN);
            double latitude = point.get(1).asDouble(Double.NaN);
            if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
                throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
            }

            DirectionsResponse.Coordinate coordinate =
                    new DirectionsResponse.Coordinate(latitude, longitude);
            if (path.isEmpty() || !sameCoordinate(path.get(path.size() - 1), coordinate)) {
                path.add(coordinate);
            }
        }
    }

    private boolean sameCoordinate(
            DirectionsResponse.Coordinate left,
            DirectionsResponse.Coordinate right
    ) {
        return Double.compare(left.latitude(), right.latitude()) == 0
                && Double.compare(left.longitude(), right.longitude()) == 0;
    }

    private String encoded(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private BusinessException providerError(HttpStatusCode status) {
        if (status.value() == 401
                || status.value() == 403
                || status.value() == 429
                || status.is5xxServerError()) {
            return new BusinessException(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
        }
        return new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
    }
}
