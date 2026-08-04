-- 맞춤 정책 조건을 공식 분류와 설명형 추천에 맞게 확장한다.
ALTER TABLE user_policy_preferences
  MODIFY COLUMN employment_status VARCHAR(30) NULL
    COMMENT '이전 앱 호환용 취업 상태',
  ADD COLUMN work_status VARCHAR(30) NULL
    COMMENT 'EMPLOYED / SELF_EMPLOYED / UNEMPLOYED / FREELANCER / DAILY_WORKER / PROSPECTIVE_FOUNDER / SHORT_TERM_WORKER / FARMER / OTHER'
    AFTER district_code,
  ADD COLUMN job_seeking TINYINT(1) NULL
    COMMENT '현재 구직 여부. 모르면 NULL'
    AFTER work_status,
  ADD COLUMN education_status VARCHAR(30) NULL
    COMMENT 'STUDENT / ON_LEAVE / GRADUATED / NOT_STUDENT / OTHER'
    AFTER job_seeking;

UPDATE user_policy_preferences
SET work_status = CASE employment_status
      WHEN 'EMPLOYED' THEN 'EMPLOYED'
      WHEN 'JOB_SEEKING' THEN 'UNEMPLOYED'
      WHEN 'UNEMPLOYED' THEN 'UNEMPLOYED'
      ELSE NULL
    END,
    job_seeking = CASE employment_status
      WHEN 'JOB_SEEKING' THEN 1
      ELSE NULL
    END,
    education_status = CASE employment_status
      WHEN 'STUDENT' THEN 'STUDENT'
      ELSE NULL
    END;

CREATE TABLE user_policy_interests (
  user_id        BIGINT UNSIGNED NOT NULL,
  interest_code VARCHAR(40)      NOT NULL,
  created_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, interest_code),
  CONSTRAINT fk_user_policy_interests_user FOREIGN KEY (user_id)
    REFERENCES users (user_id) ON DELETE CASCADE
) COMMENT '사용자별 맞춤 정책 관심 주제';

INSERT INTO user_policy_interests (user_id, interest_code)
SELECT user_id,
       CASE category
         WHEN 'HOUSING' THEN 'HOUSING'
         WHEN 'EMPLOYMENT' THEN 'EMPLOYMENT'
         WHEN 'CULTURE' THEN 'WELFARE_CULTURE'
         WHEN 'ASSET' THEN 'ASSET_BUILDING'
         WHEN 'TRANSPORT' THEN 'TRANSPORT'
       END
FROM user_policy_preferences
WHERE category IN ('HOUSING', 'EMPLOYMENT', 'CULTURE', 'ASSET', 'TRANSPORT');
