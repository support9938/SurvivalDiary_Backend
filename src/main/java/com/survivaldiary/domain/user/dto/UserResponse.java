package com.survivaldiary.domain.user.dto;

import com.survivaldiary.domain.user.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String email,
        String name,
        LocalDate birthDate,
        User.Gender gender,
        String region,
        User.Role role,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getBirthDate(),
                user.getGender(),
                user.getRegion(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
