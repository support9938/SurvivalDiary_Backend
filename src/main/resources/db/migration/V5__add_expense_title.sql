ALTER TABLE expenses
  ADD COLUMN title VARCHAR(100) NULL COMMENT '지출 내용'
  AFTER category_id;

UPDATE expenses
SET title = COALESCE(NULLIF(LEFT(TRIM(memo), 100), ''), '지출 내역')
WHERE title IS NULL;

ALTER TABLE expenses
  MODIFY COLUMN title VARCHAR(100) NOT NULL COMMENT '지출 내용';
