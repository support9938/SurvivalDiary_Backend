package com.survivaldiary.domain.user.dto;

import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.entity.UserProfile;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String email,
        String name,
        String phone,
        LocalDate birthDate,
        Integer birthYear,
        User.Gender gender,
        String region,
        String signupInterest,
        String bio,
        String profileImageUrl,
        User.Role role,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user, UserProfile profile) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getBirthDate(),
                user.getBirthYear(),
                user.getGender(),
                user.getRegion(),
                user.getSignupInterest(),
                profile == null ? null : profile.getBio(),
                profile == null ? null : profile.getProfileImageUrl(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
