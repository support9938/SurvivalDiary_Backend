package com.survivaldiary.domain.map.controller;

import com.survivaldiary.domain.map.dto.GoodPriceStoreResponse;
import com.survivaldiary.domain.map.dto.MapViewportBounds;
import com.survivaldiary.domain.map.service.GoodPriceStoreService;
import com.survivaldiary.global.common.PageResponse;
import com.survivaldiary.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoodPriceStoreControllerTest {

    private GoodPriceStoreService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(GoodPriceStoreService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GoodPriceStoreController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 공통_응답과_페이지_형식으로_목록을_반환한다() throws Exception {
        GoodPriceStoreResponse.Store store = new GoodPriceStoreResponse.Store(
                "서울특별시",
                "종로구",
                "양식",
                "돈까스보라",
                "02-741-3455",
                "서울특별시 종로구 대학로5길 5",
                "수제 돈까스",
                "7000",
                "",
                "",
                "",
                "",
                "",
                "",
                37.5796,
                126.9990
        );
        when(service.findStores(
                0,
                20,
                "서울특별시",
                "종로구",
                "price",
                new MapViewportBounds(37.5, 126.9, 37.6, 127.1)
        ))
                .thenReturn(new PageResponse<>(List.of(store), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/map/good-price-stores")
                        .param("province", "서울특별시")
                        .param("district", "종로구")
                        .param("sort", "price")
                        .param("southWestLat", "37.5")
                        .param("southWestLng", "126.9")
                        .param("northEastLat", "37.6")
                        .param("northEastLng", "127.1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("돈까스보라"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }
}
