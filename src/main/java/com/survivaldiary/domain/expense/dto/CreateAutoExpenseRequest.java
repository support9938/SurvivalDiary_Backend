package com.survivaldiary.domain.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "결제 알림 감지 지출 저장 요청")
public record CreateAutoExpenseRequest(

        @Schema(description = "로그인 사용자 ID", example = "1")
        @NotNull(message = "사용자 ID는 필수입니다.")
        @Positive(message = "사용자 ID가 올바르지 않습니다.")
        Long userId,

        @Schema(description = "시스템 기본 카테고리 ID(1: 식비, 2: 카페, 3: 교통, 4: 쇼핑, 5: 기타)",
                example = "2")
        @NotNull(message = "카테고리는 필수입니다.")
        @Min(value = 1, message = "카테고리가 올바르지 않습니다.")
        @Max(value = 5, message = "카테고리가 올바르지 않습니다.")
        Long categoryId,

        @Schema(description = "감지된 가맹점 또는 지출 내용", example = "스타벅스 강남점")
        @NotBlank(message = "지출 내용은 필수입니다.")
        @Size(max = 100, message = "지출 내용은 100자 이하여야 합니다.")
        String title,

        @Schema(description = "지출 금액", example = "5500")
        @NotNull(message = "금액은 필수입니다.")
        @Positive(message = "금액은 0보다 커야 합니다.")
        Integer amount,

        @Schema(description = "알림이 감지된 결제 일시", example = "2026-08-03T09:42:00")
        @NotNull(message = "지출 일시는 필수입니다.")
        LocalDateTime spentAt,

        @Schema(description = "메모", example = "결제 알림에서 자동 감지")
        @Size(max = 200, message = "메모는 200자 이하여야 합니다.")
        String memo,

        @Schema(description = "같은 알림의 중복 등록을 막는 식별 키",
                example = "87d341ae615e73ad7fe237abf1828c01b7dd7731a5a7c2f98efa953aa9ecfc83")
        @NotBlank(message = "감지 식별 키는 필수입니다.")
        @Size(max = 64, message = "감지 식별 키는 64자 이하여야 합니다.")
        String detectionKey,

        @Schema(description = "결제 알림 출처", example = "토스")
        @NotBlank(message = "알림 출처는 필수입니다.")
        @Size(max = 100, message = "알림 출처는 100자 이하여야 합니다.")
        String notificationSource
) {}
