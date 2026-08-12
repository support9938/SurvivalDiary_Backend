package com.survivaldiary.domain.map.dto;

import java.util.List;

public record PublicParkingProviderPage(
        int page,
        int pageSize,
        int totalCount,
        List<Item> items
) {

    public record Item(
            String parkingNumber,
            String name,
            String parkingDivision,
            String parkingType,
            String roadAddress,
            String lotAddress,
            Integer capacity,
            String operationDays,
            String weekdayOpenTime,
            String weekdayCloseTime,
            String saturdayOpenTime,
            String saturdayCloseTime,
            String holidayOpenTime,
            String holidayCloseTime,
            String chargeType,
            Integer basicMinutes,
            Integer basicFee,
            Integer additionalMinutes,
            Integer additionalFee,
            Integer dailyFee,
            Integer monthlyFee,
            String paymentMethods,
            String notes,
            String institution,
            String phone,
            Double latitude,
            Double longitude,
            String accessibleParking,
            String referenceDate,
            String institutionCode
    ) {
    }
}
