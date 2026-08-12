-- user01의 2026년 6~8월 생활비·결제 테스트 데이터
-- 카테고리 기본 ID: 1 식비, 2 카페, 3 교통, 4 쇼핑, 5 기타

INSERT INTO budgets (user_id, budget_date, amount)
SELECT u.user_id, seed.budget_date, seed.amount
FROM (
    SELECT DATE '2026-06-01' AS budget_date, 35000 AS amount
    UNION ALL SELECT DATE '2026-07-01', 38000
    UNION ALL SELECT DATE '2026-08-01', 40000
) seed
JOIN users u ON u.email = 'user01@survivaldiary.local'
WHERE NOT EXISTS (
    SELECT 1 FROM budgets b
    WHERE b.user_id = u.user_id AND b.budget_date = seed.budget_date
);

INSERT INTO monthly_budgets (user_id, budget_month, amount)
SELECT u.user_id, seed.budget_month, seed.amount
FROM (
    SELECT DATE '2026-06-01' AS budget_month, 900000 AS amount
    UNION ALL SELECT DATE '2026-07-01', 950000
    UNION ALL SELECT DATE '2026-08-01', 1000000
) seed
JOIN users u ON u.email = 'user01@survivaldiary.local'
WHERE NOT EXISTS (
    SELECT 1 FROM monthly_budgets b
    WHERE b.user_id = u.user_id AND b.budget_month = seed.budget_month
);

INSERT INTO expenses
    (user_id, category_id, title, amount, spent_at, memo, payment_method, entry_type, created_at)
SELECT u.user_id, seed.category_id, seed.title, seed.amount, seed.spent_at,
       seed.memo, seed.payment_method, 'MANUAL', seed.spent_at
FROM (
    SELECT 1 AS category_id, '회사 근처 점심' AS title, 8500 AS amount,
           TIMESTAMP '2026-06-03 12:22:00' AS spent_at, '김치찌개와 공기밥' AS memo, 'CARD' AS payment_method
    UNION ALL SELECT 2, '아메리카노', 4500, TIMESTAMP '2026-06-04 08:41:00', '출근길 테이크아웃', 'CARD'
    UNION ALL SELECT 3, '교통카드 충전', 30000, TIMESTAMP '2026-06-06 09:10:00', '6월 대중교통 충전', 'CARD'
    UNION ALL SELECT 4, '생활용품 장보기', 27600, TIMESTAMP '2026-06-08 19:35:00', '세제, 휴지, 샴푸', 'CARD'
    UNION ALL SELECT 1, '저녁 삼겹살', 32000, TIMESTAMP '2026-06-12 20:18:00', '친구와 외식', 'CARD'
    UNION ALL SELECT 5, '약국', 12800, TIMESTAMP '2026-06-15 18:03:00', '감기약과 비타민', 'CARD'
    UNION ALL SELECT 2, '카페 라떼', 5200, TIMESTAMP '2026-06-20 14:27:00', '주말 공부', 'CASH'
    UNION ALL SELECT 1, '마트 식재료', 43800, TIMESTAMP '2026-06-27 17:44:00', '계란, 채소, 닭가슴살', 'CARD'
    UNION ALL SELECT 1, '회사 근처 점심', 9000, TIMESTAMP '2026-07-01 12:16:00', '비빔밥', 'CARD'
    UNION ALL SELECT 2, '아메리카노', 4500, TIMESTAMP '2026-07-03 08:35:00', '출근길 테이크아웃', 'CARD'
    UNION ALL SELECT 3, '택시', 11400, TIMESTAMP '2026-07-05 22:11:00', '늦은 귀가', 'CARD'
    UNION ALL SELECT 4, '여름 옷 쇼핑', 68900, TIMESTAMP '2026-07-07 18:52:00', '셔츠와 반바지', 'CARD'
    UNION ALL SELECT 1, '배달 치킨', 23900, TIMESTAMP '2026-07-11 19:26:00', '주말 저녁', 'CARD'
    UNION ALL SELECT 5, '온라인 구독', 9900, TIMESTAMP '2026-07-15 09:00:00', '생활 서비스 정기결제', 'CARD'
    UNION ALL SELECT 1, '냉면', 11000, TIMESTAMP '2026-07-19 13:08:00', '주말 점심', 'CASH'
    UNION ALL SELECT 1, '마트 장보기', 51700, TIMESTAMP '2026-07-26 16:43:00', '일주일 식재료', 'CARD'
    UNION ALL SELECT 1, '회사 근처 점심', 8500, TIMESTAMP '2026-08-01 12:19:00', '된장찌개', 'CARD'
    UNION ALL SELECT 2, '아메리카노', 4500, TIMESTAMP '2026-08-03 08:38:00', '출근길 테이크아웃', 'CARD'
    UNION ALL SELECT 3, '교통카드 충전', 30000, TIMESTAMP '2026-08-05 09:02:00', '8월 대중교통 충전', 'CARD'
    UNION ALL SELECT 1, '샐러드 점심', 12500, TIMESTAMP '2026-08-07 12:31:00', '건강 관리 식단', 'CARD'
    UNION ALL SELECT 4, '생필품 온라인 주문', 34200, TIMESTAMP '2026-08-09 21:17:00', '주방 세제와 수건', 'CARD'
    UNION ALL SELECT 1, '친구 생일 저녁', 46500, TIMESTAMP '2026-08-10 19:42:00', '저녁 식사와 케이크', 'CARD'
    UNION ALL SELECT 5, '세탁소', 8500, TIMESTAMP '2026-08-11 18:26:00', '겨울 이불 세탁', 'CASH'
    UNION ALL SELECT 2, '카페 아이스티', 4800, TIMESTAMP '2026-08-12 15:05:00', '오후 간식', 'CARD'
) seed
JOIN users u ON u.email = 'user01@survivaldiary.local'
WHERE NOT EXISTS (
    SELECT 1 FROM expenses e
    WHERE e.user_id = u.user_id AND e.spent_at = seed.spent_at AND e.title = seed.title
);
