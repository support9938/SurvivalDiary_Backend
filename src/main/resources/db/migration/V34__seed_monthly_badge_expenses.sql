-- 2026년 9월 절약 뱃지 확인용 6~8월 지출 데이터
-- 7월 대비 8월 절감액: user01 10,500원 / user02 35,000원 / user03 55,000원

INSERT INTO monthly_budgets (user_id, budget_month, amount)
SELECT u.user_id, seed.budget_month, seed.amount
FROM (
    SELECT 'user02@survivaldiary.local' AS email, DATE '2026-06-01' AS budget_month, 650000 AS amount
    UNION ALL SELECT 'user02@survivaldiary.local', DATE '2026-07-01', 650000
    UNION ALL SELECT 'user02@survivaldiary.local', DATE '2026-08-01', 650000
    UNION ALL SELECT 'user03@survivaldiary.local', DATE '2026-06-01', 750000
    UNION ALL SELECT 'user03@survivaldiary.local', DATE '2026-07-01', 750000
    UNION ALL SELECT 'user03@survivaldiary.local', DATE '2026-08-01', 750000
) seed
JOIN users u ON u.email = seed.email
WHERE NOT EXISTS (
    SELECT 1 FROM monthly_budgets b
    WHERE b.user_id = u.user_id AND b.budget_month = seed.budget_month
);

-- V32의 user01 8월 합계 149,500원에 30,300원을 더해 179,800원으로 조정한다.
-- 7월 합계 190,300원보다 10,500원 적어 '든든한 한 끼' 뱃지가 표시된다.
INSERT INTO expenses
    (user_id, category_id, title, amount, spent_at, memo, payment_method, entry_type, created_at)
SELECT u.user_id, 4, '여름 운동화', 30300, TIMESTAMP '2026-08-18 19:20:00',
       '절약 뱃지 비교용 추가 지출', 'CARD', 'MANUAL', TIMESTAMP '2026-08-18 19:20:00'
FROM users u
WHERE u.email = 'user01@survivaldiary.local'
  AND NOT EXISTS (
      SELECT 1 FROM expenses e
      WHERE e.user_id = u.user_id
        AND e.spent_at = TIMESTAMP '2026-08-18 19:20:00'
        AND e.title = '여름 운동화'
  );

INSERT INTO expenses
    (user_id, category_id, title, amount, spent_at, memo, payment_method, entry_type, created_at)
SELECT u.user_id, seed.category_id, seed.title, seed.amount, seed.spent_at,
       seed.memo, 'CARD', 'MANUAL', seed.spent_at
