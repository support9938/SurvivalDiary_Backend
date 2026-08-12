package com.survivaldiary.domain.map.controller;

import com.survivaldiary.domain.map.dto.DirectionsRequest;
import com.survivaldiary.domain.map.dto.DirectionsResponse;
import com.survivaldiary.domain.map.service.DirectionsService;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Map", description = "절약 지도 장소 조회")
@RestController
@RequestMapping("/api/map/directions")
public class DirectionsController {

    private final DirectionsService directionsService;

    public DirectionsController(DirectionsService directionsService) {
        this.directionsService = directionsService;
    }

    @Operation(summary = "현재 위치에서 목적지까지 이동 수단별 추천 경로 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<DirectionsResponse>> findOptimalRoute(
            @ParameterObject @Valid @ModelAttribute DirectionsRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(directionsService.findOptimalRoute(request)));
    }
}
