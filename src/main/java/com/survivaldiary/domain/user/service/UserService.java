package com.survivaldiary.domain.user.service;

import com.survivaldiary.domain.user.dto.UpdateUserRequest;
import com.survivaldiary.domain.user.dto.UserResponse;
import com.survivaldiary.domain.user.entity.User;
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
    private final ProfileImageStorage profileImageStorage;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = findUser(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMe(Long userId, UpdateUserRequest request) {
        User user = findUser(userId);
        user.updateProfile(
                request.name().trim(),
                normalize(request.phone()),
                request.birthDate(),
                request.gender(),
                normalize(request.region()),
                normalize(request.bio())
        );
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfileImage(Long userId, MultipartFile image) {
        User user = findUser(userId);
        String previousImageUrl = user.getProfileImageUrl();
        String imageUrl = profileImageStorage.store(image);
        user.updateProfileImageUrl(imageUrl);
        profileImageStorage.delete(previousImageUrl);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse deleteProfileImage(Long userId) {
        User user = findUser(userId);
        if (user.getProfileImageUrl() == null) {
            return UserResponse.from(user);
        }
        String previousImageUrl = user.getProfileImageUrl();
        user.updateProfileImageUrl(null);
        profileImageStorage.delete(previousImageUrl);
        return UserResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
