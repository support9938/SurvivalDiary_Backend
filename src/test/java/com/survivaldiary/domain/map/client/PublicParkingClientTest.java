package com.survivaldiary.domain.map.client;

import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PublicParkingClientTest {

    private PublicParkingClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        PublicParkingProperties properties = new PublicParkingProperties();
        properties.setApiKey("encoded%2Fkey%3D");
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.data.go.kr");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PublicParkingClient(builder.build(), properties);
    }

    @Test
    void 공공데이터_페이지와_주차장_필드를_변환한다() {
        server.expect(request -> {
                    String query = UriUtils.decode(
                            request.getURI().getRawQuery(),
                            StandardCharsets.UTF_8
                    );
                    assertThat(query).contains("serviceKey=encoded/key=");
                    assertThat(query).contains("pageNo=1");
                    assertThat(query).contains("numOfRows=1000");
                    assertThat(query).contains("type=json");
                    assertThat(query).contains("prkplceSe=공영");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                                {
                                  "response": {
                                    "header": {"resultCode": "00", "resultMsg": "NORMAL_SERVICE"},
                                    "body": {
                                      "items": [{
                                        "prkplceNo": "P-1",
                                        "prkplceNm": "시청 공영주차장",
                                        "prkplceSe": "공영",
                                        "prkplceType": "노외",
                                        "rdnmadr": "서울특별시 중구 세종대로 1",
                                        "latitude": "37.5665",
                                        "longitude": "126.9780",
                                        "instt_code": "1114000"
                                      }],
                                      "pageNo": 1,
                                      "numOfRows": 1000,
                                      "totalCount": 1
                                    }
                                  }
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        var page = client.fetchPage(1, 1000);

        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).name()).isEqualTo("시청 공영주차장");
        assertThat(page.items().get(0).latitude()).isEqualTo(37.5665);
        assertThat(page.items().get(0).institutionCode()).isEqualTo("1114000");
        server.verify();
    }

    @Test
    void 제공처_인증_오류를_안전한_장애로_변환한다() {
        server.expect(request -> {
                })
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.fetchPage(1, 1000)
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
        server.verify();
    }
}
