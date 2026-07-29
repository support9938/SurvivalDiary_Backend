package com.survivaldiary.domain.user.dto;

import com.survivaldiary.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "회원가입 요청")
public record SignupRequest(

        @Schema(description = "로그인 이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Schema(description = "비밀번호 — 8~64자, 영문·숫자 각 1자 이상", example = "password1234")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                 message = "비밀번호는 영문과 숫자를 각각 1자 이상 포함해야 합니다.")
        String password,

        @Schema(description = "이름", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "생년월일 (선택)", example = "2000-03-15")
        LocalDate birthDate,

        @Schema(description = "성별 (선택): MALE / FEMALE / OTHER")
        User.Gender gender,

        @Schema(description = "지역 (선택)", example = "서울")
        @Size(max = 50)
        String region
) {}
