package com.survivaldiary.domain.user.service;

import com.survivaldiary.domain.user.dto.UpdateUserRequest;
import com.survivaldiary.domain.user.dto.UserResponse;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.entity.UserProfile;
import com.survivaldiary.domain.user.repository.UserProfileRepository;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfileImageStorage profileImageStorage;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = findUser(userId);
        return response(user);
    }

    @Transactional
    public UserResponse updateMe(Long userId, UpdateUserRequest request) {
        User user = findUser(userId);
        user.updateProfile(
                request.name().trim(),
                normalize(request.phone()),
                request.birthDate(),
                request.gender(),
                normalize(request.region())
        );

        UserProfile profile = getOrCreateProfile(userId);
        profile.updateBio(normalize(request.bio()));
        return UserResponse.from(user, profile);
    }

    @Transactional
    public UserResponse updateProfileImage(Long userId, MultipartFile image) {
        User user = findUser(userId);
        UserProfile profile = getOrCreateProfile(userId);
        String previousImageUrl = profile.getProfileImageUrl();
        String imageUrl = profileImageStorage.store(image);
        profile.updateProfileImageUrl(imageUrl);
        userProfileRepository.save(profile);
        profileImageStorage.delete(previousImageUrl);
        return UserResponse.from(user, profile);
    }

    @Transactional
    public UserResponse deleteProfileImage(Long userId) {
        User user = findUser(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null || profile.getProfileImageUrl() == null) {
            return UserResponse.from(user, profile);
        }
        String previousImageUrl = profile.getProfileImageUrl();
        profile.updateProfileImageUrl(null);
        profileImageStorage.delete(previousImageUrl);
        return UserResponse.from(user, profile);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private UserProfile getOrCreateProfile(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseGet(() -> userProfileRepository.save(UserProfile.create(userId)));
    }

    private UserResponse response(User user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        return UserResponse.from(user, profile);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
