-- 테스트 계정별 관리자 문의 1건과 일반 비밀글 1건을 추가한다.
INSERT INTO posts
    (user_id, category, admin_inquiry, is_secret, title, content, hashtags,
     image_alignment, view_count, comments_disabled, comments_hidden, created_at)
SELECT u.user_id, '질문', seed.admin_inquiry, seed.is_secret, seed.title,
       seed.content, NULL, 'center', 0, FALSE, FALSE, seed.created_at
FROM (
    SELECT 'user01@survivaldiary.local' AS email, TRUE AS admin_inquiry, FALSE AS is_secret,
           '결제 알림이 중복으로 기록돼요' AS title,
           '같은 카드 결제가 지출 내역에 두 번 등록되는 경우가 있습니다. 중복 내역을 안전하게 정리하는 방법과 자동 감지 기준을 확인해 주세요.' AS content,
           DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 6 HOUR) AS created_at
    UNION ALL
    SELECT 'user01@survivaldiary.local', FALSE, TRUE,
           '월세와 관리비 비중을 어떻게 조정해야 할까요?',
           '현재 소득에서 월세와 관리비가 차지하는 비중이 높아 구체적인 금액은 공개하지 않고 조언을 받고 싶습니다. 주거비 예산을 조정할 때 우선 확인할 항목이 궁금해요.',
           DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 HOUR)
    UNION ALL
    SELECT 'user02@survivaldiary.local', TRUE, FALSE,
           '소셜 로그인 계정을 기존 계정과 연결하고 싶어요',
           '카카오 로그인으로 만든 계정과 기존 이메일 계정의 지출 기록을 하나로 합칠 수 있는지 문의드립니다. 진행 전에 필요한 확인 절차도 알려주세요.',
           DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4 HOUR)
    UNION ALL
    SELECT 'user02@survivaldiary.local', FALSE, TRUE,
           '비상금 목표 금액을 어떻게 정하면 좋을까요?',
           '소득과 고정비를 기준으로 비상금 목표를 다시 세우고 있습니다. 개인 재정 상황이 드러날 수 있어 비밀글로 조언을 구합니다.',
           DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 HOUR)
    UNION ALL
    SELECT 'user03@survivaldiary.local', TRUE, FALSE,
           '맞춤 정책 추천 지역이 실제 거주지와 달라요',
           '마이페이지에는 현재 거주지를 등록했지만 다른 지역의 정책이 추천되고 있습니다. 거주지 설정과 정책 추천 조건을 확인해 주세요.',
           DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 HOUR)
    UNION ALL
    SELECT 'user03@survivaldiary.local', FALSE, TRUE,
           '가족 생활비 분담 기준이 고민입니다',
           '가족 구성원별 소득 차이가 있어 생활비 분담 비율을 정하기 어렵습니다. 개인 상황은 공개하지 않고 합리적인 기준에 대한 의견을 받고 싶어요.',
           DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 HOUR)
) seed
JOIN users u ON u.email = seed.email
WHERE NOT EXISTS (
    SELECT 1
    FROM posts p
    WHERE p.user_id = u.user_id
      AND p.title = seed.title
);

-- 답변 완료와 미답변 상태를 함께 확인할 수 있도록 user02 문의에만 관리자 답변을 등록한다.
INSERT INTO comments (post_id, user_id, content, created_at)
SELECT p.post_id, admin_user.user_id,
       '현재는 서로 다른 로그인 방식으로 생성된 계정을 자동으로 합치는 기능을 제공하지 않습니다. 본인 확인 후 기록 이전 가능 여부를 검토할 수 있도록 가입 이메일과 로그인 방식을 함께 알려주세요.',
       DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 210 MINUTE)
FROM posts p
JOIN users author ON author.user_id = p.user_id
JOIN users admin_user ON admin_user.email = 'admin@survivaldiary.local'
WHERE author.email = 'user02@survivaldiary.local'
  AND p.title = '소셜 로그인 계정을 기존 계정과 연결하고 싶어요'
  AND p.admin_inquiry = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM comments c
      WHERE c.post_id = p.post_id
        AND c.user_id = admin_user.user_id
  );
