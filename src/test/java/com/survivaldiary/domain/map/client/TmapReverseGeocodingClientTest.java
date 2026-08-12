package com.survivaldiary.domain.map.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TmapReverseGeocodingClientTest {

    private TmapDirectionsProperties properties;
    private TmapReverseGeocodingClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new TmapDirectionsProperties();
        properties.setAppKey("app-key");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://apis.openapi.sk.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TmapReverseGeocodingClient(builder.build(), properties);
    }

    @Test
    void 좌표의_시도_시군구와_법정동_코드를_반환한다() {
        server.expect(request -> assertThat(request.getHeaders().getFirst("appKey"))
                        .isEqualTo("app-key"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("version", "1"))
                .andExpect(queryParam("lat", "35.1861"))
                .andExpect(queryParam("lon", "129.0801"))
                .andExpect(queryParam("coordType", "WGS84GEO"))
                .andExpect(queryParam("addressType", "A10"))
                .andExpect(queryParam("newAddressExtend", "Y"))
                .andRespond(withSuccess(
                        """
                                {
                                  "addressInfo": {
                                    "city_do": "부산광역시",
                                    "gu_gun": "연제구",
                                    "legalDongCode": "2647010200"
                                  }
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        var region = client.findRegion(35.1861, 129.0801);

        assertThat(region).contains(new TmapReverseGeocodingClient.Region(
                "부산광역시",
                "연제구",
                "26470"
        ));
        server.verify();
    }

    @Test
    void 세종은_시군구_대신_시도명을_사용한다() {
        server.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                                {
                                  "addressInfo": {
                                    "city_do": "세종특별자치시",
                                    "gu_gun": "",
                                    "legalDongCode": "3611010300"
                                  }
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        var region = client.findRegion(36.48, 127.289);

        assertThat(region).contains(new TmapReverseGeocodingClient.Region(
                "세종특별자치시",
                "세종특별자치시",
                "36110"
        ));
        server.verify();
    }

    @Test
    void 잘못된_법정동_코드이면_응답을_사용하지_않는다() {
        server.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                                {
                                  "addressInfo": {
                                    "city_do": "부산광역시",
                                    "gu_gun": "연제구",
                                    "legalDongCode": "invalid"
                                  }
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        assertThat(client.findRegion(35.1861, 129.0801)).isEmpty();
        server.verify();
    }

    @Test
    void 좌표나_인증정보가_유효하지_않으면_외부_API를_호출하지_않는다() {
        assertThat(client.findRegion(91, 129.0801)).isEmpty();
        properties.setAppKey("");
        assertThat(client.findRegion(35.1861, 129.0801)).isEmpty();
        server.verify();
    }
}
