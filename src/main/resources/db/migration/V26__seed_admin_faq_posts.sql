-- 관리자 계정과 서비스 기본 Q&A를 등록한다.
-- 비밀번호는 운영 환경에서 반드시 변경한다. 초기 비밀번호: password
INSERT INTO users (email, password, name, nickname, role, created_at)
SELECT 'admin@survivaldiary.local',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '생존일기 운영자', '생존일기 운영팀', 'ADMIN', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@survivaldiary.local');

INSERT INTO posts (user_id, category, title, content, hashtags, image_alignment, view_count, created_at)
SELECT u.user_id, '질문', '예산을 처음 세울 때 어디서부터 시작하나요?',
       '지난달 지출을 식비, 교통비, 고정비처럼 큰 항목으로 나누고, 꼭 필요한 지출과 줄일 수 있는 지출을 구분해 보세요.',
       '예산관리\n절약방법', 'center', 0, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'admin@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '예산을 처음 세울 때 어디서부터 시작하나요?');

INSERT INTO posts (user_id, category, title, content, hashtags, image_alignment, view_count, created_at)
SELECT u.user_id, '질문', '식비를 줄이면서 건강하게 먹는 방법이 있나요?',
       '일주일 식단을 먼저 정하고 장보기 목록을 만드는 방법을 추천해요. 제철 식재료와 집에 있는 재료를 우선 활용해 보세요.',
       '식비절약\n생활비절약', 'center', 0, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'admin@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '식비를 줄이면서 건강하게 먹는 방법이 있나요?');

INSERT INTO posts (user_id, category, title, content, hashtags, image_alignment, view_count, created_at)
SELECT u.user_id, '질문', '고정비를 줄일 때 가장 먼저 확인할 것은 무엇인가요?',
       '통신비, 구독 서비스, 보험처럼 매달 자동으로 빠져나가는 항목을 먼저 확인하면 절약 효과를 빠르게 확인할 수 있어요.',
       '고정비절약\n절약습관', 'center', 0, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'admin@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '고정비를 줄일 때 가장 먼저 확인할 것은 무엇인가요?');
