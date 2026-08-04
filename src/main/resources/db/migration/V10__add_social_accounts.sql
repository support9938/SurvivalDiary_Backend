-- SNS 계정은 이메일이 아니라 공급자와 공급자 사용자 ID 조합으로 식별한다.
-- SNS 전용 사용자는 이메일 또는 비밀번호가 없을 수 있다.
ALTER TABLE users
  MODIFY COLUMN email VARCHAR(255) NULL,
  MODIFY COLUMN password VARCHAR(255) NULL;

CREATE TABLE social_accounts (
  social_account_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id           BIGINT UNSIGNED NOT NULL,
  provider          VARCHAR(20)     NOT NULL,
  provider_user_id  VARCHAR(255)    NOT NULL,
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (social_account_id),
  UNIQUE KEY uk_social_accounts_provider_user (provider, provider_user_id),
  KEY idx_social_accounts_user (user_id),
  CONSTRAINT fk_social_accounts_user FOREIGN KEY (user_id)
    REFERENCES users (user_id) ON DELETE CASCADE
) COMMENT '카카오·네이버 SNS 계정 연결';
