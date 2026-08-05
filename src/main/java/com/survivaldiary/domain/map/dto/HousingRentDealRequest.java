package com.survivaldiary.domain.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HousingRentDealRequest(
        @Schema(description = "법정동 코드 앞 5자리", example = "11680")
        @Pattern(regexp = "^[0-9]{5}$")
        String lawdCd,

        @Schema(description = "조회 종료 계약년월", example = "202608")
        @Pattern(regexp = "^[0-9]{6}$")
        String dealYmd,

        @Schema(description = "조회할 개월 수", example = "3", defaultValue = "3")
        @Min(1)
        @Max(12)
        Integer months,

        @Schema(description = "법정동 필터", example = "역삼동")
        String neighborhood,

        @Schema(description = "지오코딩용 선택 지역명", example = "서울특별시 강남구 역삼동")
        @Size(max = 100)
        String region,

        @Schema(description = "최대 응답 건수", example = "100", defaultValue = "100")
        @Min(1)
        @Max(100)
        Integer limit
) {
    public int requestedMonths() {
        return months == null ? 3 : months;
    }

    public int requestedLimit() {
        return limit == null ? 100 : limit;
    }
}
