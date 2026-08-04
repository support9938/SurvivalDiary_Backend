package com.survivaldiary.domain.policy.repository;

import com.survivaldiary.domain.policy.entity.PolicyPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyPreferenceRepository extends JpaRepository<PolicyPreference, Long> {
}
