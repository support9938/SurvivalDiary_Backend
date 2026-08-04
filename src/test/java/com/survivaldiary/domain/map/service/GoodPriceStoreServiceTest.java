package com.survivaldiary.domain.map.service;

import com.survivaldiary.domain.map.client.GoodPriceStoreClient;
import com.survivaldiary.domain.map.client.NaverGeocodingClient;
import com.survivaldiary.domain.map.dto.GoodPriceStoreResponse;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoodPriceStoreServiceTest {

    private GoodPriceStoreClient client;
    private NaverGeocodingClient geocodingClient;
    private GoodPriceStoreService service;

    @BeforeEach
    void setUp() {
        client = mock(GoodPriceStoreClient.class);
        geocodingClient = mock(NaverGeocodingClient.class);
        when(geocodingClient.findCoordinates(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
        service = new GoodPriceStoreService(client, geocodingClient);
    }

    @Test
    void 지역_조건과_페이지를_제공처_형식으로_변환한다() {
        when(client.fetchStores(2, 20, "서울특별시", "종로구"))
                .thenReturn(response(List.of(store("가게", "7000")), 21));

        var result = service.findStores(
                1,
                20,
                " 서울특별시 ",
                " 종로구 ",
                "default"
        );

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(21);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
        verify(client).fetchStores(2, 20, "서울특별시", "종로구");
    }

    @Test
    void 가격순은_현재_페이지의_최저_메뉴가격으로_정렬한다() {
        when(client.fetchStores(1, 20, null, null)).thenReturn(
                response(
                        List.of(
                                store("비싼 가게", "12,000원"),
                                store("가격 없음", ""),
                                store("저렴한 가게", "5,000")
                        ),
                        3
                )
        );

        var result = service.findStores(0, 20, null, null, "price");

        assertThat(result.content())
                .extracting(GoodPriceStoreResponse.Store::name)
                .containsExactly("저렴한 가게", "비싼 가게", "가격 없음");
    }

    @Test
    void 시도_없이_시군만_요청하면_거부한다() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.findStores(0, 20, null, "종로구", "default")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_MAP_FILTER);
        verify(client, never()).fetchStores(1, 20, null, "종로구");
    }

    @Test
    void 페이지_크기가_범위를_벗어나면_거부한다() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.findStores(0, 101, null, null, "default")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_MAP_FILTER);
    }

    private GoodPriceStoreResponse response(
            List<GoodPriceStoreResponse.Store> stores,
            int totalCount
    ) {
        return new GoodPriceStoreResponse(
                stores.size(),
                totalCount,
                1,
                20,
                totalCount,
                stores
        );
    }

    private GoodPriceStoreResponse.Store store(String name, String price) {
        return new GoodPriceStoreResponse.Store(
                "서울특별시",
                "종로구",
                "한식",
                name,
                "02-000-0000",
                "서울특별시 종로구",
                "메뉴",
                price,
                "",
                "",
                "",
                "",
                "",
                "",
                null,
                null
        );
    }
}
