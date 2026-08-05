package com.survivaldiary.domain.map.service;

import com.survivaldiary.domain.map.client.PublicFacilityClient;
import com.survivaldiary.domain.map.client.PublicFacilityProperties;
import com.survivaldiary.domain.map.dto.PublicFacilityProviderPage;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicFacilityServiceTest {

    private PublicFacilityClient client;
    private PublicFacilityService service;

    @BeforeEach
    void setUp() {
        client = mock(PublicFacilityClient.class);
        PublicFacilityProperties properties = new PublicFacilityProperties();
        properties.setPageSize(1000);
        service = new PublicFacilityService(client, properties);
    }

    @Test
    void 지도_영역의_시설만_거리순으로_반환한다() {
        when(client.fetchPage(1, 1000)).thenReturn(page(List.of(
                item("가까운 회의실", "N", 37.5005, 127.0005),
                item("먼 체육관", "Y", 37.5100, 127.0100),
                item("영역 밖 시설", "N", 36.0000, 128.0000)
        )));

        var result = service.findFacilities(
                0,
                20,
                37.49,
                126.99,
                37.52,
                127.02,
                37.50,
                127.00,
                null,
                false,
                "distance"
        );

        assertThat(result.content())
                .extracting(facility -> facility.name())
                .containsExactly("가까운 회의실", "먼 체육관");
        assertThat(result.content().get(0).distanceMeters()).isNotNull();
    }

    @Test
    void 무료_우선_정렬과_무료_필터를_지원한다() {
        when(client.fetchPage(1, 1000)).thenReturn(page(List.of(
                item("유료 시설", "Y", 37.5000, 127.0000),
                item("무료 시설", "N", 37.5100, 127.0100)
        )));

        var result = service.findFacilities(
                0,
                20,
                null,
                null,
                null,
                null,
                37.50,
                127.00,
                null,
                true,
                "free"
        );

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("무료 시설");
        assertThat(result.content().get(0).fee()).isEqualTo("무료");
    }

    @Test
    void 일부_지도_경계만_전달하면_거부한다() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.findFacilities(
                        0,
                        20,
                        37.0,
                        null,
                        null,
                        null,
                        37.5,
                        127.0,
                        null,
                        false,
                        "distance"
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_MAP_FILTER);
    }

    private PublicFacilityProviderPage page(List<PublicFacilityProviderPage.Item> items) {
        return new PublicFacilityProviderPage(1, 1000, items.size(), items);
    }

    private PublicFacilityProviderPage.Item item(
            String name,
            String paid,
            double latitude,
            double longitude
    ) {
        return new PublicFacilityProviderPage.Item(
                name,
                "청년센터",
                "회의실",
                "연중무휴",
                "09:00",
                "18:00",
                "10:00",
                "17:00",
                paid,
                "1",
                "Y".equals(paid) ? "10000" : "",
                "",
                "",
                "20",
                "50",
                "와이파이",
                "온라인",
                "",
                "서울특별시 종로구 세종대로 1",
                "",
                "서울특별시",
                "청년정책과",
                "02-000-0000",
                "https://example.com",
                latitude,
                longitude,
                "2026-08-01",
                "123"
        );
    }
}
