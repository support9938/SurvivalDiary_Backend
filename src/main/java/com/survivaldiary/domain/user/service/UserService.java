package com.survivaldiary.domain.user.service;

import com.survivaldiary.domain.user.dto.UpdateUserRequest;
import com.survivaldiary.domain.user.dto.UpdateDefaultResidenceRequest;
import com.survivaldiary.domain.user.dto.UserResponse;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.entity.UserLocation;
import com.survivaldiary.domain.user.repository.UserLocationRepository;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.domain.savingbadge.service.SavingBadgeService;
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
    private final UserLocationRepository userLocationRepository;
    private final ProfileImageStorage profileImageStorage;
    private final SavingBadgeService savingBadgeService;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = findUser(userId);
        return responseFor(user);
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
        return responseFor(user);
    }

    @Transactional
    public UserResponse updateProfileImage(Long userId, MultipartFile image) {
        User user = findUser(userId);
        String previousImageUrl = user.getProfileImageUrl();
        String imageUrl = profileImageStorage.store(image);
        user.updateProfileImageUrl(imageUrl);
        profileImageStorage.delete(previousImageUrl);
        return responseFor(user);
    }

    @Transactional
    public UserResponse deleteProfileImage(Long userId) {
        User user = findUser(userId);
        if (user.getProfileImageUrl() == null) {
            return responseFor(user);
        }
        String previousImageUrl = user.getProfileImageUrl();
        user.updateProfileImageUrl(null);
        profileImageStorage.delete(previousImageUrl);
        return responseFor(user);
    }

    @Transactional
    public UserResponse updateDefaultResidence(Long userId, UpdateDefaultResidenceRequest request) {
        User user = findUser(userId);
        userLocationRepository.deleteByUserId(userId);
        UserLocation residence = userLocationRepository.save(new UserLocation(
                userId,
                request.address().trim(),
                request.latitude(),
                request.longitude()
        ));
        return UserResponse.from(user, residence, savingBadgeService.badgeFor(userId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UserResponse responseFor(User user) {
        return UserResponse.from(
                user,
                userLocationRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null),
                savingBadgeService.badgeFor(user.getId())
        );
    }
}
