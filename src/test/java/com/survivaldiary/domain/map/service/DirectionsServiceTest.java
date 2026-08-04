package com.survivaldiary.domain.map.service;

import com.survivaldiary.domain.map.client.TmapDirectionsClient;
import com.survivaldiary.domain.map.dto.DirectionsRequest;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DirectionsServiceTest {

    private final TmapDirectionsClient client = mock(TmapDirectionsClient.class);
    private final DirectionsService service = new DirectionsService(client);

    @Test
    void 위도와_경도_범위를_벗어나면_외부_API를_호출하지_않는다() {
        DirectionsRequest request = new DirectionsRequest(
                91.0,
                129.0592,
                35.1600,
                129.0650
        );

        assertThatThrownBy(() -> service.findOptimalRoute(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_MAP_FILTER);
        verifyNoInteractions(client);
    }
}
