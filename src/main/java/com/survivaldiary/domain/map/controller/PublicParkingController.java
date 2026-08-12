package com.survivaldiary.domain.map.controller;

import com.survivaldiary.domain.map.dto.PublicParkingResponse;
import com.survivaldiary.domain.map.service.PublicParkingService;
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
@RequestMapping("/api/map/public-parking")
public class PublicParkingController {

    private final PublicParkingService publicParkingService;

    public PublicParkingController(PublicParkingService publicParkingService) {
        this.publicParkingService = publicParkingService;
    }

    @Operation(summary = "현재 지도 영역의 공영주차장 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PublicParkingResponse.ParkingLot>>>
    findParkingLots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Double southWestLat,
            @RequestParam(required = false) Double southWestLng,
            @RequestParam(required = false) Double northEastLat,
            @RequestParam(required = false) Double northEastLng,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "false") boolean freeOnly,
            @RequestParam(defaultValue = "distance") String sort
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(publicParkingService.findParkingLots(
                        page,
                        size,
                        southWestLat,
                        southWestLng,
                        northEastLat,
                        northEastLng,
                        latitude,
                        longitude,
                        freeOnly,
                        sort
                )));
    }
}
