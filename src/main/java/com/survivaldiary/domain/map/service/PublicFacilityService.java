package com.survivaldiary.domain.map.service;

import com.survivaldiary.domain.map.client.PublicFacilityClient;
import com.survivaldiary.domain.map.client.PublicFacilityProperties;
import com.survivaldiary.domain.map.dto.PublicFacilityProviderPage;
import com.survivaldiary.domain.map.dto.PublicFacilityResponse;
import com.survivaldiary.global.common.PageResponse;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PublicFacilityService {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final PublicFacilityClient publicFacilityClient;
    private final PublicFacilityProperties properties;
    private volatile Snapshot snapshot = Snapshot.empty();

    public PublicFacilityService(
            PublicFacilityClient publicFacilityClient,
            PublicFacilityProperties properties
    ) {
        this.publicFacilityClient = publicFacilityClient;
        this.properties = properties;
    }

    public PageResponse<PublicFacilityResponse.Facility> findFacilities(
            int page,
            int size,
            Double southWestLat,
            Double southWestLng,
            Double northEastLat,
            Double northEastLng,
            Double latitude,
            Double longitude,
            String category,
            boolean freeOnly,
            String sort
    ) {
        validate(
                page,
                size,
                southWestLat,
                southWestLng,
                northEastLat,
                northEastLng,
                latitude,
                longitude,
                sort
        );

        String normalizedCategory = normalize(category);
        String normalizedSort = normalize(sort);
        List<PublicFacilityResponse.Facility> filtered = facilities().stream()
                .filter(facility -> isInsideBounds(
                        facility,
                        southWestLat,
                        southWestLng,
                        northEastLat,
                        northEastLng
                ))
                .filter(facility -> normalizedCategory == null
                        || normalizedCategory.equalsIgnoreCase(facility.category()))
                .filter(facility -> !freeOnly || Boolean.FALSE.equals(facility.paid()))
                .map(facility -> facility.withDistance(distanceMeters(
                        latitude,
                        longitude,
                        facility.latitude(),
                        facility.longitude()
                )))
                .sorted(comparator(normalizedSort))
                .toList();

        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        long totalElements = filtered.size();
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(
                List.copyOf(filtered.subList(fromIndex, toIndex)),
                page,
                size,
                totalElements,
                totalPages,
                page + 1 < totalPages
        );
    }

    private List<PublicFacilityResponse.Facility> facilities() {
        Snapshot current = snapshot;
        if (current.isFresh(properties.getCacheTtl())) {
            return current.facilities();
        }
        synchronized (this) {
            current = snapshot;
            if (current.isFresh(properties.getCacheTtl())) {
                return current.facilities();
            }
            try {
                List<PublicFacilityResponse.Facility> refreshed = loadAllFacilities();
                snapshot = new Snapshot(refreshed, Instant.now());
                return refreshed;
            } catch (BusinessException exception) {
                if (!current.facilities().isEmpty()) {
                    return current.facilities();
                }
                throw exception;
            }
        }
    }

    private List<PublicFacilityResponse.Facility> loadAllFacilities() {
        int providerPageSize = properties.getPageSize();
        if (providerPageSize < 1 || providerPageSize > 1000) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }

        PublicFacilityProviderPage firstPage =
                publicFacilityClient.fetchPage(1, providerPageSize);
        List<PublicFacilityProviderPage.Item> sourceItems =
                new ArrayList<>(firstPage.items());
        int pageCount = firstPage.totalCount() == 0
                ? 1
                : (int) Math.ceil((double) firstPage.totalCount() / providerPageSize);
        for (int page = 2; page <= pageCount; page++) {
            sourceItems.addAll(
                    publicFacilityClient.fetchPage(page, providerPageSize).items()
            );
        }

        LinkedHashMap<String, PublicFacilityResponse.Facility> unique =
                new LinkedHashMap<>();
        sourceItems.stream()
                .map(this::toFacility)
                .filter(facility -> facility.latitude() != null
                        && facility.longitude() != null)
                .forEach(facility -> unique.putIfAbsent(facility.id(), facility));
        return List.copyOf(unique.values());
    }

    private PublicFacilityResponse.Facility toFacility(
            PublicFacilityProviderPage.Item item
    ) {
        String name = firstNonBlank(item.facilityName(), item.locationName(), "공공시설");
        String locationName = firstNonBlank(item.locationName(), name);
        String address = firstNonBlank(item.roadAddress(), item.lotAddress(), "");
        Boolean paid = paidValue(item.paidUse());
        String idSource = String.join(
                "|",
                nullToEmpty(item.institutionCode()),
                name,
                locationName,
                address,
                String.valueOf(item.latitude()),
                String.valueOf(item.longitude())
        );
        String id = UUID.nameUUIDFromBytes(
                idSource.getBytes(StandardCharsets.UTF_8)
        ).toString();
        return new PublicFacilityResponse.Facility(
                id,
                name,
                locationName,
                firstNonBlank(item.category(), "기타"),
                address,
                nullToEmpty(item.phone()),
                validLatitude(item.latitude()),
                validLongitude(item.longitude()),
                null,
                paid,
                feeLabel(paid, item.fee()),
                hours(item.weekdayOpenTime(), item.weekdayCloseTime()),
                hours(item.weekendOpenTime(), item.weekendCloseTime()),
                nullToEmpty(item.closedDays()),
                nullToEmpty(item.institution()),
                nullToEmpty(item.department()),
                nullToEmpty(item.homepageUrl()),
                nullToEmpty(item.imageUrl()),
                nullToEmpty(item.capacity()),
                nullToEmpty(item.area()),
                nullToEmpty(item.amenities()),
                nullToEmpty(item.applicationMethod()),
                nullToEmpty(item.referenceDate())
        );
    }

    private void validate(
            int page,
            int size,
            Double southWestLat,
            Double southWestLng,
            Double northEastLat,
            Double northEastLng,
            Double latitude,
            Double longitude,
            String sort
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
        boolean hasAnyBounds = southWestLat != null || southWestLng != null
                || northEastLat != null || northEastLng != null;
        boolean hasAllBounds = southWestLat != null && southWestLng != null
                && northEastLat != null && northEastLng != null;
        if (hasAnyBounds && (!hasAllBounds
                || !validCoordinates(southWestLat, southWestLng)
                || !validCoordinates(northEastLat, northEastLng)
                || southWestLat > northEastLat
                || southWestLng > northEastLng)) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
        if ((latitude == null) != (longitude == null)
                || (latitude != null && !validCoordinates(latitude, longitude))) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
        String normalizedSort = normalize(sort);
        if (normalizedSort != null
                && !List.of("distance", "name", "free").contains(normalizedSort)) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
        if ("distance".equals(normalizedSort) && latitude == null) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
    }

    private static Comparator<PublicFacilityResponse.Facility> comparator(String sort) {
        Comparator<PublicFacilityResponse.Facility> byName = Comparator.comparing(
                PublicFacilityResponse.Facility::name,
                Comparator.nullsLast(String::compareTo)
        );
        if ("free".equals(sort)) {
            return Comparator
                    .comparingInt(PublicFacilityService::paidOrder)
                    .thenComparing(
                            PublicFacilityResponse.Facility::distanceMeters,
                            Comparator.nullsLast(Integer::compareTo)
                    )
                    .thenComparing(byName);
        }
        if ("distance".equals(sort)) {
            return Comparator
                    .comparing(
                            PublicFacilityResponse.Facility::distanceMeters,
                            Comparator.nullsLast(Integer::compareTo)
                    )
                    .thenComparing(byName);
        }
        return byName;
    }

    private static int paidOrder(PublicFacilityResponse.Facility facility) {
        if (Boolean.FALSE.equals(facility.paid())) {
            return 0;
        }
        return facility.paid() == null ? 1 : 2;
    }

    private static boolean isInsideBounds(
            PublicFacilityResponse.Facility facility,
            Double southWestLat,
            Double southWestLng,
            Double northEastLat,
            Double northEastLng
    ) {
        if (southWestLat == null) {
            return true;
        }
        return facility.latitude() >= southWestLat
                && facility.latitude() <= northEastLat
                && facility.longitude() >= southWestLng
                && facility.longitude() <= northEastLng;
    }

    private static Integer distanceMeters(
            Double latitude,
            Double longitude,
            Double targetLatitude,
            Double targetLongitude
    ) {
        if (latitude == null || longitude == null
                || targetLatitude == null || targetLongitude == null) {
            return null;
        }
        double latitudeDelta = Math.toRadians(targetLatitude - latitude);
        double longitudeDelta = Math.toRadians(targetLongitude - longitude);
        double startLatitude = Math.toRadians(latitude);
        double endLatitude = Math.toRadians(targetLatitude);
        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(startLatitude) * Math.cos(endLatitude)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return (int) Math.round(
                EARTH_RADIUS_METERS * 2
                        * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine))
        );
    }

    private static Boolean paidValue(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if ("Y".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("N".equalsIgnoreCase(normalized)) {
            return false;
        }
        return null;
    }

    private static String feeLabel(Boolean paid, String fee) {
        if (Boolean.FALSE.equals(paid)) {
            return "무료";
        }
        if (fee != null && !fee.isBlank()) {
            return fee.trim();
        }
        return Boolean.TRUE.equals(paid) ? "요금 정보 확인" : "요금 정보 없음";
    }

    private static String hours(String open, String close) {
        String normalizedOpen = normalize(open);
        String normalizedClose = normalize(close);
        if (normalizedOpen == null && normalizedClose == null) {
            return "";
        }
        return firstNonBlank(normalizedOpen, "?") + "~"
                + firstNonBlank(normalizedClose, "?");
    }

    private static Double validLatitude(Double value) {
        return value != null && value >= -90 && value <= 90 ? value : null;
    }

    private static Double validLongitude(Double value) {
        return value != null && value >= -180 && value <= 180 ? value : null;
    }

    private static boolean validCoordinates(Double latitude, Double longitude) {
        return validLatitude(latitude) != null && validLongitude(longitude) != null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private record Snapshot(
            List<PublicFacilityResponse.Facility> facilities,
            Instant refreshedAt
    ) {
        private static Snapshot empty() {
            return new Snapshot(List.of(), Instant.EPOCH);
        }

        private boolean isFresh(java.time.Duration ttl) {
            return !facilities.isEmpty() && Instant.now().isBefore(refreshedAt.plus(ttl));
        }
    }
}
