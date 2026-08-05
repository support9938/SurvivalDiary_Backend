package com.survivaldiary.domain.map.controller;

import com.survivaldiary.domain.map.dto.PublicFacilityResponse;
import com.survivaldiary.domain.map.service.PublicFacilityService;
import com.survivaldiary.global.common.ApiResponse;
import com.survivaldiary.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Map", description = "절약 지도 장소 조회")
@RestController
@RequestMapping("/api/map/public-facilities")
public class PublicFacilityController {

    private final PublicFacilityService publicFacilityService;

    public PublicFacilityController(PublicFacilityService publicFacilityService) {
        this.publicFacilityService = publicFacilityService;
    }

    @Operation(summary = "현재 지도 영역의 공공시설 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PublicFacilityResponse.Facility>>>
    findFacilities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Double southWestLat,
            @RequestParam(required = false) Double southWestLng,
            @RequestParam(required = false) Double northEastLat,
            @RequestParam(required = false) Double northEastLng,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean freeOnly,
            @RequestParam(defaultValue = "distance") String sort
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(publicFacilityService.findFacilities(
                        page,
                        size,
                        southWestLat,
                        southWestLng,
                        northEastLat,
                        northEastLng,
                        latitude,
                        longitude,
                        category,
                        freeOnly,
                        sort
                )));
    }
}
