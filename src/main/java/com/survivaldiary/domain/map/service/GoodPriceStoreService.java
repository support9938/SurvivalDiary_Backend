package com.survivaldiary.domain.map.service;

import com.survivaldiary.domain.map.client.GoodPriceStoreClient;
import com.survivaldiary.domain.map.client.NaverGeocodingClient;
import com.survivaldiary.domain.map.dto.GoodPriceStoreResponse;
import com.survivaldiary.global.common.PageResponse;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class GoodPriceStoreService {

    private final GoodPriceStoreClient goodPriceStoreClient;
    private final NaverGeocodingClient naverGeocodingClient;

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
        validate(page, size, province, district, sort);

        String normalizedProvince = normalize(province);
        String normalizedDistrict = normalize(district);
        String normalizedSort = normalize(sort);
        GoodPriceStoreResponse providerResponse = goodPriceStoreClient.fetchStores(
                page + 1,
                size,
                normalizedProvince,
                normalizedDistrict
        );

        List<GoodPriceStoreResponse.Store> stores = providerResponse.data().stream()
                .map(this::addCoordinates)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if ("name".equals(normalizedSort)) {
            stores.sort(Comparator.comparing(
                    GoodPriceStoreResponse.Store::name,
                    Comparator.nullsLast(String::compareTo)
            ));
        } else if ("price".equals(normalizedSort)) {
            stores.sort(Comparator
                    .comparingInt(GoodPriceStoreService::lowestPrice)
                    .thenComparing(
                            GoodPriceStoreResponse.Store::name,
                            Comparator.nullsLast(String::compareTo)
                    ));
        }

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

    private void validate(
            int page,
            int size,
            String province,
            String district,
            String sort
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
        if (normalizedSort != null
                && !List.of("default", "name", "price").contains(normalizedSort)) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
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
}
