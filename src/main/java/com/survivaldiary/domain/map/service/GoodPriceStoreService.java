package com.survivaldiary.domain.map.service;

import com.survivaldiary.domain.map.client.GoodPriceStoreClient;
import com.survivaldiary.domain.map.client.NaverGeocodingClient;
import com.survivaldiary.domain.map.dto.GoodPriceStoreResponse;
import com.survivaldiary.domain.map.dto.MapViewportBounds;
import com.survivaldiary.global.common.PageResponse;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@Service
public class GoodPriceStoreService {

    private static final int PROVIDER_PAGE_SIZE = 1000;
    private static final Duration VIEWPORT_CACHE_TTL = Duration.ofHours(6);

    private final GoodPriceStoreClient goodPriceStoreClient;
    private final NaverGeocodingClient naverGeocodingClient;
    private final ConcurrentMap<String, StoreSnapshot> viewportSnapshots =
            new ConcurrentHashMap<>();

    public GoodPriceStoreService(
            GoodPriceStoreClient goodPriceStoreClient,
            NaverGeocodingClient naverGeocodingClient
    ) {
        this.goodPriceStoreClient = goodPriceStoreClient;
        this.naverGeocodingClient = naverGeocodingClient;
    }

    public PageResponse<GoodPriceStoreResponse.Store> findStores(
            int page,
            int size,
            String province,
            String district,
            String sort
    ) {
        return findStores(
                page,
                size,
                province,
                district,
                sort,
                MapViewportBounds.empty()
        );
    }

    public PageResponse<GoodPriceStoreResponse.Store> findStores(
            int page,
            int size,
            String province,
            String district,
            String sort,
            MapViewportBounds bounds
    ) {
        MapViewportBounds requestedBounds = bounds == null
                ? MapViewportBounds.empty()
                : bounds;
        validate(page, size, province, district, sort, requestedBounds);

        String normalizedProvince = normalize(province);
        String normalizedDistrict = normalize(district);
        String normalizedSort = normalize(sort);
        if (requestedBounds.isSpecified()) {
            return findStoresInViewport(
                    page,
                    size,
                    normalizedProvince,
                    normalizedDistrict,
                    normalizedSort,
                    requestedBounds
            );
        }

        GoodPriceStoreResponse providerResponse = goodPriceStoreClient.fetchStores(
                page + 1,
                size,
                normalizedProvince,
                normalizedDistrict
        );

        List<GoodPriceStoreResponse.Store> stores = providerResponse.data().stream()
                .map(this::addCoordinates)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        sort(stores, normalizedSort);

        long totalElements = providerResponse.matchCount();
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(
                List.copyOf(stores),
                page,
                size,
                totalElements,
                totalPages,
                page + 1 < totalPages
        );
    }

    private PageResponse<GoodPriceStoreResponse.Store> findStoresInViewport(
            int page,
            int size,
            String province,
            String district,
            String sort,
            MapViewportBounds bounds
    ) {
        List<GoodPriceStoreResponse.Store> stores = storesForScope(
                province,
                district
        ).stream()
                .filter(store -> bounds.contains(
                        store.latitude(),
                        store.longitude()
                ))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        sort(stores, sort);

        int fromIndex = Math.min(page * size, stores.size());
        int toIndex = Math.min(fromIndex + size, stores.size());
        long totalElements = stores.size();
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(
                List.copyOf(stores.subList(fromIndex, toIndex)),
                page,
                size,
                totalElements,
                totalPages,
                page + 1 < totalPages
        );
    }

