-- 테스트 사용자별 실제 사용 흐름을 확인할 수 있는 커뮤니티 게시글을 등록한다.
INSERT INTO posts (user_id, category, title, content, hashtags, image_alignment, view_count, created_at)
SELECT u.user_id, '절약 인증', '이번 주 장보기 예산 안에서 성공했어요',
       '지난주 냉장고 재료를 먼저 확인하고 필요한 것만 장 봤더니 예산 5만원 안에서 일주일 식사를 해결했어요.',
       '식비절약\n절약인증', 'center', 42, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 DAY)
FROM users u
WHERE u.email = 'user01@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '이번 주 장보기 예산 안에서 성공했어요');

INSERT INTO posts (user_id, category, title, content, hashtags, image_alignment, view_count, created_at)
SELECT u.user_id, '정보 공유', '통신비를 낮춘 뒤 매달 2만원을 아꼈어요',
       '사용하지 않는 부가서비스를 정리하고 가족 결합 할인을 확인했어요. 요금제 변경 전에는 데이터 사용량을 꼭 비교해 보세요.',
       '고정비절약\n통신비', 'center', 87, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 DAY)
FROM users u
WHERE u.email = 'user01@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '통신비를 낮춘 뒤 매달 2만원을 아꼈어요');

INSERT INTO posts (user_id, category, title, content, hashtags, image_alignment, view_count, created_at)
SELECT u.user_id, '질문', '배달비를 줄이는 좋은 방법이 있을까요?',
       '퇴근 후 배달을 자주 시키게 되는데 식비와 배달비를 함께 줄일 수 있었던 방법이 있다면 알려주세요.',
       '식비절약\n생활습관', 'center', 31, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY)
FROM users u
WHERE u.email = 'user01@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '배달비를 줄이는 좋은 방법이 있을까요?');

INSERT INTO posts (user_id, category, title, content, hashtags, image_alignment, view_count, created_at)
SELECT u.user_id, '자유게시판', '월급날 바로 예산을 나누는 습관을 만들었어요',
       '월급이 들어오면 저축과 고정비를 먼저 분리하고 남은 금액을 주간 예산으로 나누니 충동 지출이 줄었어요.',
       '예산관리\n절약습관', 'center', 64, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 DAY)
FROM users u
WHERE u.email = 'user02@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '월급날 바로 예산을 나누는 습관을 만들었어요');

INSERT INTO posts (user_id, category, title, content, hashtags, image_alignment, view_count, created_at)
SELECT u.user_id, '절약 인증', '한 달 동안 카페 지출을 절반으로 줄였습니다',
       '텀블러를 챙기고 회사 근처 무료 커피를 활용했어요. 무조건 참기보다 대체할 방법을 정하니 오래 유지할 수 있었습니다.',
       '카페비절약\n절약인증', 'center', 108, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 DAY)
FROM users u
WHERE u.email = 'user02@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '한 달 동안 카페 지출을 절반으로 줄였습니다');

INSERT INTO posts (user_id, category, title, content, hashtags, image_alignment, view_count, created_at)
SELECT u.user_id, '질문', '사회초년생 저축 비율을 어떻게 정하면 좋을까요?',
       '처음 월급을 받기 시작해서 저축과 생활비 비율을 정하는 중이에요. 무리하지 않고 시작할 수 있는 기준이 궁금합니다.',
       '저축초보\n예산관리', 'center', 53, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4 DAY)
FROM users u
WHERE u.email = 'user02@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '사회초년생 저축 비율을 어떻게 정하면 좋을까요?');

INSERT INTO posts (user_id, category, title, content, hashtags, image_alignment, view_count, created_at)
SELECT u.user_id, '정보 공유', '놓치고 있던 청년 지원 정책을 찾았어요',
       '조건에 맞는 청년 지원 정책을 검색해 보니 생각보다 받을 수 있는 혜택이 많았어요. 지역과 나이를 먼저 입력해 보는 것을 추천합니다.',
       '청년정책\n지원금', 'center', 76, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 6 DAY)
