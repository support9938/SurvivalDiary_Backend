package com.survivaldiary.domain.map.controller;

import com.survivaldiary.domain.map.dto.PublicFacilityResponse;
import com.survivaldiary.domain.map.service.PublicFacilityService;
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

class PublicFacilityControllerTest {

    private PublicFacilityService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PublicFacilityService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicFacilityController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 공통_응답과_페이지_형식으로_시설을_반환한다() throws Exception {
        var facility = new PublicFacilityResponse.Facility(
                "facility-id",
                "세미나실",
                "청년센터",
                "회의실",
                "서울특별시 종로구 세종대로 1",
                "02-000-0000",
                37.5700,
                126.9800,
                320,
                false,
                "무료",
                "09:00~18:00",
                "10:00~17:00",
                "연중무휴",
                "서울특별시",
                "청년정책과",
                "https://example.com",
                "",
                "20",
                "50",
                "와이파이",
                "온라인",
                "2026-08-01"
        );
        when(service.findFacilities(
                0, 50, 37.5, 126.9, 37.6, 127.1,
                37.55, 127.0, null, false, "distance"
        )).thenReturn(new PageResponse<>(List.of(facility), 0, 50, 1, 1, false));

        mockMvc.perform(get("/api/map/public-facilities")
                        .param("southWestLat", "37.5")
                        .param("southWestLng", "126.9")
                        .param("northEastLat", "37.6")
                        .param("northEastLng", "127.1")
                        .param("latitude", "37.55")
                        .param("longitude", "127.0"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("세미나실"))
                .andExpect(jsonPath("$.data.content[0].fee").value("무료"));
    }
}
