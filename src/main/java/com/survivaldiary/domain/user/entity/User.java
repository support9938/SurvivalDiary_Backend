package com.survivaldiary.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true)
    private String email;

    /** BCrypt 해시만 저장 — 평문 저장·로깅 금지 */
    @Column
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 50)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(length = 20)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 50)
    private String region;

    @Column(name = "signup_interest", length = 255)
    private String signupInterest;

    @Column(length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private User(String email, String password, String name, String nickname, String profileImageUrl, String phone,
                 LocalDate birthDate, Integer birthYear, Gender gender, String region,
                 String signupInterest, String bio, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.phone = phone;
        this.birthDate = birthDate;
        this.birthYear = birthYear;
        this.gender = gender;
        this.region = region;
        this.signupInterest = signupInterest;
        this.bio = bio;
        this.role = role != null ? role : Role.USER;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void updateProfile(
            String name,
            String phone,
            LocalDate birthDate,
            Gender gender,
            String region,
            String bio) {
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.region = region;
        this.bio = bio;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateByAdmin(
            String name,
            String nickname,
            String phone,
            LocalDate birthDate,
            Gender gender,
            String region,
            String signupInterest,
            String bio) {
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.region = region;
        this.signupInterest = signupInterest;
        this.bio = bio;
    }

    public void promoteToAdmin() {
        this.role = Role.ADMIN;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateBootstrapProfile(
            String name, String nickname, String phone, LocalDate birthDate,
            Gender gender, String region, String signupInterest, String bio) {
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.region = region;
        this.signupInterest = signupInterest;
        this.bio = bio;
    }

    public enum Role { USER, ADMIN }

    public enum Gender { MALE, FEMALE }
}