FROM users u
WHERE u.email = 'user03@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '놓치고 있던 청년 지원 정책을 찾았어요');

INSERT INTO posts (user_id, category, title, content, hashtags, image_alignment, view_count, created_at)
SELECT u.user_id, '자유게시판', '동네 장보기 할인 정보를 공유해요',
       '전통시장 앱과 마트 앱의 할인 시간을 비교해 보니 같은 품목도 가격 차이가 꽤 컸어요. 다들 어떤 앱을 사용하시나요?',
       '장보기\n생활비절약', 'center', 39, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 DAY)
FROM users u
WHERE u.email = 'user03@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '동네 장보기 할인 정보를 공유해요');

INSERT INTO posts (user_id, category, title, content, hashtags, image_alignment, view_count, created_at)
SELECT u.user_id, '절약 인증', '이번 달 구독 서비스를 정리했습니다',
       '최근 한 달 동안 사용하지 않은 구독 세 가지를 해지했어요. 다음 결제일을 캘린더에 표시해 두니 관리하기 편했습니다.',
       '구독정리\n고정비절약', 'center', 91, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 8 DAY)
FROM users u
WHERE u.email = 'user03@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '이번 달 구독 서비스를 정리했습니다');

-- 테스트 사용자들이 서로의 글을 적당히 좋아요·북마크한 상태를 만든다.
INSERT INTO post_likes (post_id, user_id)
SELECT p.post_id, u.user_id
FROM posts p
JOIN users author ON author.user_id = p.user_id
JOIN users u ON u.email = CASE author.email
    WHEN 'user01@survivaldiary.local' THEN 'user02@survivaldiary.local'
    WHEN 'user02@survivaldiary.local' THEN 'user03@survivaldiary.local'
    WHEN 'user03@survivaldiary.local' THEN 'user01@survivaldiary.local'
END
WHERE author.email IN ('user01@survivaldiary.local', 'user02@survivaldiary.local', 'user03@survivaldiary.local')
  AND NOT EXISTS (SELECT 1 FROM post_likes l WHERE l.post_id = p.post_id AND l.user_id = u.user_id);

INSERT INTO post_likes (post_id, user_id)
SELECT p.post_id, u.user_id
FROM posts p
JOIN users author ON author.user_id = p.user_id
JOIN users u ON u.email = CASE author.email
    WHEN 'user01@survivaldiary.local' THEN 'user03@survivaldiary.local'
    WHEN 'user02@survivaldiary.local' THEN 'user01@survivaldiary.local'
    WHEN 'user03@survivaldiary.local' THEN 'user02@survivaldiary.local'
END
WHERE author.email IN ('user01@survivaldiary.local', 'user02@survivaldiary.local', 'user03@survivaldiary.local')
  AND p.title IN ('통신비를 낮춘 뒤 매달 2만원을 아꼈어요', '한 달 동안 카페 지출을 절반으로 줄였습니다', '놓치고 있던 청년 지원 정책을 찾았어요')
  AND NOT EXISTS (SELECT 1 FROM post_likes l WHERE l.post_id = p.post_id AND l.user_id = u.user_id);

INSERT INTO post_bookmarks (post_id, user_id)
SELECT p.post_id, u.user_id
FROM posts p
JOIN users author ON author.user_id = p.user_id
JOIN users u ON u.email = CASE author.email
    WHEN 'user01@survivaldiary.local' THEN 'user03@survivaldiary.local'
    WHEN 'user02@survivaldiary.local' THEN 'user01@survivaldiary.local'
    WHEN 'user03@survivaldiary.local' THEN 'user02@survivaldiary.local'
END
WHERE author.email IN ('user01@survivaldiary.local', 'user02@survivaldiary.local', 'user03@survivaldiary.local')
  AND p.category IN ('절약 인증', '정보 공유')
  AND NOT EXISTS (SELECT 1 FROM post_bookmarks b WHERE b.post_id = p.post_id AND b.user_id = u.user_id);

