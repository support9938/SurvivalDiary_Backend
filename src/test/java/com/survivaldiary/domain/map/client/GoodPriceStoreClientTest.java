package com.survivaldiary.domain.map.client;

import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
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

class GoodPriceStoreClientTest {

    private GoodPriceStoreProperties properties;
    private GoodPriceStoreClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new GoodPriceStoreProperties();
        properties.setApiKey("encoded%2Fkey%3D");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.odcloud.kr/api");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GoodPriceStoreClient(builder.build(), properties);
    }

    @Test
    void 인증_접두사와_지역_조건을_전달한다() {
        server.expect(request -> {
                    assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                            .isEqualTo("Infuser encoded/key=");
                    String decodedQuery = UriUtils.decode(
                            request.getURI().getRawQuery(),
                            StandardCharsets.UTF_8
                    );
                    assertThat(decodedQuery).contains("page=1");
                    assertThat(decodedQuery).contains("perPage=20");
                    assertThat(decodedQuery).contains("cond[시도::EQ]=서울특별시");
                    assertThat(decodedQuery).contains("cond[시군::EQ]=종로구");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                                {
                                  "currentCount": 0,
                                  "matchCount": 0,
                                  "page": 1,
                                  "perPage": 20,
                                  "totalCount": 12645,
                                  "data": []
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        var response = client.fetchStores(1, 20, "서울특별시", "종로구");

        assertThat(response.data()).isEmpty();
        server.verify();
    }

    @Test
    void 인증키_거절은_안전한_제공처_장애로_변환한다() {
        server.expect(request -> {
                })
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":-401,\"msg\":\"인증키 오류\"}"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.fetchStores(1, 20, null, null)
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
        assertThat(exception.getMessage()).doesNotContain("인증키 오류");
        assertThat(exception.getMessage()).doesNotContain("encoded/key=");
        server.verify();
    }
}
