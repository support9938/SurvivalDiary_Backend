package com.survivaldiary.domain.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MapLocationSearchResponse(
        @Schema(description = "Place or address name") String name,
        @Schema(description = "Resolved address") String address,
        @Schema(description = "Latitude") double latitude,
        @Schema(description = "Longitude") double longitude
) {
}
