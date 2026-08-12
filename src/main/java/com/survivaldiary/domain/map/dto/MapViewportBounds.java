package com.survivaldiary.domain.map.dto;

public record MapViewportBounds(
        Double southWestLat,
        Double southWestLng,
        Double northEastLat,
        Double northEastLng
) {

    public static MapViewportBounds empty() {
        return new MapViewportBounds(null, null, null, null);
    }

    public boolean isSpecified() {
        return southWestLat != null || southWestLng != null
                || northEastLat != null || northEastLng != null;
    }

    public boolean isValid() {
        if (!isSpecified()) {
            return true;
        }
        return validLatitude(southWestLat)
                && validLongitude(southWestLng)
                && validLatitude(northEastLat)
                && validLongitude(northEastLng)
                && southWestLat <= northEastLat
                && southWestLng <= northEastLng;
    }

    public boolean contains(Double latitude, Double longitude) {
        if (!isSpecified()) {
            return true;
        }
        return latitude != null
                && longitude != null
                && latitude >= southWestLat
                && latitude <= northEastLat
                && longitude >= southWestLng
                && longitude <= northEastLng;
    }

    private static boolean validLatitude(Double value) {
        return value != null
                && Double.isFinite(value)
                && value >= -90
                && value <= 90;
    }

    private static boolean validLongitude(Double value) {
        return value != null
                && Double.isFinite(value)
                && value >= -180
                && value <= 180;
    }
}
