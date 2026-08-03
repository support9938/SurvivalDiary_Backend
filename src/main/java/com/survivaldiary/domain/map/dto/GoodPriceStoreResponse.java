package com.survivaldiary.domain.map.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record GoodPriceStoreResponse(
        int currentCount,
        int matchCount,
        int page,
        int perPage,
        int totalCount,
        List<Store> data
) {

    public record Store(
            @JsonAlias("시도") String province,
            @JsonAlias("시군") String district,
            @JsonAlias("업종") String category,
            @JsonAlias("업소명") String name,
            @JsonAlias("연락처") String phone,
            @JsonAlias("주소") String address,
            @JsonAlias("메뉴1") String menu1,
            @JsonAlias("가격1") String price1,
            @JsonAlias("메뉴2") String menu2,
            @JsonAlias("가격2") String price2,
            @JsonAlias("메뉴3") String menu3,
            @JsonAlias("가격3") String price3,
            @JsonAlias("메뉴4") String menu4,
            @JsonAlias("가격4") String price4,
            Double latitude,
            Double longitude
    ) {
        public Store withCoordinates(Double latitude, Double longitude) {
            return new Store(
                    province,
                    district,
                    category,
                    name,
                    phone,
                    address,
                    menu1,
                    price1,
                    menu2,
                    price2,
                    menu3,
                    price3,
                    menu4,
                    price4,
                    latitude,
                    longitude
            );
        }
    }
}
