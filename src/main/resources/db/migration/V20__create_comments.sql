CREATE TABLE IF NOT EXISTS comments (
    comment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    post_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id),
    KEY idx_comments_post (post_id),
    KEY idx_comments_user (user_id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id)
        REFERENCES posts (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
);
