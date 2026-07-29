-- ============================================================
-- V2: 리프레시 토큰 저장 테이블 (이슈 #5 로그인 + JWT)
-- 서버측 저장으로 강제 로그아웃·토큰 무효화에 대비한다
-- 주의: 적용된 마이그레이션 파일은 수정 금지. 변경은 새 버전(V3, V4...) 추가로만.
-- ============================================================

-- 리프레시 토큰 (RefreshToken)
CREATE TABLE refresh_tokens (
  token_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  token       VARCHAR(512)    NOT NULL COMMENT '리프레시 토큰 값 (JWT)',
  expires_at  DATETIME        NOT NULL COMMENT '만료 시각',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급일',
  PRIMARY KEY (token_id),
  UNIQUE KEY uk_refresh_tokens_token (token),
  KEY idx_refresh_tokens_user (user_id),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
    REFERENCES users (user_id) ON DELETE CASCADE
) COMMENT '리프레시 토큰';
