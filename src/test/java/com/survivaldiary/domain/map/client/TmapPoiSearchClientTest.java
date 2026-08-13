package com.survivaldiary.domain.map.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TmapPoiSearchClientTest {

    private TmapDirectionsProperties properties;
    private TmapPoiSearchClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new TmapDirectionsProperties();
        properties.setAppKey("app-key");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://apis.openapi.sk.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TmapPoiSearchClient(builder.build(), properties);
    }

    @Test
    void searchesPoiAndUsesEntranceCoordinates() {
        server.expect(request -> assertThat(request.getHeaders().getFirst("appKey"))
                        .isEqualTo("app-key"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("version", "1"))
                .andExpect(queryParam("searchKeyword", "서면역"))
                .andExpect(queryParam("count", "5"))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("reqCoordType", "WGS84GEO"))
                .andExpect(queryParam("resCoordType", "WGS84GEO"))
                .andRespond(withSuccess(
                        """
                                {
                                  "searchPoiInfo": {
                                    "pois": {
                                      "poi": [{
                                        "name": "서면역 부산1호선",
                                        "upperAddrName": "부산광역시",
                                        "middleAddrName": "부산진구",
                                        "lowerAddrName": "부전동",
                                        "detailAddrName": "573-1",
                                        "frontLat": "35.1578",
                                        "frontLon": "129.0592",
                                        "noorLat": "35.1577",
                                        "noorLon": "129.0591"
                                      }]
                                    }
                                  }
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        assertThat(client.search("서면역", 5))
                .containsExactly(new TmapPoiSearchClient.Place(
                        "서면역 부산1호선",
                        "부산광역시 부산진구 부전동 573-1",
                        35.1578,
                        129.0592
                ));
        server.verify();
    }

    @Test
    void skipsRequestWhenAppKeyIsMissing() {
        properties.setAppKey("");

        assertThat(client.search("서면역", 5)).isEmpty();
        server.verify();
    }
}
