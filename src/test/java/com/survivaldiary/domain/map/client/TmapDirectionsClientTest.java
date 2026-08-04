package com.survivaldiary.domain.map.client;

import com.survivaldiary.domain.map.dto.DirectionsRequest;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TmapDirectionsClientTest {

    private TmapDirectionsClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        TmapDirectionsProperties properties = new TmapDirectionsProperties();
        properties.setAppKey("app-key");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://apis.openapi.sk.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TmapDirectionsClient(builder.build(), properties);
    }

    @Test
    void 보행자_추천_경로와_요약_정보를_반환한다() {
        server.expect(request -> assertThat(request.getHeaders().getFirst("appKey"))
                        .isEqualTo("app-key"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(queryParam("version", "1"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "startX":129.0592,
                          "startY":35.1578,
                          "endX":129.065,
                          "endY":35.16,
                          "reqCoordType":"WGS84GEO",
                          "resCoordType":"WGS84GEO",
                          "searchOption":"0",
                          "sort":"index"
                        }
                        """, false))
                .andRespond(withSuccess(
                        """
                                {
                                  "type": "FeatureCollection",
                                  "features": [
                                    {
                                      "type": "Feature",
                                      "geometry": {
                                        "type": "Point",
                                        "coordinates": [129.0592, 35.1578]
                                      },
                                      "properties": {
                                        "totalDistance": 1250,
                                        "totalTime": 240,
                                        "pointType": "SP"
                                      }
                                    },
                                    {
                                      "type": "Feature",
                                      "geometry": {
                                        "type": "LineString",
                                        "coordinates": [
                                          [129.0592, 35.1578],
                                          [129.0610, 35.1585]
                                        ]
                                      },
                                      "properties": {"index": 1}
                                    },
                                    {
                                      "type": "Feature",
                                      "geometry": {
                                        "type": "LineString",
                                        "coordinates": [
                                          [129.0610, 35.1585],
                                          [129.0650, 35.1600]
                                        ]
                                      },
                                      "properties": {"index": 2}
                                    }
                                  ]
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        var response = client.findOptimalRoute(request());

        assertThat(response.distanceMeters()).isEqualTo(1250);
        assertThat(response.durationMillis()).isEqualTo(240000);
        assertThat(response.tollFare()).isZero();
        assertThat(response.taxiFare()).isZero();
        assertThat(response.fuelPrice()).isZero();
        assertThat(response.path()).hasSize(3);
        assertThat(response.path().get(1).latitude()).isEqualTo(35.1585);
        assertThat(response.path().get(1).longitude()).isEqualTo(129.0610);
        server.verify();
    }

    @Test
    void 경로가_없으면_장소_경로_오류를_반환한다() {
        server.expect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"type\":\"FeatureCollection\",\"features\":[]}",
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.findOptimalRoute(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MAP_ROUTE_NOT_FOUND);
        server.verify();
    }

    private DirectionsRequest request() {
        return new DirectionsRequest(35.1578, 129.0592, 35.1600, 129.0650);
    }
}
