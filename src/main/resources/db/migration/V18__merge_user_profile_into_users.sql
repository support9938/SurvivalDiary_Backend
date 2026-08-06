ALTER TABLE users
    ADD COLUMN bio VARCHAR(500) NULL COMMENT '사용자 소개' AFTER signup_interest;

UPDATE users u
JOIN user_profiles up ON up.user_id = u.user_id
SET u.bio = up.bio,
    u.profile_image_url = CASE
        WHEN up.profile_image_url IS NOT NULL AND TRIM(up.profile_image_url) <> ''
            THEN up.profile_image_url
        ELSE u.profile_image_url
    END;

DROP TABLE user_profiles;
