package com.survivaldiary.domain.admin.dto;

import com.survivaldiary.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "관리자용 회원 상세 정보")
public record AdminUserDetailResponse(
        Long userId,
        String email,
        String name,
        String nickname,
        String profileImageUrl,
        String phone,
        LocalDate birthDate,
        Integer birthYear,
        User.Gender gender,
        String region,
        String signupInterest,
        String bio,
        User.Role role,
        LocalDateTime createdAt
) {
    public static AdminUserDetailResponse from(User user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getPhone(),
                user.getBirthDate(),
                user.getBirthYear(),
                user.getGender(),
                user.getRegion(),
                user.getSignupInterest(),
                user.getBio(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
