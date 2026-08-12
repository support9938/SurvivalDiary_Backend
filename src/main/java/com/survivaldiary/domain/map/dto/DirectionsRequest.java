package com.survivaldiary.domain.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record DirectionsRequest(
        @Schema(description = "출발지 위도", example = "35.1578")
        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double startLatitude,

        @Schema(description = "출발지 경도", example = "129.0592")
        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double startLongitude,

        @Schema(description = "목적지 위도", example = "35.1600")
        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double goalLatitude,

        @Schema(description = "목적지 경도", example = "129.0650")
        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double goalLongitude,

        @Schema(
                description = "이동 수단. 생략하면 도보 경로를 조회합니다.",
                example = "WALKING",
                defaultValue = "WALKING",
                allowableValues = {"WALKING", "DRIVING"}
        )
        DirectionsMode mode
) {
    public DirectionsRequest(
            Double startLatitude,
            Double startLongitude,
            Double goalLatitude,
            Double goalLongitude
    ) {
        this(startLatitude, startLongitude, goalLatitude, goalLongitude, null);
    }

    public DirectionsMode resolvedMode() {
        return mode == null ? DirectionsMode.WALKING : mode;
    }
}
