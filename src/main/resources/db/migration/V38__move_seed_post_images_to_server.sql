-- 커뮤니티 더미 게시글 이미지를 DB Base64 대신 서버 정적 리소스로 연결한다.
-- 정적 파일은 src/main/resources/static/community-images/seed 에 포함된다.

INSERT INTO posts
    (user_id, category, admin_inquiry, is_secret, title, content, hashtags,
     image_urls, image_alignment, view_count, comments_disabled, comments_hidden, created_at)
SELECT u.user_id, '자유게시판', FALSE, FALSE, '영화 추천',
       '<img src="/community-images/seed/post-17-1.jpg"><div>꼭 보세요. 두 번 보세요.</div><div>세 번 보세요.</div>',
       '영화추천', '/community-images/seed/post-17-1.jpg', 'center', 0, FALSE, FALSE,
       TIMESTAMP '2026-08-31 18:00:00'
FROM users u
WHERE u.email = 'user01@survivaldiary.local'
  AND NOT EXISTS (SELECT 1 FROM posts WHERE title = '영화 추천');

UPDATE posts
SET content = CASE title
        WHEN '이번 주 장보기 예산 안에서 성공했어요' THEN
            '지난주 냉장고 재료를 먼저 확인하고 필요한 것만 장 봤더니 예산 5만원 안에서 일주일 식사를 해결했어요.<img src="/community-images/seed/post-4-1.jpg">'
        WHEN '월급날 바로 예산을 나누는 습관을 만들었어요' THEN
            '월급이 들어오면 저축과 고정비를 먼저 분리하고 남은 금액을 주간 예산으로 나누니 충동 지출이 줄었어요.<img src="/community-images/seed/post-7-1.jpg">'
        WHEN '한 달 동안 카페 지출을 절반으로 줄였습니다' THEN
            '<img src="/community-images/seed/post-8-1.jpg">텀블러를 챙기고 회사 근처 무료 커피를 활용했어요. 무조건 참기보다 대체할 방법을 정하니 오래 유지할 수 있었습니다.<img src="/community-images/seed/post-8-2.jpg">'
        WHEN '동네 장보기 할인 정보를 공유해요' THEN
            '전통시장 앱과 마트 앱의 할인 시간을 비교해 보니 같은 품목도 가격 차이가 꽤 컸어요. 다들 어떤 앱을 사용하시나요?<img src="/community-images/seed/post-11-1.jpg">'
        WHEN '이번 달 구독 서비스를 정리했습니다' THEN
            '<img src="/community-images/seed/post-12-1.jpg">최근 한 달 동안 사용하지 않은 구독 세 가지를 해지했어요. 다음 결제일을 캘린더에 표시해 두니 관리하기 편했습니다.'
        WHEN '냉장고 식재료 유통기한 표로 장보기 횟수를 줄였어요' THEN
            '<img src="/community-images/seed/post-16-1.jpg">냉장고 문에 식재료 이름과 유통기한을 적은 표를 붙이고, 일주일에 두 번 남은 재료를 확인하고 있어요. 장보기 전에 먼저 소비할 재료를 정하니 같은 식재료를 중복 구매하는 일이 줄었고 버리는 음식도 눈에 띄게 적어졌습니다.'
        WHEN '영화 추천' THEN
            '<img src="/community-images/seed/post-17-1.jpg"><div>꼭 보세요. 두 번 보세요.</div><div>세 번 보세요.</div>'
        WHEN '무지출 데이를 부담 없이 이어가는 세 가지 규칙' THEN
            '처음에는 하루 종일 아무것도 사지 않으려고 해서 매번 실패했어요. 지금은 교통비와 이미 정해진 식비는 허용하고, 즉흥적인 간식과 온라인 쇼핑만 멈추는 방식으로 바꿨습니다. 전날 물병과 간식을 챙기고 사고 싶은 물건은 24시간 뒤 다시 보는 규칙을 더하니 일주일에 이틀은 꾸준히 지킬 수 있었어요.<img src="/community-images/seed/post-27-1.jpg">'
        WHEN '도서관 전자책과 구독 정리로 문화비를 줄이는 방법' THEN
            '읽고 싶은 책은 먼저 지역 도서관 전자책 앱에서 검색하고, 없는 책만 구매 목록에 담아 월말에 한 번 주문하고 있어요. 사용 기록이 없는 전자책 구독도 해지했더니 독서량은 그대로인데 매달 나가던 구독료와 충동 구매가 함께 줄었습니다.<img src="/community-images/seed/post-28-1.jpg"><img src="/community-images/seed/post-28-2.jpg">'
        WHEN '안 쓰는 물건을 중고 거래해 4만 원을 생활비로 돌렸어요' THEN
            '최근 6개월 동안 사용하지 않은 소형 가전과 운동용품을 정리해 중고 거래로 판매했습니다. 판매 금액 4만 원은 바로 다음 달 생활비 통장으로 옮겼고, 새 물건을 사기 전 비슷한 물건이 집에 없는지 확인하는 습관도 생겼어요.<img src="/community-images/seed/post-29-1.jpg">'
        ELSE content
    END,
    image_urls = CASE title
        WHEN '이번 주 장보기 예산 안에서 성공했어요' THEN '/community-images/seed/post-4-1.jpg'
        WHEN '월급날 바로 예산을 나누는 습관을 만들었어요' THEN '/community-images/seed/post-7-1.jpg'
        WHEN '한 달 동안 카페 지출을 절반으로 줄였습니다' THEN '/community-images/seed/post-8-1.jpg\n/community-images/seed/post-8-2.jpg'
        WHEN '동네 장보기 할인 정보를 공유해요' THEN '/community-images/seed/post-11-1.jpg'
        WHEN '이번 달 구독 서비스를 정리했습니다' THEN '/community-images/seed/post-12-1.jpg'
        WHEN '냉장고 식재료 유통기한 표로 장보기 횟수를 줄였어요' THEN '/community-images/seed/post-16-1.jpg'
        WHEN '영화 추천' THEN '/community-images/seed/post-17-1.jpg'
        WHEN '무지출 데이를 부담 없이 이어가는 세 가지 규칙' THEN '/community-images/seed/post-27-1.jpg'
        WHEN '도서관 전자책과 구독 정리로 문화비를 줄이는 방법' THEN '/community-images/seed/post-28-1.jpg\n/community-images/seed/post-28-2.jpg'
        WHEN '안 쓰는 물건을 중고 거래해 4만 원을 생활비로 돌렸어요' THEN '/community-images/seed/post-29-1.jpg'
        ELSE image_urls
    END
WHERE title IN (
    '이번 주 장보기 예산 안에서 성공했어요',
    '월급날 바로 예산을 나누는 습관을 만들었어요',
    '한 달 동안 카페 지출을 절반으로 줄였습니다',
    '동네 장보기 할인 정보를 공유해요',
    '이번 달 구독 서비스를 정리했습니다',
    '냉장고 식재료 유통기한 표로 장보기 횟수를 줄였어요',
    '영화 추천',
    '무지출 데이를 부담 없이 이어가는 세 가지 규칙',
    '도서관 전자책과 구독 정리로 문화비를 줄이는 방법',
    '안 쓰는 물건을 중고 거래해 4만 원을 생활비로 돌렸어요'
);
