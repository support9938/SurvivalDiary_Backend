package com.survivaldiary.domain.map.dto;

public final class PublicParkingResponse {

    private PublicParkingResponse() {
    }

    public record ParkingLot(
            String id,
            String name,
            String parkingType,
            String address,
            String phone,
            Double latitude,
            Double longitude,
            Integer distanceMeters,
            boolean free,
            Integer capacity,
            String operationDays,
            String weekdayHours,
            String saturdayHours,
            String holidayHours,
            Integer basicMinutes,
            Integer basicFee,
            Integer additionalMinutes,
            Integer additionalFee,
            Integer dailyFee,
            Integer monthlyFee,
            String paymentMethods,
            String notes,
            String institution,
            boolean accessibleParking,
            String referenceDate
    ) {
        public ParkingLot withDistance(Integer distanceMeters) {
            return new ParkingLot(
                    id,
                    name,
                    parkingType,
                    address,
                    phone,
                    latitude,
                    longitude,
                    distanceMeters,
                    free,
                    capacity,
                    operationDays,
                    weekdayHours,
                    saturdayHours,
                    holidayHours,
                    basicMinutes,
                    basicFee,
                    additionalMinutes,
                    additionalFee,
                    dailyFee,
                    monthlyFee,
                    paymentMethods,
                    notes,
                    institution,
                    accessibleParking,
                    referenceDate
            );
        }
    }
}
