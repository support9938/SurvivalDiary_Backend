package com.survivaldiary.domain.map.controller;

import com.survivaldiary.domain.map.dto.DirectionsRequest;
import com.survivaldiary.domain.map.dto.DirectionsResponse;
import com.survivaldiary.domain.map.service.DirectionsService;
import com.survivaldiary.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DirectionsControllerTest {

    private DirectionsService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(DirectionsService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DirectionsController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 공통_응답으로_최적_경로를_반환한다() throws Exception {
        DirectionsResponse response = new DirectionsResponse(
                1250,
                240000,
                0,
                0,
                0,
                List.of(
                        new DirectionsResponse.Coordinate(35.1578, 129.0592),
                        new DirectionsResponse.Coordinate(35.1600, 129.0650)
                )
        );
        when(service.findOptimalRoute(any(DirectionsRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/map/directions")
                        .param("startLatitude", "35.1578")
                        .param("startLongitude", "129.0592")
                        .param("goalLatitude", "35.1600")
                        .param("goalLongitude", "129.0650"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.distanceMeters").value(1250))
                .andExpect(jsonPath("$.data.durationMillis").value(240000))
                .andExpect(jsonPath("$.data.path[1].latitude").value(35.1600))
                .andExpect(jsonPath("$.data.path[1].longitude").value(129.0650));
    }

    @Test
    void 위도_범위를_벗어난_요청은_거절한다() throws Exception {
        mockMvc.perform(get("/api/map/directions")
                        .param("startLatitude", "91.0")
                        .param("startLongitude", "129.0592")
                        .param("goalLatitude", "35.1600")
                        .param("goalLongitude", "129.0650"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C001"));

        verifyNoInteractions(service);
    }
}
