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

class PublicFacilityClientTest {

    private PublicFacilityClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        PublicFacilityProperties properties = new PublicFacilityProperties();
        properties.setApiKey("encoded%2Fkey%3D");
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.data.go.kr");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PublicFacilityClient(builder.build(), properties);
    }

    @Test
    void 공공데이터_페이지와_시설_필드를_변환한다() {
        server.expect(request -> {
                    String query = UriUtils.decode(
                            request.getURI().getRawQuery(),
                            StandardCharsets.UTF_8
                    );
                    assertThat(query).contains("serviceKey=encoded/key=");
                    assertThat(query).contains("pageNo=1");
                    assertThat(query).contains("numOfRows=1000");
                    assertThat(query).contains("type=json");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                                {
                                  "response": {
                                    "header": {"resultCode": "00", "resultMsg": "NORMAL_SERVICE"},
                                    "body": {
                                      "items": [{
                                        "openFcltyNm": "세미나실",
                                        "openLcNm": "청년센터",
                                        "openFcltyType": "회의실",
                                        "pchrgUseYn": "N",
                                        "rdnmadr": "서울특별시 종로구 세종대로 1",
                                        "latitude": "37.5700",
                                        "longitude": "126.9800",
                                        "insttCode": "1111000"
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
        assertThat(page.items().get(0).facilityName()).isEqualTo("세미나실");
        assertThat(page.items().get(0).latitude()).isEqualTo(37.5700);
        assertThat(page.items().get(0).institutionCode()).isEqualTo("1111000");
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
