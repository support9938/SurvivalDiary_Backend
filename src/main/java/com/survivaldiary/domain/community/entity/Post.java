package com.survivaldiary.domain.community.entity;

import com.survivaldiary.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "admin_inquiry", nullable = false)
    private boolean adminInquiry;

    @Column(name = "is_secret", nullable = false)
    private boolean secret;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(length = 1000)
    private String hashtags;

    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    @Column(name = "image_alignment", nullable = false, length = 20)
    private String imageAlignment;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "comments_disabled", nullable = false)
    private boolean commentsDisabled;

    @Column(name = "comments_hidden", nullable = false)
    private boolean commentsHidden;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Post(User user, String category, String title, String content,
                 String hashtags, String imageUrls, String imageAlignment,
                 boolean commentsDisabled, boolean commentsHidden,
                 boolean adminInquiry, boolean secret) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.content = content;
        this.hashtags = hashtags;
        this.imageUrls = imageUrls;
        this.imageAlignment = imageAlignment == null ? "center" : imageAlignment;
        this.commentsDisabled = commentsDisabled;
        this.commentsHidden = commentsHidden;
        this.adminInquiry = adminInquiry;
        this.secret = secret;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public void update(String category, String title, String content, String hashtags,
                       String imageUrls, String imageAlignment,
                       boolean commentsDisabled, boolean commentsHidden,
                       boolean adminInquiry, boolean secret) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.hashtags = hashtags;
        this.imageUrls = imageUrls;
        this.imageAlignment = imageAlignment == null ? "center" : imageAlignment;
        this.commentsDisabled = commentsDisabled;
        this.commentsHidden = commentsHidden;
        this.adminInquiry = adminInquiry;
        this.secret = secret;
    }
}
