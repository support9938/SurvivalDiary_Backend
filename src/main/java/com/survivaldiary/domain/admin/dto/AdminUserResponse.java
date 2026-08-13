package com.survivaldiary.domain.admin.dto;

import com.survivaldiary.domain.user.entity.User;
import java.time.LocalDateTime;

public record AdminUserResponse(
        Long userId, String email, String name, String nickname, User.Role role, LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getName(), user.getNickname(), user.getRole(), user.getCreatedAt());
    }
}
