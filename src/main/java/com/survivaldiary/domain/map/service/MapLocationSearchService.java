package com.survivaldiary.domain.map.service;

import com.survivaldiary.domain.map.client.NaverGeocodingClient;
import com.survivaldiary.domain.map.client.NaverPlaceSearchClient;
import com.survivaldiary.domain.map.dto.MapLocationSearchResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MapLocationSearchService {

    private static final int MAX_RESULTS = 5;

    private final NaverPlaceSearchClient placeSearchClient;
    private final NaverGeocodingClient geocodingClient;

    public MapLocationSearchService(
            NaverPlaceSearchClient placeSearchClient,
            NaverGeocodingClient geocodingClient
    ) {
        this.placeSearchClient = placeSearchClient;
        this.geocodingClient = geocodingClient;
    }

    public List<MapLocationSearchResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalizedQuery = query.trim();
        List<MapLocationSearchResponse> places = placeSearchClient.search(normalizedQuery, MAX_RESULTS)
                .stream()
                .map(place -> new MapLocationSearchResponse(
                        place.name().isBlank() ? normalizedQuery : place.name(),
                        place.address(),
                        place.latitude(),
                        place.longitude()
                ))
                .toList();
        if (!places.isEmpty()) {
            return places;
        }
        return geocodingClient.findCoordinates(normalizedQuery)
                .map(coordinates -> List.of(new MapLocationSearchResponse(
                        normalizedQuery,
                        normalizedQuery,
                        coordinates.latitude(),
                        coordinates.longitude()
                )))
                .orElseGet(List::of);
    }
}