    private List<GoodPriceStoreResponse.Store> storesForScope(
            String province,
            String district
    ) {
        String cacheKey = province + "\u0000" + (district == null ? "" : district);
        StoreSnapshot current = viewportSnapshots.get(cacheKey);
        if (current != null && current.isFresh()) {
            return current.stores();
        }

        synchronized (viewportSnapshots) {
            current = viewportSnapshots.get(cacheKey);
            if (current != null && current.isFresh()) {
                return current.stores();
            }
            try {
                List<GoodPriceStoreResponse.Store> refreshed = loadAllStores(
                        province,
                        district
                );
                viewportSnapshots.put(
                        cacheKey,
                        new StoreSnapshot(refreshed, Instant.now())
                );
                return refreshed;
            } catch (BusinessException exception) {
                if (current != null) {
                    return current.stores();
                }
                throw exception;
            }
        }
    }

    private List<GoodPriceStoreResponse.Store> loadAllStores(
            String province,
            String district
    ) {
        GoodPriceStoreResponse firstPage = goodPriceStoreClient.fetchStores(
                1,
                PROVIDER_PAGE_SIZE,
                province,
                district
        );
        List<GoodPriceStoreResponse.Store> source = new ArrayList<>(firstPage.data());
        int pageCount = firstPage.matchCount() == 0
                ? 1
                : (int) Math.ceil((double) firstPage.matchCount() / PROVIDER_PAGE_SIZE);
        for (int providerPage = 2; providerPage <= pageCount; providerPage++) {
            source.addAll(goodPriceStoreClient.fetchStores(
                    providerPage,
                    PROVIDER_PAGE_SIZE,
                    province,
                    district
            ).data());
        }

        LinkedHashMap<String, GoodPriceStoreResponse.Store> unique =
                new LinkedHashMap<>();
        source.forEach(store -> unique.putIfAbsent(storeKey(store), store));
        return unique.values().parallelStream()
                .map(this::addCoordinates)
                .toList();
    }

    private void validate(
            int page,
            int size,
            String province,
            String district,
            String sort,
            MapViewportBounds bounds
    ) {
        String normalizedProvince = normalize(province);
        String normalizedDistrict = normalize(district);
        String normalizedSort = normalize(sort);
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
        if (normalizedDistrict != null && normalizedProvince == null) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
        if (!bounds.isValid()
                || (bounds.isSpecified() && normalizedProvince == null)) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
        if (normalizedSort != null
                && !List.of("default", "name", "price").contains(normalizedSort)) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
    }

    private static void sort(
            List<GoodPriceStoreResponse.Store> stores,
            String sort
    ) {
        if ("name".equals(sort)) {
            stores.sort(Comparator.comparing(
                    GoodPriceStoreResponse.Store::name,
                    Comparator.nullsLast(String::compareTo)
            ));
        } else if ("price".equals(sort)) {
            stores.sort(Comparator
                    .comparingInt(GoodPriceStoreService::lowestPrice)
                    .thenComparing(
                            GoodPriceStoreResponse.Store::name,
                            Comparator.nullsLast(String::compareTo)
                    ));
        }
    }

    private static String storeKey(GoodPriceStoreResponse.Store store) {
        return String.join(
                "|",
                nullToEmpty(store.province()),
                nullToEmpty(store.district()),
                nullToEmpty(store.name()),
                nullToEmpty(store.address()),
                nullToEmpty(store.phone())
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static int lowestPrice(GoodPriceStoreResponse.Store store) {
        return Stream.of(store.price1(), store.price2(), store.price3(), store.price4())
                .map(GoodPriceStoreService::parsePrice)
                .filter(price -> price > 0)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);
    }

    private static int parsePrice(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private GoodPriceStoreResponse.Store addCoordinates(
            GoodPriceStoreResponse.Store store
    ) {
        return naverGeocodingClient.findCoordinates(store.address())
                .map(coordinates -> store.withCoordinates(
                        coordinates.latitude(),
                        coordinates.longitude()
                ))
                .orElse(store);
    }

    private record StoreSnapshot(
            List<GoodPriceStoreResponse.Store> stores,
            Instant refreshedAt
    ) {
        private boolean isFresh() {
            return Instant.now().isBefore(refreshedAt.plus(VIEWPORT_CACHE_TTL));
        }
    }
}
