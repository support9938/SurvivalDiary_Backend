package com.survivaldiary.domain.user.social;

import com.survivaldiary.domain.user.entity.User;
import java.time.LocalDate;

public record SocialProfile(
        String providerUserId,
        String email,
        String name,
        User.Gender gender,
        Integer birthYear,
        LocalDate birthDate
) {

    public SocialProfile(String providerUserId, String email, String name) {
        this(providerUserId, email, name, null, null, null);
    }
}
