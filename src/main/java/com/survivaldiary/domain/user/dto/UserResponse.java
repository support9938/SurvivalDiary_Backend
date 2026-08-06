package com.survivaldiary.domain.user.dto;

import com.survivaldiary.domain.user.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
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

    public static UserResponse from(User user) {
        return new UserResponse(
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
