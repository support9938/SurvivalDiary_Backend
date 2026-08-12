package com.survivaldiary.domain.map.controller;

import com.survivaldiary.domain.map.dto.GoodPriceStoreResponse;
import com.survivaldiary.domain.map.dto.MapViewportBounds;
import com.survivaldiary.domain.map.service.GoodPriceStoreService;
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
@RequestMapping("/api/map/good-price-stores")
public class GoodPriceStoreController {

    private final GoodPriceStoreService goodPriceStoreService;

    public GoodPriceStoreController(GoodPriceStoreService goodPriceStoreService) {
        this.goodPriceStoreService = goodPriceStoreService;
    }

    @Operation(summary = "착한가격업소 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<GoodPriceStoreResponse.Store>>> findStores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String district,
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(required = false) Double southWestLat,
            @RequestParam(required = false) Double southWestLng,
            @RequestParam(required = false) Double northEastLat,
            @RequestParam(required = false) Double northEastLng
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(
                        goodPriceStoreService.findStores(
                                page,
                                size,
                                province,
                                district,
                                sort,
                                new MapViewportBounds(
                                        southWestLat,
                                        southWestLng,
                                        northEastLat,
                                        northEastLng
                                )
                        )
                ));
    }
}
