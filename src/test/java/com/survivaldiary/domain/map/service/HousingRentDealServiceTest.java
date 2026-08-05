package com.survivaldiary.domain.map.service;

import com.survivaldiary.domain.map.client.RealEstateRentClient;
import com.survivaldiary.domain.map.client.NaverGeocodingClient;
import com.survivaldiary.domain.map.dto.HousingRentDealRequest;
import com.survivaldiary.domain.map.dto.HousingRentDealResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;

class HousingRentDealServiceTest {

    @Test
    void 최근_개월의_두_주택유형을_합치고_선택한_동만_남긴다() {
        RealEstateRentClient client = mock(RealEstateRentClient.class);
        NaverGeocodingClient geocodingClient = mock(NaverGeocodingClient.class);
        when(geocodingClient.findCoordinates("서울특별시 강남구 역삼동 123-*"))
                .thenReturn(Optional.of(new NaverGeocodingClient.Coordinates(37.5, 127.0)));
        HousingRentDealService service = new HousingRentDealService(client, geocodingClient);
        when(client.fetchSingleFamilyDeals("11680", "202608"))
                .thenReturn(List.of(deal("1", "단독/다가구", "역삼동", LocalDate.of(2026, 8, 1))));
        when(client.fetchOfficetelDeals("11680", "202608"))
                .thenReturn(List.of(deal("2", "오피스텔", "논현동", LocalDate.of(2026, 8, 2))));
        when(client.fetchSingleFamilyDeals("11680", "202607"))
                .thenReturn(List.of());
        when(client.fetchOfficetelDeals("11680", "202607"))
                .thenReturn(List.of(deal("3", "오피스텔", "역삼동", LocalDate.of(2026, 7, 20))));

        var result = service.findDeals(
                new HousingRentDealRequest(
                        "11680",
                        "202608",
                        2,
                        " 역삼동 ",
                        "서울특별시 강남구 역삼동",
                        100
                )
        );

        assertThat(result).extracting(HousingRentDealResponse::id)
                .containsExactly("1", "3");
        assertThat(result).allSatisfy(deal -> {
            assertThat(deal.latitude()).isEqualTo(37.5);
            assertThat(deal.longitude()).isEqualTo(127.0);
            assertThat(deal.locationAccuracy()).isEqualTo("지번");
        });
        verify(client).fetchSingleFamilyDeals("11680", "202608");
        verify(client).fetchOfficetelDeals("11680", "202608");
        verify(client).fetchSingleFamilyDeals("11680", "202607");
        verify(client).fetchOfficetelDeals("11680", "202607");
    }

    private HousingRentDealResponse deal(
            String id,
            String propertyType,
            String neighborhood,
            LocalDate contractDate
    ) {
        return new HousingRentDealResponse(
                id,
                propertyType,
                propertyType,
                "전세",
                10000,
                0,
                contractDate,
                new BigDecimal("29.8"),
                3,
                neighborhood,
                "123-*",
                2020,
                "2026.08~2028.07",
                "신규",
                null,
                null,
                "",
                "",
                null,
                null,
                ""
        );
    }
}
