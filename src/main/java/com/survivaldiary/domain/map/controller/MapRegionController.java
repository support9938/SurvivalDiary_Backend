package com.survivaldiary.domain.map.controller;

import com.survivaldiary.domain.map.client.NaverGeocodingClient;
import com.survivaldiary.domain.map.client.TmapReverseGeocodingClient;
import com.survivaldiary.domain.map.dto.MapRegionResponse;
import com.survivaldiary.global.common.ApiResponse;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/map/region")
public class MapRegionController {

    private static final Map<String, String> DISTRICT_LAWD_CODES = Map.of(
            "부산광역시 연제구", "26470"
    );

    private final NaverGeocodingClient naverGeocodingClient;
    private final TmapReverseGeocodingClient tmapReverseGeocodingClient;

    public MapRegionController(
            NaverGeocodingClient naverGeocodingClient,
            TmapReverseGeocodingClient tmapReverseGeocodingClient
    ) {
        this.naverGeocodingClient = naverGeocodingClient;
        this.tmapReverseGeocodingClient = tmapReverseGeocodingClient;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MapRegionResponse>> findRegion(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) String address
    ) {
        MapRegionResponse response = (address == null || address.isBlank()
                ? findRegionByCoordinates(latitude, longitude)
                : naverGeocodingClient.findRegionByAddress(address)
                        .map(this::toResponse)
                        .or(() -> findRegionByCoordinates(latitude, longitude)))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_MAP_FILTER));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(response));
    }

    private Optional<MapRegionResponse> findRegionByCoordinates(
            double latitude,
            double longitude
    ) {
        return naverGeocodingClient.findRegion(latitude, longitude)
                .map(this::toResponse)
                .or(() -> tmapReverseGeocodingClient.findRegion(latitude, longitude)
                        .map(region -> new MapRegionResponse(
                                region.province(),
                                region.district(),
                                region.lawdCode()
                        )));
    }

    private MapRegionResponse toResponse(NaverGeocodingClient.Region region) {
        return new MapRegionResponse(
                region.province(),
                region.district(),
                region.lawdCode() == null
                        ? DISTRICT_LAWD_CODES.get(region.province() + " " + region.district())
                        : region.lawdCode()
        );
    }
}
