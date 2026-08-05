package com.survivaldiary.domain.map.dto;

public final class PublicFacilityResponse {

    private PublicFacilityResponse() {
    }

    public record Facility(
            String id,
            String name,
            String locationName,
            String category,
            String address,
            String phone,
            Double latitude,
            Double longitude,
            Integer distanceMeters,
            Boolean paid,
            String fee,
            String weekdayHours,
            String weekendHours,
            String closedDays,
            String institution,
            String department,
            String homepageUrl,
            String imageUrl,
            String capacity,
            String area,
            String amenities,
            String applicationMethod,
            String referenceDate
    ) {
        public Facility withDistance(Integer distanceMeters) {
            return new Facility(
                    id,
                    name,
                    locationName,
                    category,
                    address,
                    phone,
                    latitude,
                    longitude,
                    distanceMeters,
                    paid,
                    fee,
                    weekdayHours,
                    weekendHours,
                    closedDays,
                    institution,
                    department,
                    homepageUrl,
                    imageUrl,
                    capacity,
                    area,
                    amenities,
                    applicationMethod,
                    referenceDate
            );
        }
    }
}
