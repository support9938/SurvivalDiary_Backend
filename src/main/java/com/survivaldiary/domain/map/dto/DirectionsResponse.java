package com.survivaldiary.domain.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record DirectionsResponse(
        @Schema(description = "전체 경로 거리(m)", example = "1250")
        int distanceMeters,

        @Schema(description = "예상 소요 시간(ms)", example = "240000")
        long durationMillis,

        @Schema(description = "통행 요금(원), 도보 경로는 0", example = "0")
        int tollFare,

        @Schema(description = "예상 택시 요금(원), 도보 경로는 0", example = "0")
        int taxiFare,

        @Schema(description = "예상 유류비(원), 도보 경로는 0", example = "0")
        int fuelPrice,

        @Schema(description = "지도에 표시할 경로 좌표")
        List<Coordinate> path
) {
    public DirectionsResponse {
        path = List.copyOf(path);
    }

    public record Coordinate(
            @Schema(description = "위도", example = "35.1578")
            double latitude,

            @Schema(description = "경도", example = "129.0592")
            double longitude
    ) {
    }
}
