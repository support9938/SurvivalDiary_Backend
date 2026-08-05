package com.survivaldiary.domain.home.controller;

import com.survivaldiary.domain.home.dto.HomeSummaryResponse;
import com.survivaldiary.domain.home.service.HomeSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeSummaryControllerTest {

    @Test
    void wrapsSummaryInCommonResponseAndDisablesCaching() {
        HomeSummaryService service = mock(HomeSummaryService.class);
        HomeSummaryController controller = new HomeSummaryController(service);
        HomeSummaryResponse summary = new HomeSummaryResponse(
                "절약이", 35_000, 23_000, 12_000, 0, 245_000, 61_000, 1L
        );
        when(service.getSummary(1L)).thenReturn(summary);

        var response = controller.getSummary(1L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getCacheControl())
                .isEqualTo(CacheControl.noStore().getHeaderValue());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).isEqualTo(summary);
    }
}
