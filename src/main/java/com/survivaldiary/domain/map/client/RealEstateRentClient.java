package com.survivaldiary.domain.map.client;

import com.survivaldiary.domain.map.dto.HousingRentDealResponse;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class RealEstateRentClient {

    private static final String SINGLE_FAMILY_PATH =
            "/RTMSDataSvcSHRent/getRTMSDataSvcSHRent";
    private static final String OFFICETEL_PATH =
            "/RTMSDataSvcOffiRent/getRTMSDataSvcOffiRent";
    private static final int PAGE_SIZE = 1000;

    private final RestClient restClient;
    private final RealEstateRentProperties properties;

    public RealEstateRentClient(
            @Qualifier("realEstateRentRestClient") RestClient restClient,
            RealEstateRentProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public List<HousingRentDealResponse> fetchSingleFamilyDeals(
            String lawdCd,
            String dealYmd
    ) {
        return fetchDeals(SINGLE_FAMILY_PATH, "단독/다가구", lawdCd, dealYmd);
    }

    public List<HousingRentDealResponse> fetchOfficetelDeals(
            String lawdCd,
            String dealYmd
    ) {
        return fetchDeals(OFFICETEL_PATH, "오피스텔", lawdCd, dealYmd);
    }

    private List<HousingRentDealResponse> fetchDeals(
            String path,
            String propertyType,
            String lawdCd,
            String dealYmd
    ) {
        List<HousingRentDealResponse> deals = new ArrayList<>();
        int pageNo = 1;
        int totalCount;
        do {
            ProviderPage page = fetchPage(
                    path,
                    propertyType,
                    lawdCd,
                    dealYmd,
                    pageNo
            );
            deals.addAll(page.deals());
            totalCount = page.totalCount();
            if (page.deals().isEmpty()) {
                break;
            }
            pageNo++;
        } while (deals.size() < totalCount);
        return List.copyOf(deals);
    }

    private ProviderPage fetchPage(
            String path,
            String propertyType,
            String lawdCd,
            String dealYmd,
            int pageNo
    ) {
        try {
            byte[] body = restClient.get()
                    .uri(
                            path
                                    + "?serviceKey={serviceKey}"
                                    + "&LAWD_CD={lawdCd}"
                                    + "&DEAL_YMD={dealYmd}"
                                    + "&numOfRows={numOfRows}"
                                    + "&pageNo={pageNo}",
                            decodedApiKey(),
                            lawdCd,
                            dealYmd,
                            PAGE_SIZE,
                            pageNo
                    )
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw providerError(response.getStatusCode());
                    })
                    .body(byte[].class);
            if (body == null || body.length == 0) {
                throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
            }
            return parse(
                    new String(body, StandardCharsets.UTF_8),
                    propertyType,
                    lawdCd,
                    dealYmd,
                    pageNo
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }
    }

    private ProviderPage parse(
            String xml,
            String propertyType,
            String lawdCd,
            String dealYmd,
            int pageNo
    ) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(
                    new InputSource(new StringReader(xml))
            );

            if (document.getElementsByTagName("OpenAPI_ServiceResponse").getLength() > 0) {
                throw new BusinessException(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
            }
            String resultCode = firstText(document, "resultCode");
            if (resultCode != null && !List.of("0", "00", "000").contains(resultCode)) {
                throw new BusinessException(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
            }

            NodeList itemNodes = document.getElementsByTagName("item");
            List<HousingRentDealResponse> deals = new ArrayList<>(itemNodes.getLength());
            for (int index = 0; index < itemNodes.getLength(); index++) {
                Node node = itemNodes.item(index);
                if (node instanceof Element item) {
                    deals.add(toDeal(
                            item,
                            propertyType,
                            lawdCd,
                            dealYmd,
                            (pageNo - 1) * PAGE_SIZE + index
                    ));
                }
            }
            int totalCount = documentInteger(document, "totalCount", deals.size());
            return new ProviderPage(List.copyOf(deals), totalCount);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }
    }

    private HousingRentDealResponse toDeal(
            Element item,
            String propertyType,
            String lawdCd,
            String dealYmd,
            int index
    ) {
        String neighborhood = text(item, "umdNm", "법정동");
        String lotNumber = text(item, "jibun", "지번");
        String propertyName = "오피스텔".equals(propertyType)
                ? text(item, "offiNm", "단지")
                : text(item, "houseType", "주택유형");
        if (propertyName.isBlank()) {
            propertyName = neighborhood + " " + propertyType;
        }
        int deposit = integer(item, "deposit", "보증금액");
        int monthlyRent = integer(item, "monthlyRent", "월세금액");
        LocalDate contractDate = date(item, dealYmd);
        BigDecimal area = decimal(item, "excluUseAr", "totalFloorAr", "전용면적");
        Integer floor = nullableInteger(item, "floor", "층");
        Integer buildYear = nullableInteger(item, "buildYear", "건축년도");
        String contractTerm = text(item, "contractTerm", "계약기간");
        String contractType = text(item, "contractType", "계약구분");
        Integer previousDeposit = nullableInteger(item, "preDeposit", "종전계약 보증금");
        Integer previousMonthlyRent = nullableInteger(
                item,
                "preMonthlyRent",
                "종전계약 월세"
        );
        String renewalRequestRightUsed = text(
                item,
                "useRRRight",
                "갱신요구권 사용"
        );
        String id = String.join("|",
                propertyType,
                lawdCd,
                contractDate.toString(),
                neighborhood,
                lotNumber,
                propertyName,
                Integer.toString(deposit),
                Integer.toString(monthlyRent),
                Integer.toString(index)
        );
        return new HousingRentDealResponse(
                id,
                propertyType,
                propertyName,
                monthlyRent == 0 ? "전세" : "월세",
                deposit,
                monthlyRent,
                contractDate,
                area,
                floor,
                neighborhood,
                lotNumber,
                buildYear,
                contractTerm,
                contractType,
                previousDeposit,
                previousMonthlyRent,
                renewalRequestRightUsed,
                "",
                null,
                null,
                ""
        );
    }

    private LocalDate date(Element item, String dealYmd) {
        int year = integer(item, "dealYear", "계약년도");
        int month = integer(item, "dealMonth", "계약월");
        int day = integer(item, "dealDay", "계약일");
        if (year == 0) {
            year = Integer.parseInt(dealYmd.substring(0, 4));
        }
        if (month == 0) {
            month = Integer.parseInt(dealYmd.substring(4, 6));
        }
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }
    }

    private String decodedApiKey() {
        String apiKey = properties.requireApiKey();
        return apiKey.contains("%")
                ? URLDecoder.decode(apiKey, StandardCharsets.UTF_8)
                : apiKey;
    }

    private static String firstText(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().trim();
    }

    private static int documentInteger(
            Document document,
            String tagName,
            int fallback
    ) {
        String value = firstText(document, tagName);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }
    }

    private static String text(Element item, String... names) {
        for (String name : names) {
            NodeList nodes = item.getElementsByTagName(name);
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent().trim();
            }
        }
        return "";
    }

    private static int integer(Element item, String... names) {
        Integer value = nullableInteger(item, names);
        return value == null ? 0 : value;
    }

    private static Integer nullableInteger(Element item, String... names) {
        String value = text(item, names).replaceAll("[^0-9-]", "");
        if (value.isBlank() || "-".equals(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }
    }

    private static BigDecimal decimal(Element item, String... names) {
        String value = text(item, names).replace(",", "");
        if (value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
        }
    }

    private BusinessException providerError(HttpStatusCode status) {
        if (status.value() == 401 || status.value() == 403 || status.is5xxServerError()) {
            return new BusinessException(ErrorCode.MAP_PROVIDER_UNAVAILABLE);
        }
        return new BusinessException(ErrorCode.MAP_PROVIDER_BAD_RESPONSE);
    }

    private record ProviderPage(
            List<HousingRentDealResponse> deals,
            int totalCount
    ) {
    }
}
