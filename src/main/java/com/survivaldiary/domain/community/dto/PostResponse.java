package com.survivaldiary.domain.community.dto;

import com.survivaldiary.domain.community.entity.Post;
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
        boolean commentsDisabled, boolean commentsHidden
) {
    public static PostResponse from(Post post, long likes, long comments, long bookmarks,
                                    boolean liked, boolean bookmarked, boolean owner) {
        return new PostResponse(post.getId(), post.getCategory(), post.getTitle(), post.getContent(),
                post.getCreatedAt(), post.getUser().getName(), post.getUser().getNickname(),
                split(post.getImageUrls()), post.getImageAlignment(), split(post.getHashtags()),
                likes, comments, bookmarks, liked, bookmarked, owner, post.getUser().getRole(),
                post.isCommentsDisabled(), post.isCommentsHidden());
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\n")).filter(s -> !s.isBlank()).toList();
    }
}
