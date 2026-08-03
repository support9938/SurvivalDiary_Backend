package com.survivaldiary.domain.user.dto;

import com.survivaldiary.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "회원가입 요청")
public record SignupRequest(

        @Schema(description = "로그인 이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Schema(description = "비밀번호. 임시 정책상 빈 값만 거부합니다.", example = "1")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(max = 64, message = "비밀번호는 64자 이하여야 합니다.")
        String password,

        @Schema(description = "이름", example = "김민")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "Phone number", example = "01012345678")
//        @NotBlank(message = "Phone number is required.")
        @Size(min = 10, max = 11, message = "Phone number must contain 10 to 11 digits.")
        @Pattern(regexp = "\\d{10,11}", message = "Phone number must contain digits only.")
        String phone,

        @Schema(description = "생년월일", example = "1998-08-08")
        LocalDate birthDate,

        @Schema(description = "성별: MALE / FEMALE")
        User.Gender gender,

        @Schema(description = "지역", example = "서울")
        @Size(max = 50)
        String region,

        @Schema(
                description = "회원가입 관심사 전체 목록",
                example = "[\"LIVING_COST\", \"YOUTH_POLICY\"]"
        )
        List<String> signupInterests
) {}
