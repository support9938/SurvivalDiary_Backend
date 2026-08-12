package com.survivaldiary.domain.map.controller;

import com.survivaldiary.domain.map.dto.MapLocationSearchResponse;
import com.survivaldiary.domain.map.service.MapLocationSearchService;
import com.survivaldiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Map", description = "Map location search")
@RestController
@RequestMapping("/api/map/location-search")
public class MapLocationSearchController {

    private final MapLocationSearchService mapLocationSearchService;

    public MapLocationSearchController(MapLocationSearchService mapLocationSearchService) {
        this.mapLocationSearchService = mapLocationSearchService;
    }

    @Operation(summary = "Search a station, place, road name, or address")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MapLocationSearchResponse>>> search(
            @RequestParam String query
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(mapLocationSearchService.search(query)));
    }
}
