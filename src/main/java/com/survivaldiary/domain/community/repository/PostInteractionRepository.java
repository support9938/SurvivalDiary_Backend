package com.survivaldiary.domain.community.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class PostInteractionRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public long likeCount(Long postId) {
        return ((Number) entityManager.createNativeQuery(
                "select count(*) from post_likes where post_id = :postId")
                .setParameter("postId", postId).getSingleResult()).longValue();
    }

    public long commentCount(Long postId) {
        return ((Number) entityManager.createNativeQuery(
                "select count(*) from comments where post_id = :postId")
                .setParameter("postId", postId).getSingleResult()).longValue();
    }

    public boolean likedBy(Long postId, Long userId) {
        return ((Number) entityManager.createNativeQuery(
                "select count(*) from post_likes where post_id = :postId and user_id = :userId")
                .setParameter("postId", postId).setParameter("userId", userId)
                .getSingleResult()).longValue() > 0;
    }

    public void toggleLike(Long postId, Long userId) {
        if (likedBy(postId, userId)) {
            entityManager.createNativeQuery("delete from post_likes where post_id = :postId and user_id = :userId")
                    .setParameter("postId", postId).setParameter("userId", userId).executeUpdate();
        } else {
            entityManager.createNativeQuery("insert into post_likes(post_id, user_id) values (:postId, :userId)")
                    .setParameter("postId", postId).setParameter("userId", userId).executeUpdate();
        }
    }
}
