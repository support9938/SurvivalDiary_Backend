ALTER TABLE expenses
  ADD COLUMN notification_source VARCHAR(100) NULL COMMENT '자동 감지 결제 알림 출처'
    AFTER entry_type,
  ADD COLUMN detection_key VARCHAR(64) NULL COMMENT '사용자별 알림 중복 방지 키'
    AFTER notification_source,
  ADD UNIQUE KEY uk_expenses_user_detection (user_id, detection_key);
