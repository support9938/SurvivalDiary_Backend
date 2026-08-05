package com.survivaldiary.domain.map.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RealEstateRentClientTest {

    private RealEstateRentClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RealEstateRentProperties properties = new RealEstateRentProperties();
        properties.setApiKey("encoded%2Fkey%2Bvalue%3D");
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://apis.data.go.kr/1613000");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RealEstateRentClient(builder.build(), properties);
    }

    @Test
    void 단독다가구_전월세_응답을_공통_형식으로_변환한다() {
        server.expect(request -> {
                    assertThat(request.getURI().getPath())
                            .isEqualTo("/1613000/RTMSDataSvcSHRent/getRTMSDataSvcSHRent");
                    String query = UriUtils.decode(
                            request.getURI().getRawQuery(),
                            StandardCharsets.UTF_8
                    );
                    assertThat(request.getURI().getRawQuery())
                            .contains("serviceKey=encoded%2Fkey%2Bvalue%3D");
                    assertThat(query).contains("serviceKey=encoded/key+value=");
                    assertThat(query).contains("LAWD_CD=11680");
                    assertThat(query).contains("DEAL_YMD=202608");
                    assertThat(query).contains("numOfRows=1000");
                    assertThat(query).contains("pageNo=1");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(singleFamilyXml(), MediaType.APPLICATION_XML));

        var deals = client.fetchSingleFamilyDeals("11680", "202608");

        assertThat(deals).hasSize(1);
        assertThat(deals.get(0).propertyType()).isEqualTo("단독/다가구");
        assertThat(deals.get(0).dealType()).isEqualTo("월세");
        assertThat(deals.get(0).depositTenThousandWon()).isEqualTo(10000);
        assertThat(deals.get(0).monthlyRentTenThousandWon()).isEqualTo(80);
        assertThat(deals.get(0).neighborhood()).isEqualTo("역삼동");
        server.verify();
    }

    @Test
    void 오피스텔_전월세_응답을_공통_형식으로_변환한다() {
        server.expect(request -> {
                    assertThat(request.getURI().getPath())
                            .isEqualTo("/1613000/RTMSDataSvcOffiRent/getRTMSDataSvcOffiRent");
                })
                .andRespond(withSuccess(officetelXml(), MediaType.APPLICATION_XML));

        var deals = client.fetchOfficetelDeals("11680", "202608");

        assertThat(deals).hasSize(1);
        assertThat(deals.get(0).propertyName()).isEqualTo("강남역 오피스텔");
        assertThat(deals.get(0).dealType()).isEqualTo("전세");
        assertThat(deals.get(0).floor()).isEqualTo(8);
        assertThat(deals.get(0).areaSquareMeters()).isEqualByComparingTo("29.8");
        server.verify();
    }

    private String singleFamilyXml() {
        return """
                <response>
                  <header><resultCode>000</resultCode><resultMsg>OK</resultMsg></header>
                  <body><items><item>
                    <dealYear>2026</dealYear><dealMonth>8</dealMonth><dealDay>3</dealDay>
                    <deposit>10,000</deposit><monthlyRent>80</monthlyRent>
                    <totalFloorAr>41.2</totalFloorAr><buildYear>2018</buildYear>
                    <umdNm>역삼동</umdNm><jibun>123-*</jibun><houseType>다가구</houseType>
                  </item></items><totalCount>1</totalCount></body>
                </response>
                """;
    }

    private String officetelXml() {
        return """
                <response>
                  <header><resultCode>000</resultCode><resultMsg>OK</resultMsg></header>
                  <body><items><item>
                    <dealYear>2026</dealYear><dealMonth>8</dealMonth><dealDay>2</dealDay>
                    <deposit>23,500</deposit><monthlyRent>0</monthlyRent>
                    <excluUseAr>29.8</excluUseAr><floor>8</floor><buildYear>2020</buildYear>
                    <umdNm>역삼동</umdNm><jibun>456-*</jibun><offiNm>강남역 오피스텔</offiNm>
                  </item></items><totalCount>1</totalCount></body>
                </response>
                """;
    }
}
