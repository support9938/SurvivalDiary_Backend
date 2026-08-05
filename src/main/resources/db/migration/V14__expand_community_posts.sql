ALTER TABLE posts
    ADD COLUMN hashtags VARCHAR(1000) NULL,
    ADD COLUMN image_urls TEXT NULL,
    ADD COLUMN image_alignment VARCHAR(20) NOT NULL DEFAULT 'center';

CREATE TABLE post_bookmarks (
    bookmark_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    post_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bookmark_id),
    UNIQUE KEY uk_post_bookmarks (post_id, user_id),
    CONSTRAINT fk_post_bookmarks_post FOREIGN KEY (post_id)
        REFERENCES posts (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_bookmarks_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
);
