package com.survivaldiary.domain.user.repository;

import com.survivaldiary.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query("select u from User u where :query = '' or lower(u.email) like lower(concat('%', :query, '%')) or lower(u.nickname) like lower(concat('%', :query, '%')) order by u.createdAt desc")
    Page<User> searchForAdmin(@Param("query") String query, Pageable pageable);
}
