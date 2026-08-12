INSERT INTO users (email, password, name, nickname, phone, birth_date, gender, region, signup_interest, bio, role, created_at)
SELECT 'user01@survivaldiary.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '김절약', '절약하는 김씨', '010-2001-0001', '1998-03-14', 'FEMALE', '서울', 'LIVING_COST,FOOD_COST', '식비와 생활비를 꼼꼼하게 관리하는 테스트 사용자', 'USER', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'user01@survivaldiary.local');

INSERT INTO users (email, password, name, nickname, phone, birth_date, gender, region, signup_interest, bio, role, created_at)
SELECT 'user02@survivaldiary.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '이알뜰', '알뜰한 이씨', '010-2002-0002', '1996-07-22', 'MALE', '경기', 'BUDGETING,SAVING_INVESTMENT', '예산을 세우고 저축 습관을 만드는 테스트 사용자', 'USER', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'user02@survivaldiary.local');

INSERT INTO users (email, password, name, nickname, phone, birth_date, gender, region, signup_interest, bio, role, created_at)
SELECT 'user03@survivaldiary.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '박생활', '생활비 지킴이', '010-2003-0003', '2000-11-05', 'FEMALE', '부산', 'GOVERNMENT_POLICY,BENEFIT', '정책과 지원 혜택을 찾아보는 테스트 사용자', 'USER', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'user03@survivaldiary.local');
