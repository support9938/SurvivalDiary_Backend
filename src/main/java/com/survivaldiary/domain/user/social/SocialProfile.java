package com.survivaldiary.domain.user.social;

public record SocialProfile(
        String providerUserId,
        String email,
        String name
) {
}
