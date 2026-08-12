package com.survivaldiary.domain.map.service;

import com.survivaldiary.domain.map.client.PublicParkingClient;
import com.survivaldiary.domain.map.client.PublicParkingProperties;
import com.survivaldiary.domain.map.dto.PublicParkingProviderPage;
import com.survivaldiary.domain.map.dto.PublicParkingResponse;
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
public class PublicParkingService {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final PublicParkingClient publicParkingClient;
    private final PublicParkingProperties properties;
    private volatile Snapshot snapshot = Snapshot.empty();

    public PublicParkingService(
            PublicParkingClient publicParkingClient,
            PublicParkingProperties properties
    ) {
        this.publicParkingClient = publicParkingClient;
        this.properties = properties;
    }

    public PageResponse<PublicParkingResponse.ParkingLot> findParkingLots(
            int page,
            int size,
            Double southWestLat,
            Double southWestLng,
            Double northEastLat,
            Double northEastLng,
            Double latitude,
            Double longitude,
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

        String normalizedSort = normalize(sort);
        List<PublicParkingResponse.ParkingLot> filtered = parkingLots().stream()
                .filter(parkingLot -> isInsideBounds(
                        parkingLot,
                        southWestLat,
                        southWestLng,
                        northEastLat,
                        northEastLng
                ))
                .filter(parkingLot -> !freeOnly || parkingLot.free())
                .map(parkingLot -> parkingLot.withDistance(distanceMeters(
                        latitude,
                        longitude,
                        parkingLot.latitude(),
                        parkingLot.longitude()
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

    private List<PublicParkingResponse.ParkingLot> parkingLots() {
        Snapshot current = snapshot;
        if (current.isFresh(properties.getCacheTtl())) {
            return current.parkingLots();
        }
        synchronized (this) {
            current = snapshot;
            if (current.isFresh(properties.getCacheTtl())) {
                return current.parkingLots();
            }
            try {
                List<PublicParkingResponse.ParkingLot> refreshed = loadAllParkingLots();
                snapshot = new Snapshot(refreshed, Instant.now());
                return refreshed;
            } catch (BusinessException exception) {
                if (!current.parkingLots().isEmpty()) {
                    return current.parkingLots();
                }
                throw exception;
            }
        }
    }

    private List<PublicParkingResponse.ParkingLot> loadAllParkingLots() {
        int providerPageSize = properties.getPageSize();
        if (providerPageSize < 1 || providerPageSize > 1000) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }

        PublicParkingProviderPage firstPage =
                publicParkingClient.fetchPage(1, providerPageSize);
        List<PublicParkingProviderPage.Item> sourceItems =
                new ArrayList<>(firstPage.items());
        int pageCount = firstPage.totalCount() == 0
                ? 1
                : (int) Math.ceil((double) firstPage.totalCount() / providerPageSize);
        for (int providerPage = 2; providerPage <= pageCount; providerPage++) {
            sourceItems.addAll(
                    publicParkingClient.fetchPage(providerPage, providerPageSize).items()
            );
        }

        LinkedHashMap<String, PublicParkingResponse.ParkingLot> unique =
                new LinkedHashMap<>();
        sourceItems.stream()
                .filter(item -> containsPublic(item.parkingDivision()))
                .map(this::toParkingLot)
                .filter(parkingLot -> parkingLot.latitude() != null
                        && parkingLot.longitude() != null)
                .forEach(parkingLot -> unique.putIfAbsent(
                        parkingLot.id(),
                        parkingLot
                ));
        return List.copyOf(unique.values());
    }

    private PublicParkingResponse.ParkingLot toParkingLot(
            PublicParkingProviderPage.Item item
    ) {
        String name = firstNonBlank(item.name(), "공영주차장");
        String address = firstNonBlank(item.roadAddress(), item.lotAddress(), "");
        String idSource = String.join(
                "|",
                nullToEmpty(item.institutionCode()),
                nullToEmpty(item.parkingNumber()),
                name,
                address,
                String.valueOf(item.latitude()),
                String.valueOf(item.longitude())
        );
        String id = UUID.nameUUIDFromBytes(
                idSource.getBytes(StandardCharsets.UTF_8)
        ).toString();
        return new PublicParkingResponse.ParkingLot(
                id,
                name,
                firstNonBlank(item.parkingType(), "공영"),
                address,
                nullToEmpty(item.phone()),
                validLatitude(item.latitude()),
                validLongitude(item.longitude()),
                null,
                isFree(item.chargeType()),
                positiveOrZero(item.capacity()),
                nullToEmpty(item.operationDays()),
                hours(item.weekdayOpenTime(), item.weekdayCloseTime()),
                hours(item.saturdayOpenTime(), item.saturdayCloseTime()),
                hours(item.holidayOpenTime(), item.holidayCloseTime()),
                positiveOrZero(item.basicMinutes()),
                positiveOrZero(item.basicFee()),
                positiveOrZero(item.additionalMinutes()),
                positiveOrZero(item.additionalFee()),
                positiveOrZero(item.dailyFee()),
                positiveOrZero(item.monthlyFee()),
                nullToEmpty(item.paymentMethods()),
                nullToEmpty(item.notes()),
                nullToEmpty(item.institution()),
                isYes(item.accessibleParking()),
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
                && !List.of("distance", "name", "fee").contains(normalizedSort)) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
        if ("distance".equals(normalizedSort) && latitude == null) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
    }

    private static Comparator<PublicParkingResponse.ParkingLot> comparator(
            String sort
    ) {
        Comparator<PublicParkingResponse.ParkingLot> byName = Comparator.comparing(
                PublicParkingResponse.ParkingLot::name,
                Comparator.nullsLast(String::compareTo)
        );
        if ("fee".equals(sort)) {
            return Comparator
                    .comparingInt(PublicParkingService::feeOrder)
                    .thenComparing(
                            PublicParkingResponse.ParkingLot::distanceMeters,
                            Comparator.nullsLast(Integer::compareTo)
                    )
                    .thenComparing(byName);
        }
        if ("distance".equals(sort)) {
            return Comparator
                    .comparing(
                            PublicParkingResponse.ParkingLot::distanceMeters,
                            Comparator.nullsLast(Integer::compareTo)
                    )
                    .thenComparing(byName);
        }
        return byName;
    }

    private static int feeOrder(PublicParkingResponse.ParkingLot parkingLot) {
        if (parkingLot.free()) {
            return 0;
        }
        return parkingLot.basicFee() == null
                ? Integer.MAX_VALUE
                : parkingLot.basicFee();
    }

    private static boolean isInsideBounds(
            PublicParkingResponse.ParkingLot parkingLot,
            Double southWestLat,
            Double southWestLng,
            Double northEastLat,
            Double northEastLng
    ) {
        if (southWestLat == null) {
            return true;
        }
        return parkingLot.latitude() >= southWestLat
                && parkingLot.latitude() <= northEastLat
                && parkingLot.longitude() >= southWestLng
                && parkingLot.longitude() <= northEastLng;
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

    private static boolean containsPublic(String value) {
        String normalized = normalize(value);
        return normalized != null && normalized.contains("공영");
    }

    private static boolean isFree(String value) {
        String normalized = normalize(value);
        return normalized != null
                && normalized.contains("무료")
                && !normalized.contains("유료");
    }

    private static boolean isYes(String value) {
        String normalized = normalize(value);
        return "y".equals(normalized) || "예".equals(normalized)
                || "있음".equals(normalized);
    }

    private static String hours(String open, String close) {
        String normalizedOpen = blankToNull(open);
        String normalizedClose = blankToNull(close);
        if (normalizedOpen == null && normalizedClose == null) {
            return "";
        }
        return firstNonBlank(normalizedOpen, "?") + "~"
                + firstNonBlank(normalizedClose, "?");
    }

    private static Integer positiveOrZero(Integer value) {
        return value != null && value >= 0 ? value : null;
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
            List<PublicParkingResponse.ParkingLot> parkingLots,
            Instant refreshedAt
    ) {
        private static Snapshot empty() {
            return new Snapshot(List.of(), Instant.EPOCH);
        }

        private boolean isFresh(java.time.Duration ttl) {
            return !parkingLots.isEmpty()
                    && Instant.now().isBefore(refreshedAt.plus(ttl));
        }
    }
}
