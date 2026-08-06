CREATE TABLE user_hidden_policies (
  hidden_policy_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id          BIGINT UNSIGNED NOT NULL,
  policy_id        VARCHAR(100)    NOT NULL COMMENT '온통청년 정책 번호',
  title            VARCHAR(200)    NOT NULL COMMENT '숨길 당시 정책명',
  category         VARCHAR(100)    NULL COMMENT '숨길 당시 정책 분야',
  short_summary    VARCHAR(500)    NULL COMMENT '숨길 당시 목록 요약',
  hidden_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '관심 없음 설정 시각',
  PRIMARY KEY (hidden_policy_id),
  UNIQUE KEY uk_user_hidden_policies_user_policy (user_id, policy_id),
  KEY idx_user_hidden_policies_user_hidden_at (user_id, hidden_at),
  CONSTRAINT fk_user_hidden_policies_user FOREIGN KEY (user_id)
    REFERENCES users (user_id) ON DELETE CASCADE
) COMMENT '사용자별 관심 없음 정책';
