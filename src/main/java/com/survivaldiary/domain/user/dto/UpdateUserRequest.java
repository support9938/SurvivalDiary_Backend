package com.survivaldiary.domain.user.dto;

import com.survivaldiary.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateUserRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Size(max = 20, message = "휴대폰 번호는 20자 이하여야 합니다.")
        @Pattern(regexp = "^$|^[0-9+\\- ]+$", message = "휴대폰 번호 형식을 확인해 주세요.")
        String phone,

        @Past(message = "생년월일은 오늘보다 이전이어야 합니다.")
        LocalDate birthDate,

        User.Gender gender,

        @Size(max = 50, message = "지역은 50자 이하여야 합니다.")
        String region,

        @Size(max = 500, message = "소개는 500자 이하여야 합니다.")
        String bio
) {}
