package com.survivaldiary.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDefaultResidenceRequest(
        @Schema(description = "Address selected from map search")
        @NotBlank
        @Size(max = 255)
        String address,

        @Schema(description = "Latitude")
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        double latitude,

        @Schema(description = "Longitude")
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        double longitude
) {
}
