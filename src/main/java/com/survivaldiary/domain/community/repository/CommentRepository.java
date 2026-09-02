package com.survivaldiary.domain.community.repository;

import com.survivaldiary.domain.community.entity.Comment;
import com.survivaldiary.domain.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    boolean existsByPostIdAndUserRole(Long postId, User.Role role);
}
