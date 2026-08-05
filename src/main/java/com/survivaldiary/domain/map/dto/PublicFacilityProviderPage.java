package com.survivaldiary.domain.map.dto;

import java.util.List;

public record PublicFacilityProviderPage(
        int page,
        int pageSize,
        int totalCount,
        List<Item> items
) {

    public record Item(
            String facilityName,
            String locationName,
            String category,
            String closedDays,
            String weekdayOpenTime,
            String weekdayCloseTime,
            String weekendOpenTime,
            String weekendCloseTime,
            String paidUse,
            String standardUseTime,
            String fee,
            String excessUseUnitTime,
            String excessFee,
            String capacity,
            String area,
            String amenities,
            String applicationMethod,
            String imageUrl,
            String roadAddress,
            String lotAddress,
            String institution,
            String department,
            String phone,
            String homepageUrl,
            Double latitude,
            Double longitude,
            String referenceDate,
            String institutionCode
    ) {
    }
}
