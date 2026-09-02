-- 임시로 작성된 user01의 정보 공유 게시글을 실제 사용 예시로 교체한다.
UPDATE posts p
JOIN users u ON u.user_id = p.user_id
SET p.title = '냉장고 식재료 유통기한 표로 장보기 횟수를 줄였어요',
    p.content = '냉장고 문에 식재료 이름과 유통기한을 적은 표를 붙이고, 일주일에 두 번 남은 재료를 확인하고 있어요. 장보기 전에 먼저 소비할 재료를 정하니 같은 식재료를 중복 구매하는 일이 줄었고 버리는 음식도 눈에 띄게 적어졌습니다.',
    p.hashtags = '냉장고관리\n식비절약\n장보기팁',
    p.view_count = 36
WHERE u.email = 'user01@survivaldiary.local'
  AND p.category = '정보 공유'
  AND p.title = '정보 공유'
  AND p.content = '내용';

-- 테스트 계정마다 서로 다른 주제의 절약 게시글을 1건씩 추가한다.
INSERT INTO posts
    (user_id, category, admin_inquiry, is_secret, title, content, hashtags,
     image_alignment, view_count, comments_disabled, comments_hidden, created_at)
SELECT u.user_id, seed.category, FALSE, FALSE, seed.title, seed.content,
       seed.hashtags, 'center', seed.view_count, FALSE, FALSE, seed.created_at
FROM (
    SELECT 'user01@survivaldiary.local' AS email, '자유게시판' AS category,
           '무지출 데이를 부담 없이 이어가는 세 가지 규칙' AS title,
           '처음에는 하루 종일 아무것도 사지 않으려고 해서 매번 실패했어요. 지금은 교통비와 이미 정해진 식비는 허용하고, 즉흥적인 간식과 온라인 쇼핑만 멈추는 방식으로 바꿨습니다. 전날 물병과 간식을 챙기고 사고 싶은 물건은 24시간 뒤 다시 보는 규칙을 더하니 일주일에 이틀은 꾸준히 지킬 수 있었어요.' AS content,
           '무지출데이\n소비습관\n생활비절약' AS hashtags, 58 AS view_count,
           TIMESTAMP '2026-08-29 19:10:00' AS created_at
    UNION ALL
    SELECT 'user02@survivaldiary.local', '정보 공유',
           '도서관 전자책과 구독 정리로 문화비를 줄이는 방법',
           '읽고 싶은 책은 먼저 지역 도서관 전자책 앱에서 검색하고, 없는 책만 구매 목록에 담아 월말에 한 번 주문하고 있어요. 사용 기록이 없는 전자책 구독도 해지했더니 독서량은 그대로인데 매달 나가던 구독료와 충동 구매가 함께 줄었습니다.',
           '전자책\n구독정리\n문화비절약', 74,
           TIMESTAMP '2026-08-30 14:20:00'
    UNION ALL
    SELECT 'user03@survivaldiary.local', '절약 인증',
           '안 쓰는 물건을 중고 거래해 4만 원을 생활비로 돌렸어요',
           '최근 6개월 동안 사용하지 않은 소형 가전과 운동용품을 정리해 중고 거래로 판매했습니다. 판매 금액 4만 원은 바로 다음 달 생활비 통장으로 옮겼고, 새 물건을 사기 전 비슷한 물건이 집에 없는지 확인하는 습관도 생겼어요.',
           '중고거래\n물건정리\n절약인증', 93,
           TIMESTAMP '2026-08-31 17:40:00'
) seed
JOIN users u ON u.email = seed.email
WHERE NOT EXISTS (
    SELECT 1
    FROM posts existing_post
    WHERE existing_post.user_id = u.user_id
      AND existing_post.title = seed.title
);
