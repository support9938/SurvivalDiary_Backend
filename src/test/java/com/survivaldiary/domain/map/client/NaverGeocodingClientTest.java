package com.survivaldiary.domain.map.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverGeocodingClientTest {

    private NaverGeocodingProperties properties;
    private NaverGeocodingClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new NaverGeocodingProperties();
        properties.setApiKeyId("key-id");
        properties.setApiKey("api-key");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://maps.apigw.ntruss.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NaverGeocodingClient(builder.build(), properties);
    }

    @Test
    void 주소를_좌표로_변환하고_같은_주소는_캐시한다() {
        server.expect(request -> {
                    assertThat(request.getHeaders().getFirst("x-ncp-apigw-api-key-id"))
                            .isEqualTo("key-id");
                    assertThat(request.getHeaders().getFirst("x-ncp-apigw-api-key"))
                            .isEqualTo("api-key");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                                {
                                  "addresses": [
                                    {"x": "126.9990", "y": "37.5796"}
                                  ]
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        var first = client.findCoordinates("서울특별시 종로구 대학로5길 5");
        var cached = client.findCoordinates("서울특별시 종로구 대학로5길 5");

        assertThat(first).isPresent();
        assertThat(first.orElseThrow().latitude()).isEqualTo(37.5796);
        assertThat(first.orElseThrow().longitude()).isEqualTo(126.9990);
        assertThat(cached).isEqualTo(first);
        server.verify();
    }

    @Test
    void 인증정보가_없으면_외부_요청_없이_좌표를_생략한다() {
        properties.setApiKey("");

        assertThat(client.findCoordinates("서울특별시 종로구")).isEmpty();
        server.verify();
    }
}
