package com.survivaldiary.domain.community.dto;

import com.survivaldiary.domain.community.entity.Comment;
import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        String content,
        LocalDateTime createdAt,
        String author,
        String nickname,
        boolean owner
) {
    public static CommentResponse from(Comment comment, Long userId) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUser().getName(),
                comment.getUser().getNickname(),
                comment.getUser().getId().equals(userId));
    }
}
