-- 로그인 사용자의 맞춤 정책 기본 조건
CREATE TABLE user_policy_preferences (
  user_id             BIGINT UNSIGNED NOT NULL,
  region_code         CHAR(2)         NOT NULL COMMENT '법정동 시도 코드 앞 2자리',
  district_code       CHAR(5)         NULL COMMENT '법정동 시군구 코드 앞 5자리',
  employment_status   VARCHAR(30)     NOT NULL COMMENT 'EMPLOYED / JOB_SEEKING / UNEMPLOYED / STUDENT',
  income_range        VARCHAR(30)     NULL COMMENT 'BELOW_50 / BELOW_100 / BELOW_150 / NO_LIMIT',
  category            VARCHAR(30)     NULL COMMENT 'HOUSING / EMPLOYMENT / ASSET / CULTURE / TRANSPORT',
  created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_user_policy_preferences_user FOREIGN KEY (user_id)
    REFERENCES users (user_id) ON DELETE CASCADE
) COMMENT '사용자별 맞춤 정책 기본 조건';
