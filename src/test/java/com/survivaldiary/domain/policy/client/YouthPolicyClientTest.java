package com.survivaldiary.domain.policy.client;

import com.survivaldiary.domain.policy.client.dto.YouthPolicySearchRequest;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class YouthPolicyClientTest {

    private YouthPolicyProperties properties;
    private YouthPolicyClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new YouthPolicyProperties();
        properties.setApiKey("test-api-key");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://www.youthcenter.go.kr");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new YouthPolicyClient(builder.build(), properties);
    }

    @Test
    void 목록_조회에_공식_파라미터와_선택_조건을_전달한다() {
        server.expect(request -> {
                    MultiValueMap<String, String> query = UriComponentsBuilder
                            .fromUri(request.getURI())
                            .build()
                            .getQueryParams();

                    assertThat(request.getURI().getPath()).isEqualTo("/go/ythip/getPlcy");
                    assertThat(query.getFirst("apiKeyNm")).isEqualTo("test-api-key");
                    assertThat(query.getFirst("pageNum")).isEqualTo("1");
                    assertThat(query.getFirst("pageSize")).isEqualTo("20");
                    assertThat(query.getFirst("pageType")).isEqualTo("1");
                    assertThat(query.getFirst("rtnType")).isEqualTo("json");
                    assertThat(query.getFirst("zipCd")).isEqualTo("11680");
                    assertThat(decoded(query.getFirst("lclsfNm"))).isEqualTo("주거");
                    assertThat(query.containsKey("mclsfNm")).isFalse();
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"data\":{\"items\":[]}}",
                        MediaType.APPLICATION_JSON
                ));

        JsonNode response = client.search(new YouthPolicySearchRequest(
                1,
                20,
                "11680",
                "주거",
                null,
                null,
                null
        ));

        assertThat(response.path("data").path("items").isArray()).isTrue();
        server.verify();
    }

    @Test
    void 상세_조회는_정책_ID를_문자열로_전달한다() {
        server.expect(request -> {
                    MultiValueMap<String, String> query = UriComponentsBuilder
                            .fromUri(request.getURI())
                            .build()
                            .getQueryParams();

                    assertThat(query.getFirst("pageType")).isEqualTo("2");
                    assertThat(query.getFirst("plcyNo")).isEqualTo("R2026-0001");
                    assertThat(query.getFirst("rtnType")).isEqualTo("json");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"data\":{\"plcyNo\":\"R2026-0001\"}}",
                        MediaType.APPLICATION_JSON
                ));

        JsonNode response = client.findDetail("R2026-0001");

        assertThat(response.path("data").path("plcyNo").asText()).isEqualTo("R2026-0001");
        server.verify();
    }

    @Test
    void 인증키_거절은_원인만_기록하고_안전한_제공처_장애로_변환한다(CapturedOutput output) {
        server.expect(request -> {
                })
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errorCode\":\"e001\",\"errorMsg\":\"invalid api key.\"}"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.search(defaultSearchRequest())
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
        assertThat(exception.getMessage()).doesNotContain("invalid api key");
        assertThat(exception.getMessage()).doesNotContain("test-api-key");
        assertThat(output.getOut())
                .contains("operation=SEARCH, reason=AUTH_REJECTED, status=403")
                .doesNotContain("invalid api key")
                .doesNotContain("test-api-key");
        server.verify();
    }

    @Test
    void 제공처_서버_오류는_일시적_장애로_변환한다() {
        server.expect(request -> {
                })
                .andRespond(withServerError());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.search(defaultSearchRequest())
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
        server.verify();
    }

    @Test
    void 잘못된_JSON은_제공처_응답_오류로_변환한다() {
        server.expect(request -> {
                })
                .andRespond(withSuccess("{not-json", MediaType.APPLICATION_JSON));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.search(defaultSearchRequest())
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_PROVIDER_BAD_RESPONSE);
        server.verify();
    }

    @Test
    void 인증키가_없으면_HTTP_요청_전에_원인을_기록하고_안전하게_실패한다(CapturedOutput output) {
        properties.setApiKey(" ");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.search(defaultSearchRequest())
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
        assertThat(output.getOut())
                .contains("operation=SEARCH, reason=API_KEY_MISSING")
                .doesNotContain("apiKeyNm");
        server.verify();
    }

    @Test
    void 읽기_시간_초과는_예외_메시지나_인증키_없이_원인만_기록한다(CapturedOutput output) {
        server.expect(request -> {
                })
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out: test-api-key");
                });

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.search(defaultSearchRequest())
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_PROVIDER_UNAVAILABLE);
        assertThat(output.getOut())
                .contains("operation=SEARCH, reason=READ_TIMEOUT")
                .doesNotContain("Read timed out")
                .doesNotContain("test-api-key")
                .doesNotContain("apiKeyNm");
        server.verify();
    }

    @Test
    void 잘못된_페이지_조건은_요청_생성_전에_거절한다() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> new YouthPolicySearchRequest(0, 20, null, null, null, null, null)
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_POLICY_FILTER);
    }

    private YouthPolicySearchRequest defaultSearchRequest() {
        return new YouthPolicySearchRequest(1, 20, null, null, null, null, null);
    }

    private String decoded(String value) {
        return UriUtils.decode(value, StandardCharsets.UTF_8);
    }
}
