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
        Double goalLongitude
) {
}
