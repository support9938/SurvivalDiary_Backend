CREATE TABLE news_articles (
  news_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  external_id     VARCHAR(100)    NOT NULL COMMENT '수집처 기사 식별자',
  category        VARCHAR(30)     NOT NULL COMMENT '뉴스 카테고리',
  title           VARCHAR(300)    NOT NULL,
  summary         VARCHAR(500)    NOT NULL,
  source          VARCHAR(100)    NOT NULL,
  source_url      VARCHAR(1000)   NOT NULL,
  interest_codes  VARCHAR(500)    NOT NULL COMMENT '쉼표로 구분한 회원가입 관심사 코드',
  published_at    DATETIME        NOT NULL,
  active          BOOLEAN         NOT NULL DEFAULT TRUE,
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (news_id),
  UNIQUE KEY uk_news_articles_external_id (external_id),
  KEY idx_news_articles_active_published_at (active, published_at)
) COMMENT '맞춤 뉴스 기사';

INSERT INTO news_articles (
  external_id,
  category,
  title,
  summary,
  source,
  source_url,
  interest_codes,
  published_at
) VALUES
  (
    'seed-living-cost-1',
    'LIVING_ECONOMY',
    '생활비를 점검할 때 먼저 확인할 항목',
    '고정비와 변동비를 나누고 이번 달에 바로 줄일 수 있는 지출을 확인해 보세요.',
    '한국소비자원',
    'https://www.kca.go.kr/',
    'LIVING_COST,BUDGETING,FOOD_COST',
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 HOUR)
  ),
  (
    'seed-finance-1',
    'FINANCE',
    '금융상품을 비교하기 전에 살펴볼 기준',
    '금리뿐 아니라 중도해지 조건과 우대 조건까지 함께 비교하는 방법을 소개합니다.',
    '금융감독원 금융소비자정보포털',
    'https://fine.fss.or.kr/',
    'SAVING_INVESTMENT,LIVING_COST',
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 HOUR)
  ),
  (
    'seed-policy-1',
    'POLICY',
    '청년 지원 정책을 놓치지 않는 확인 방법',
    '신청 기간과 거주지 조건을 기준으로 필요한 청년 정책을 정리해 보세요.',
    '온통청년',
    'https://www.youthcenter.go.kr/',
    'GOVERNMENT_POLICY,BENEFIT,HOUSING_COST',
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 HOUR)
  ),
  (
    'seed-saving-1',
    'SAVING',
    '식비 예산을 오래 유지하는 작은 습관',
    '주간 식비 한도를 정하고 장보기 전에 남은 예산을 확인하는 습관을 만들어 보세요.',
    '한국소비자원',
    'https://www.kca.go.kr/',
    'FOOD_COST,LIVING_COST,BUDGETING',
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4 HOUR)
  ),
  (
    'seed-housing-1',
    'POLICY',
    '주거비 지원 정보를 찾을 때 확인할 조건',
    '연령과 소득, 거주 지역에 따라 달라지는 주거 지원 조건을 먼저 확인해 보세요.',
    '복지로',
    'https://www.bokjiro.go.kr/',
    'HOUSING_COST,GOVERNMENT_POLICY,BENEFIT',
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 HOUR)
  ),
  (
    'seed-income-1',
    'LIVING_ECONOMY',
    '부업 소득이 생겼을 때 기록해야 할 내용',
    '수입 날짜와 금액, 필요한 비용을 함께 기록하면 실제로 남는 금액을 확인하기 쉬워집니다.',
    '국세청',
    'https://www.nts.go.kr/',
    'SIDE_INCOME,BUDGETING',
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 6 HOUR)
  ),
  (
    'seed-benefit-1',
    'POLICY',
    '내가 받을 수 있는 복지 서비스를 찾는 방법',
    '가구 상황과 거주 지역을 기준으로 신청 가능한 복지 서비스를 확인해 보세요.',
    '복지로',
    'https://www.bokjiro.go.kr/',
    'BENEFIT,GOVERNMENT_POLICY',
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 HOUR)
  ),
  (
    'seed-budget-1',
    'SAVING',
    '가계부를 꾸준히 쓰기 위한 현실적인 기준',
    '모든 지출을 완벽하게 적기보다 큰 지출과 반복 지출부터 기록해 보세요.',
    '생존일기',
    'https://github.com/KwanEon/SurvivalDiary_WebFrontend',
    'BUDGETING,LIVING_COST,SAVING_INVESTMENT',
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 8 HOUR)
  );
