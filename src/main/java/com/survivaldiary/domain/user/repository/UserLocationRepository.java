package com.survivaldiary.domain.user.repository;

import com.survivaldiary.domain.user.entity.UserLocation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

    Optional<UserLocation> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteByUserId(Long userId);
}
