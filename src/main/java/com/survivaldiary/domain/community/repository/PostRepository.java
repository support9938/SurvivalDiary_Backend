package com.survivaldiary.domain.community.repository;

import com.survivaldiary.domain.community.entity.Post;
import com.survivaldiary.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByAdminInquiryFalseOrderByCreatedAtDescIdDesc(Pageable pageable);
    Page<Post> findAllByCategoryAndAdminInquiryFalseOrderByCreatedAtDescIdDesc(
            String category, Pageable pageable);
    Page<Post> findAllByAdminInquiryTrueOrderByCreatedAtDescIdDesc(Pageable pageable);
    Page<Post> findAllByUserIdAndCategoryOrderByCreatedAtDescIdDesc(
            Long userId, String category, Pageable pageable);
    Page<Post> findAllByCategoryAndUserRoleAndAdminInquiryFalseOrderByCreatedAtDescIdDesc(
            String category, User.Role userRole, Pageable pageable);
    @Query("select p from Post p where p.user.role = :userRole and p.adminInquiry = false order by p.createdAt desc, p.id desc")
    Page<Post> findCommunityPosts(@Param("userRole") User.Role userRole, Pageable pageable);

    @Query("select p from Post p where p.category = :category and p.user.role = :userRole and p.adminInquiry = false order by p.createdAt desc, p.id desc")
    Page<Post> findCommunityPostsByCategory(
            @Param("category") String category,
            @Param("userRole") User.Role userRole,
            Pageable pageable);

    @Query(value = "select p.* from posts p join users u on u.user_id = p.user_id "
            + "where u.role = 'USER' and p.admin_inquiry = false "
            + "order by (select count(*) from post_likes l where l.post_id = p.post_id) * 3 "
            + "+ (select count(*) from comments c where c.post_id = p.post_id) * 2 "
            + "+ p.view_count desc, p.created_at desc",
            countQuery = "select count(*) from posts p join users u on u.user_id = p.user_id where u.role = 'USER' and p.admin_inquiry = false",
            nativeQuery = true)
    Page<Post> findPopularCommunityPosts(Pageable pageable);

    boolean existsByCategoryAndTitle(String category, String title);

    @Query("select count(p) from Post p where p.adminInquiry = true and not exists "
            + "(select c.id from Comment c where c.post = p and c.user.role = :adminRole)")
    long countUnansweredAdminInquiries(@Param("adminRole") User.Role adminRole);
}
