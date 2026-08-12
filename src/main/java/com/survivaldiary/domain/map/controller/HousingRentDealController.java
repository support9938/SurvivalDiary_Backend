package com.survivaldiary.domain.map.controller;

import com.survivaldiary.domain.map.dto.HousingRentDealRequest;
import com.survivaldiary.domain.map.dto.HousingRentDealResponse;
import com.survivaldiary.domain.map.dto.MapViewportBounds;
import com.survivaldiary.domain.map.service.HousingRentDealService;
import com.survivaldiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Map", description = "절약 지도 장소 조회")
@RestController
@RequestMapping("/api/map/housing-rent-deals")
public class HousingRentDealController {

    private final HousingRentDealService housingRentDealService;

    public HousingRentDealController(HousingRentDealService housingRentDealService) {
        this.housingRentDealService = housingRentDealService;
    }

    @Operation(summary = "단독·다가구 및 오피스텔 전월세 실거래 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HousingRentDealResponse>>> findDeals(
            @ParameterObject @Valid @ModelAttribute HousingRentDealRequest request,
            @RequestParam(required = false) Double southWestLat,
            @RequestParam(required = false) Double southWestLng,
            @RequestParam(required = false) Double northEastLat,
            @RequestParam(required = false) Double northEastLng
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(housingRentDealService.findDeals(
                        request,
                        new MapViewportBounds(
                                southWestLat,
                                southWestLng,
                                northEastLat,
                                northEastLng
                        )
                )));
    }
}
