package com.survivaldiary.domain.expense.dto;

import com.survivaldiary.domain.expense.entity.Expense;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "직접 입력 지출 저장 요청")
public record CreateExpenseRequest(

        @Schema(description = "로그인 사용자 ID", example = "1")
        @NotNull(message = "사용자 ID는 필수입니다.")
        @Positive(message = "사용자 ID가 올바르지 않습니다.")
        Long userId,

        @Schema(description = "시스템 기본 카테고리 ID(1: 식비, 2: 카페, 3: 교통, 4: 쇼핑, 5: 기타)",
                example = "1")
        @NotNull(message = "카테고리는 필수입니다.")
        @Min(value = 1, message = "카테고리가 올바르지 않습니다.")
        @Max(value = 5, message = "카테고리가 올바르지 않습니다.")
        Long categoryId,

        @Schema(description = "지출 내용", example = "점심 김치찌개")
        @NotBlank(message = "지출 내용은 필수입니다.")
        @Size(max = 100, message = "지출 내용은 100자 이하여야 합니다.")
        String title,

        @Schema(description = "지출 금액", example = "9000")
        @NotNull(message = "금액은 필수입니다.")
        @Positive(message = "금액은 0보다 커야 합니다.")
        @Max(value = Integer.MAX_VALUE, message = "금액은 2,147,483,647원 이하여야 합니다.")
        Long amount,

        @Schema(description = "지출 일시", example = "2026-07-31T00:00:00")
        @NotNull(message = "지출 일시는 필수입니다.")
        LocalDateTime spentAt,

        @Schema(description = "메모", example = "회사 근처 식당")
        @Size(max = 200, message = "메모는 200자 이하여야 합니다.")
        String memo,

        @Schema(description = "등록 방식. 직접 입력은 MANUAL만 허용한다.", example = "MANUAL")
        @NotNull(message = "등록 방식은 필수입니다.")
        Expense.EntryType entryType
) {}
