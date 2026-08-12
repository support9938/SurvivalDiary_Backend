package com.survivaldiary.domain.map.controller;

import com.survivaldiary.domain.map.client.NaverGeocodingClient;
import com.survivaldiary.domain.map.client.TmapReverseGeocodingClient;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MapRegionControllerTest {

    private final NaverGeocodingClient naverGeocodingClient =
            mock(NaverGeocodingClient.class);
    private final TmapReverseGeocodingClient tmapReverseGeocodingClient =
            mock(TmapReverseGeocodingClient.class);
    private final MapRegionController controller = new MapRegionController(
            naverGeocodingClient,
            tmapReverseGeocodingClient
    );

    @Test
    void 네이버_역지오코딩이_실패하면_Tmap으로_지역을_확인한다() {
        when(naverGeocodingClient.findRegion(35.1861, 129.0801))
                .thenReturn(Optional.empty());
        when(tmapReverseGeocodingClient.findRegion(35.1861, 129.0801))
                .thenReturn(Optional.of(new TmapReverseGeocodingClient.Region(
                        "부산광역시",
                        "연제구",
                        "26470"
                )));

        var response = controller.findRegion(35.1861, 129.0801, null);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().province()).isEqualTo("부산광역시");
        assertThat(response.getBody().data().district()).isEqualTo("연제구");
        assertThat(response.getBody().data().lawdCode()).isEqualTo("26470");
        verify(tmapReverseGeocodingClient).findRegion(35.1861, 129.0801);
    }

    @Test
    void 주소가_있으면_네이버_정방향_지오코딩을_우선한다() {
        when(naverGeocodingClient.findRegionByAddress("부산광역시 연제구 연산동"))
                .thenReturn(Optional.of(new NaverGeocodingClient.Region(
                        "부산광역시",
                        "연제구",
                        "26470"
                )));

        var response = controller.findRegion(
                35.1861,
                129.0801,
                "부산광역시 연제구 연산동"
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().lawdCode()).isEqualTo("26470");
        verify(naverGeocodingClient, never()).findRegion(35.1861, 129.0801);
        verify(tmapReverseGeocodingClient, never()).findRegion(35.1861, 129.0801);
    }
}
