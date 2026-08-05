package com.survivaldiary.domain.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HousingRentDealResponse(
        String id,
        @Schema(example = "오피스텔") String propertyType,
        String propertyName,
        @Schema(example = "월세") String dealType,
        @Schema(description = "보증금(만원)") int depositTenThousandWon,
        @Schema(description = "월세(만원)") int monthlyRentTenThousandWon,
        LocalDate contractDate,
        @Schema(description = "전용면적(㎡)") BigDecimal areaSquareMeters,
        Integer floor,
        String neighborhood,
        String lotNumber,
        Integer buildYear,
        String contractTerm,
        String contractType,
        Integer previousDepositTenThousandWon,
        Integer previousMonthlyRentTenThousandWon,
        String renewalRequestRightUsed,
        String address,
        Double latitude,
        Double longitude,
        @Schema(description = "지오코딩 위치 정확도", example = "지번")
        String locationAccuracy
) {
    public HousingRentDealResponse withLocation(
            String address,
            Double latitude,
            Double longitude,
            String locationAccuracy
    ) {
        return new HousingRentDealResponse(
                id,
                propertyType,
                propertyName,
                dealType,
                depositTenThousandWon,
                monthlyRentTenThousandWon,
                contractDate,
                areaSquareMeters,
                floor,
                neighborhood,
                lotNumber,
                buildYear,
                contractTerm,
                contractType,
                previousDepositTenThousandWon,
                previousMonthlyRentTenThousandWon,
                renewalRequestRightUsed,
                address,
                latitude,
                longitude,
                locationAccuracy
        );
    }
}