-- 테스트 사용자들이 서로 남긴 자연스러운 댓글 더미 데이터.
INSERT INTO comments (post_id, user_id, content, created_at)
SELECT p.post_id, u.user_id, seed.content, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL seed.days_ago DAY)
FROM (
    SELECT '이번 주 장보기 예산 안에서 성공했어요' AS title, 'user02@survivaldiary.local' AS email,
           '냉장고 재료를 먼저 확인하는 습관이 정말 도움이 되더라고요. 다음 주에도 성공하시길 응원해요!' AS content, 1 AS days_ago
    UNION ALL SELECT '이번 주 장보기 예산 안에서 성공했어요', 'user03@survivaldiary.local',
           '5만원으로 일주일 식사를 해결하다니 대단해요. 장보기 목록도 공유해 주시면 참고할게요.', 1
    UNION ALL SELECT '통신비를 낮춘 뒤 매달 2만원을 아꼈어요', 'user03@survivaldiary.local',
           '저도 부가서비스를 확인해 봐야겠어요. 매달 나가는 돈부터 줄이는 게 효과가 큰 것 같아요.', 4
    UNION ALL SELECT '배달비를 줄이는 좋은 방법이 있을까요?', 'user02@survivaldiary.local',
           '저는 주말에 미리 두 가지 메뉴를 만들어 두고 냉동해요. 평일 배달 주문이 많이 줄었습니다.', 1
    UNION ALL SELECT '배달비를 줄이는 좋은 방법이 있을까요?', 'user03@survivaldiary.local',
           '배달앱 알림을 끄고 냉장고에 있는 재료로 만들 수 있는 메뉴를 먼저 찾아보는 것도 좋아요.', 1
    UNION ALL SELECT '월급날 바로 예산을 나누는 습관을 만들었어요', 'user01@survivaldiary.local',
           '월급날 자동이체로 먼저 분리하는 방법이 실천하기 가장 쉬워 보여요. 저도 따라 해볼게요!', 2
    UNION ALL SELECT '한 달 동안 카페 지출을 절반으로 줄였습니다', 'user03@survivaldiary.local',
           '텀블러를 챙기는 것만으로도 꽤 큰 차이가 나네요. 대체 방법을 정하는 게 핵심인 것 같아요.', 6
    UNION ALL SELECT '사회초년생 저축 비율을 어떻게 정하면 좋을까요?', 'user01@survivaldiary.local',
           '처음부터 무리하기보다 고정비를 제외한 금액에서 꾸준히 가능한 비율로 시작하는 게 좋다고 들었어요.', 3
    UNION ALL SELECT '놓치고 있던 청년 지원 정책을 찾았어요', 'user01@survivaldiary.local',
           '지역과 나이를 먼저 입력해 보는 팁이 유용하네요. 저도 받을 수 있는 정책이 있는지 찾아봐야겠어요.', 5
    UNION ALL SELECT '동네 장보기 할인 정보를 공유해요', 'user02@survivaldiary.local',
           '전통시장 앱은 생각보다 할인 정보가 잘 모여 있더라고요. 좋은 정보 감사합니다!', 1
    UNION ALL SELECT '이번 달 구독 서비스를 정리했습니다', 'user01@survivaldiary.local',
           '결제일을 캘린더에 표시해 두는 방법이 좋네요. 무료 체험 후 자동 결제도 꼭 확인해야겠어요.', 7
    UNION ALL SELECT '이번 달 구독 서비스를 정리했습니다', 'user02@survivaldiary.local',
           '사용하지 않는 서비스는 해지하고 필요할 때만 다시 가입하는 게 가장 확실한 것 같아요.', 7
) seed
JOIN posts p ON p.title = seed.title
JOIN users u ON u.email = seed.email
WHERE NOT EXISTS (
    SELECT 1 FROM comments c
    WHERE c.post_id = p.post_id
      AND c.user_id = u.user_id
      AND c.content = seed.content
);
