ALTER TABLE users
    ADD COLUMN nickname VARCHAR(50) NULL COMMENT '사용자 닉네임' AFTER name;
