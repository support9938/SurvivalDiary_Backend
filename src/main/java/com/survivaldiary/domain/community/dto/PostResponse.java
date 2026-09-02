package com.survivaldiary.domain.community.dto;

import com.survivaldiary.domain.community.entity.Post;
import com.survivaldiary.domain.savingbadge.dto.SavingBadgeResponse;
import com.survivaldiary.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record PostResponse(
        Long postId, String category, String title, String content,
        LocalDateTime createdAt, String author, String nickname,
        List<String> imageUrls, String imageAlignment, List<String> hashtags,
        long likeCount, long commentCount, long bookmarkCount,
        boolean liked, boolean bookmarked, boolean owner, User.Role authorRole,
        boolean commentsDisabled, boolean commentsHidden,
        boolean adminInquiry, boolean secret, boolean accessible, boolean answered,
        SavingBadgeResponse authorSavingBadge
) {
    public static PostResponse from(Post post, long likes, long comments, long bookmarks,
                                    boolean liked, boolean bookmarked, boolean owner,
                                    boolean accessible, boolean answered,
                                    SavingBadgeResponse authorSavingBadge) {
        return new PostResponse(post.getId(), post.getCategory(), post.getTitle(),
                accessible ? post.getContent() : "",
                post.getCreatedAt(), post.getUser().getName(), post.getUser().getNickname(),
                accessible ? split(post.getImageUrls()) : List.of(), post.getImageAlignment(),
                accessible ? split(post.getHashtags()) : List.of(),
                likes, comments, bookmarks, liked, bookmarked, owner, post.getUser().getRole(),
                post.isCommentsDisabled(), post.isCommentsHidden(), post.isAdminInquiry(),
                post.isSecret(), accessible, answered, authorSavingBadge);
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\n")).filter(s -> !s.isBlank()).toList();
    }
}