FROM (
    -- user02: 6월 220,000원 / 7월 210,000원 / 8월 175,000원
    SELECT 'user02@survivaldiary.local' AS email, 1 AS category_id, '6월 식비' AS title, 65000 AS amount, TIMESTAMP '2026-06-05 18:30:00' AS spent_at, '월간 식비 합산' AS memo
    UNION ALL SELECT 'user02@survivaldiary.local', 3, '6월 교통비', 55000, TIMESTAMP '2026-06-10 09:00:00', '대중교통과 택시'
    UNION ALL SELECT 'user02@survivaldiary.local', 4, '6월 생활용품', 60000, TIMESTAMP '2026-06-17 20:10:00', '생활용품 장보기'
    UNION ALL SELECT 'user02@survivaldiary.local', 2, '6월 카페', 20000, TIMESTAMP '2026-06-22 14:00:00', '카페 이용'
    UNION ALL SELECT 'user02@survivaldiary.local', 5, '6월 기타 지출', 20000, TIMESTAMP '2026-06-27 16:00:00', '기타 생활비'
    UNION ALL SELECT 'user02@survivaldiary.local', 1, '7월 식비', 70000, TIMESTAMP '2026-07-05 18:30:00', '월간 식비 합산'
    UNION ALL SELECT 'user02@survivaldiary.local', 3, '7월 교통비', 50000, TIMESTAMP '2026-07-10 09:00:00', '대중교통과 택시'
    UNION ALL SELECT 'user02@survivaldiary.local', 4, '7월 생활용품', 50000, TIMESTAMP '2026-07-17 20:10:00', '생활용품 장보기'
    UNION ALL SELECT 'user02@survivaldiary.local', 2, '7월 카페', 20000, TIMESTAMP '2026-07-22 14:00:00', '카페 이용'
    UNION ALL SELECT 'user02@survivaldiary.local', 5, '7월 구독료', 20000, TIMESTAMP '2026-07-27 16:00:00', '정기 구독'
    UNION ALL SELECT 'user02@survivaldiary.local', 1, '8월 식비', 60000, TIMESTAMP '2026-08-05 18:30:00', '월간 식비 합산'
    UNION ALL SELECT 'user02@survivaldiary.local', 3, '8월 교통비', 45000, TIMESTAMP '2026-08-10 09:00:00', '대중교통 중심 이용'
    UNION ALL SELECT 'user02@survivaldiary.local', 4, '8월 생활용품', 40000, TIMESTAMP '2026-08-17 20:10:00', '필요한 물품만 구매'
    UNION ALL SELECT 'user02@survivaldiary.local', 2, '8월 카페', 15000, TIMESTAMP '2026-08-22 14:00:00', '카페 이용 줄이기'
    UNION ALL SELECT 'user02@survivaldiary.local', 5, '8월 기타 지출', 15000, TIMESTAMP '2026-08-27 16:00:00', '기타 생활비'

    -- user03: 6월 300,000원 / 7월 280,000원 / 8월 225,000원
    UNION ALL SELECT 'user03@survivaldiary.local', 1, '6월 식비', 95000, TIMESTAMP '2026-06-06 18:30:00', '월간 식비 합산'
    UNION ALL SELECT 'user03@survivaldiary.local', 3, '6월 교통비', 65000, TIMESTAMP '2026-06-11 09:00:00', '대중교통과 택시'
    UNION ALL SELECT 'user03@survivaldiary.local', 4, '6월 쇼핑', 80000, TIMESTAMP '2026-06-18 20:10:00', '의류와 생활용품'
    UNION ALL SELECT 'user03@survivaldiary.local', 2, '6월 카페', 30000, TIMESTAMP '2026-06-23 14:00:00', '카페 이용'
    UNION ALL SELECT 'user03@survivaldiary.local', 5, '6월 기타 지출', 30000, TIMESTAMP '2026-06-28 16:00:00', '기타 생활비'
    UNION ALL SELECT 'user03@survivaldiary.local', 1, '7월 식비', 90000, TIMESTAMP '2026-07-06 18:30:00', '월간 식비 합산'
    UNION ALL SELECT 'user03@survivaldiary.local', 3, '7월 교통비', 60000, TIMESTAMP '2026-07-11 09:00:00', '대중교통과 택시'
    UNION ALL SELECT 'user03@survivaldiary.local', 4, '7월 쇼핑', 70000, TIMESTAMP '2026-07-18 20:10:00', '의류와 생활용품'
    UNION ALL SELECT 'user03@survivaldiary.local', 2, '7월 카페', 30000, TIMESTAMP '2026-07-23 14:00:00', '카페 이용'
    UNION ALL SELECT 'user03@survivaldiary.local', 5, '7월 기타 지출', 30000, TIMESTAMP '2026-07-28 16:00:00', '기타 생활비'
    UNION ALL SELECT 'user03@survivaldiary.local', 1, '8월 식비', 75000, TIMESTAMP '2026-08-06 18:30:00', '집밥 비중 늘리기'
    UNION ALL SELECT 'user03@survivaldiary.local', 3, '8월 교통비', 50000, TIMESTAMP '2026-08-11 09:00:00', '대중교통 중심 이용'
    UNION ALL SELECT 'user03@survivaldiary.local', 4, '8월 쇼핑', 50000, TIMESTAMP '2026-08-18 20:10:00', '계획 구매'
    UNION ALL SELECT 'user03@survivaldiary.local', 2, '8월 카페', 25000, TIMESTAMP '2026-08-23 14:00:00', '카페 이용 줄이기'
    UNION ALL SELECT 'user03@survivaldiary.local', 5, '8월 기타 지출', 25000, TIMESTAMP '2026-08-28 16:00:00', '기타 생활비'
) seed
JOIN users u ON u.email = seed.email
WHERE NOT EXISTS (
    SELECT 1 FROM expenses e
    WHERE e.user_id = u.user_id
      AND e.spent_at = seed.spent_at
      AND e.title = seed.title
);
