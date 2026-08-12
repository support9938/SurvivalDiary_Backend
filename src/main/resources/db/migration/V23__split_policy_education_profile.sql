ALTER TABLE user_policy_preferences
    MODIFY COLUMN education_status VARCHAR(30) NULL
        COMMENT 'ENROLLED / ON_LEAVE / EXPECTED_GRADUATION / GRADUATED / DROPPED_OUT / NOT_APPLICABLE',
    ADD COLUMN education_level VARCHAR(30) NULL COMMENT '교육 단계' AFTER education_status;

UPDATE user_policy_preferences
SET education_status = CASE education_status
    WHEN 'STUDENT' THEN 'ENROLLED'
    WHEN 'NOT_STUDENT' THEN 'NOT_APPLICABLE'
    WHEN 'OTHER' THEN 'NOT_APPLICABLE'
    ELSE education_status
END
WHERE education_status IS NOT NULL;
