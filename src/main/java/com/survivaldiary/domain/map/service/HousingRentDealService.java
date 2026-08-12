package com.survivaldiary.domain.map.service;

import com.survivaldiary.domain.map.client.RealEstateRentClient;
import com.survivaldiary.domain.map.client.NaverGeocodingClient;
import com.survivaldiary.domain.map.dto.HousingRentDealRequest;
import com.survivaldiary.domain.map.dto.HousingRentDealResponse;
import com.survivaldiary.domain.map.dto.MapViewportBounds;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class HousingRentDealService {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final RealEstateRentClient realEstateRentClient;
    private final NaverGeocodingClient naverGeocodingClient;

    public HousingRentDealService(
            RealEstateRentClient realEstateRentClient,
            NaverGeocodingClient naverGeocodingClient
    ) {
        this.realEstateRentClient = realEstateRentClient;
        this.naverGeocodingClient = naverGeocodingClient;
    }

    public List<HousingRentDealResponse> findDeals(HousingRentDealRequest request) {
        return findDeals(request, MapViewportBounds.empty());
    }

    public List<HousingRentDealResponse> findDeals(
            HousingRentDealRequest request,
            MapViewportBounds bounds
    ) {
        MapViewportBounds requestedBounds = bounds == null
                ? MapViewportBounds.empty()
                : bounds;
        if (!requestedBounds.isValid()) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
        YearMonth endMonth = validateAndParse(request);
        List<HousingRentDealResponse> deals = new ArrayList<>();
        for (int offset = 0; offset < request.requestedMonths(); offset++) {
            String dealYmd = endMonth.minusMonths(offset).format(YEAR_MONTH);
            deals.addAll(realEstateRentClient.fetchSingleFamilyDeals(request.lawdCd(), dealYmd));
            deals.addAll(realEstateRentClient.fetchOfficetelDeals(request.lawdCd(), dealYmd));
        }

        String neighborhood = normalize(request.neighborhood());
        List<HousingRentDealResponse> selectedDeals = deals.stream()
                .filter(deal -> neighborhood == null
                        || neighborhood.equals(normalize(deal.neighborhood())))
                .sorted(Comparator.comparing(
                        HousingRentDealResponse::contractDate,
                        Comparator.reverseOrder()
                ))
                .toList();
        if (!requestedBounds.isSpecified()) {
            return selectedDeals.stream()
                    .limit(request.requestedLimit())
                    .parallel()
                    .map(deal -> addLocation(deal, request.region()))
                    .toList();
        }
        return selectedDeals.parallelStream()
                .map(deal -> addLocation(deal, request.region()))
                .filter(deal -> requestedBounds.contains(
                        deal.latitude(),
                        deal.longitude()
                ))
                .limit(request.requestedLimit())
                .toList();
    }

    private HousingRentDealResponse addLocation(
            HousingRentDealResponse deal,
            String region
    ) {
        String normalizedRegion = normalize(region);
        String neighborhood = normalize(deal.neighborhood());
        String address = normalizedRegion;
        if (neighborhood != null
                && (address == null || !address.endsWith(neighborhood))) {
            address = (address == null ? "" : address + " ") + neighborhood;
        }
        String lotNumber = normalize(deal.lotNumber());
        String accuracy = "동 단위";
        if (lotNumber != null) {
            address = (address == null ? "" : address + " ") + lotNumber;
            accuracy = "지번";
        }
        if (address == null || address.isBlank()) {
            return deal.withLocation("", null, null, "정보 없음");
        }
        String finalAddress = address.trim();
        String finalAccuracy = accuracy;
        return naverGeocodingClient.findCoordinates(finalAddress)
                .map(coordinates -> deal.withLocation(
                        finalAddress,
                        coordinates.latitude(),
                        coordinates.longitude(),
                        finalAccuracy
                ))
                .orElseGet(() -> deal.withLocation(
                        finalAddress,
                        null,
                        null,
                        finalAccuracy
                ));
    }

    private YearMonth validateAndParse(HousingRentDealRequest request) {
        if (request == null
                || request.lawdCd() == null
                || !request.lawdCd().matches("^[0-9]{5}$")
                || request.dealYmd() == null
                || !request.dealYmd().matches("^[0-9]{6}$")
                || request.requestedMonths() < 1
                || request.requestedMonths() > 12
                || request.requestedLimit() < 1
                || request.requestedLimit() > 100
                || (request.region() != null && request.region().length() > 100)) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
        try {
            return YearMonth.parse(request.dealYmd(), YEAR_MONTH);
        } catch (DateTimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
