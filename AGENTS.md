# Project identity

- Survival Diary helps users record spending, manage budgets, discover saving opportunities, explore youth and living-policy information, and share practical money-saving knowledge through community features.
- Reference screenshots may be used for interaction patterns, motion, spacing, and layout while keeping implementation aligned with Survival Diary's economy, saving, policy, and household-budget purpose.

## Repository and delivery rules

- Repository: `https://github.com/support9938/SurvivalDiary_Backend`
- Scope: Spring Boot API and database migrations only. Keep Flutter app and web frontend changes in their respective repositories.
- Create every work branch as `{name}/{type}/{task}` using the actual owner name; for example, `alex/feat/email-signup-api`.
- Use Conventional Commit messages such as `feat: persist signup interests`.
- 커밋 메시지와 PR 제목·본문은 한글로 작성한다. Conventional Commit 접두사(`feat:`, `fix:`, `docs:` 등)를 사용할 때에도 접두사 뒤 작업 설명은 한글로 쓴다.
- Never commit or push directly to `main`. Push the work branch to this repository and open a pull request targeting `main`.
- Validate schema changes with a new Flyway migration and run the relevant Gradle checks before delivery.

# AGENTS.md — 생존일기 API 서버 (Survival Diary Web)

AI 코딩 도구(ChatGPT Codex, Claude Code 등)가 이 저장소에서 작업할 때 참조하는 정본 문서.
이 문서는 **작업 규칙과 구조**에 집중한다. DB 스키마 상세는 `docs/schema-spec.md` 참조.

---

## 1. 프로젝트 정의

| 항목 | 내용 |
|---|---|
| 역할 | 생존일기(청년 경제 자립 지원 앱)의 **API 서버**. 화면 없음 — 클라이언트는 Flutter 앱(`SurvivalDiary_App`) |
| 스택 | Spring Boot 4.1.0 / Java 17 / Gradle Wrapper |
| DB | MySQL 8.x (`survival_diary`) — 스키마 정본은 **Flyway 마이그레이션** |
| 인증 | 자체 JWT (액세스+리프레시) 예정(#5) — 현재 BCrypt 회원가입까지 구현 |
| 문서 | springdoc(Swagger) — 서버 기동 후 http://localhost:8080/swagger-ui.html |
| 배포 예정 | AWS EC2 + RDS. 접속 정보·시크릿은 **환경변수로만** 주입 |

---

## 2. 작업 규칙 (필수 준수)

1. **스키마 정본은 Flyway.** `src/main/resources/db/migration/V{n}__{설명}.sql` 로만 변경한다.
   **이미 적용된 마이그레이션 파일은 절대 수정 금지** — 변경은 새 버전(V2, V3...) 추가로만.
   `jpa.hibernate.ddl-auto: validate` 를 유지한다 (JPA가 DDL을 만들지 않는다).
2. **모든 API 응답은 `ApiResponse<T>`**, 목록은 `PageResponse<T>` 를 사용한다 (아래 4장).
   Flutter 파싱 코드를 공유하기 위한 팀 규약이므로 예외 없이 지킨다.
3. **예외는 `BusinessException` + `ErrorCode`** 로만 던진다. HTTP 상태로 변환은
   `GlobalExceptionHandler` 가 담당한다. 에러 코드는 `{도메인 접두사}{3자리}` —
   C(공통) / U(사용자·인증) / P(게시글) / E(지출·예산) / Y(정책) / L(장소).
   도메인 작업 시 담당자가 본인 접두사 아래 코드를 추가한다.
4. **비밀번호는 BCrypt 해시만 저장.** 평문 저장·로깅 금지. JWT 시크릿·DB 비밀번호 등
   민감 값은 코드/커밋에 넣지 말고 환경변수(`DB_URL`/`DB_USERNAME`/`DB_PASSWORD` 등)로 주입한다.
   `application-dev.yml` 의 기본값은 팀 공용 로컬 기준값이다.
5. **DTO는 record + jakarta validation.** 요청/응답 필드에는 Swagger `@Schema`,
   엔드포인트에는 `@Operation` 을 달아 문서만으로 API 계약을 확인할 수 있게 한다.
6. **패키지 구조 준수**: `domain/<도메인>/{controller,service,repository,entity,dto}` +
   `global/{config,common,exception}`. 신규 도메인은 `domain/user` 를 견본으로 삼는다.
7. **한국어**: 주석·에러 메시지·API 설명은 한국어, 코드 식별자는 영어.
8. 변경 후 `./gradlew build` 가 통과해야 한다 (Windows는 `gradlew.bat build`).

---

## 3. 깃 공유 규칙

`SurvivalDiary_App` 과 동일한 규칙을 쓴다.

1. **브랜치는 `{이름}/{타입}/{작업명}` 형식** (예: `kimin/feat/login-jwt`).
   타입은 커밋 접두사와 동일하게 `feat`/`fix`/`docs`/`refactor`/`chore` 등.
2. 브랜치의 `{이름}`은 **현재 깃에 로그인 되어 있는 계정 이름**으로 사용한다.
3. **커밋 메시지는 Conventional Commits**: `feat: 로그인 API 구현`, `docs: 스키마 명세 갱신`.
   관련 이슈가 있으면 본문이나 제목 끝에 `(#5)` 처럼 참조한다.
4. **`main` 직커밋 금지 — PR로만 머지한다.** PR 본문에는 개요 / 작업 내용 / 테스트 방법을 적는다.
   기능 단위로 브랜치를 짧게 유지하고, 머지 후 브랜치는 삭제한다.
5. 주의: `kimin/feat` 처럼 **상위 경로와 같은 이름의 브랜치가 이미 있으면 하위 브랜치를
   만들 수 없다** (git ref 충돌). 항상 3단 전체 경로로 브랜치를 만든다.

---

## 4. API 공통 규약

### 4-1. 응답 포맷 — `global/common/ApiResponse.java`

```json
// 성공
{ "success": true, "data": { ... } }

// 실패
{ "success": false, "error": { "code": "U001", "message": "이미 사용 중인 이메일입니다." } }
```

### 4-2. 페이징 — `global/common/PageResponse.java`

요청: `?page=0&size=20&sort=createdAt,desc` (page 0부터, size 기본 20·최대 100)

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3, "hasNext": true }
```

사용 예: `PageResponse.from(repository.findAll(pageable).map(Dto::from))`

### 4-3. 현재 정의된 에러 코드 — `global/exception/ErrorCode.java`

| 코드 | HTTP | 의미 |
|---|---|---|
| C001 | 400 | 입력값 검증 실패 |
| C002 | 401 | 인증 필요 |
| C003 | 403 | 접근 권한 없음 |
| C004 | 404 | 리소스 없음 |
| C005 | 500 | 서버 오류 |
| U001 | 409 | 이메일 중복 |
| U002 | 401 | 로그인 실패 (이메일/비밀번호 불일치) |
| U003 | 401 | 유효하지 않은 토큰 |
| U004 | 401 | 만료된 토큰 |
| U005 | 404 | 사용자 없음 |

---

## 5. 디렉터리 구조

```
src/main/java/com/survivaldiary/
├─ SurvivalDiaryApplication.java
├─ global/
│  ├─ config/        SecurityConfig(BCrypt, 인증 예외 경로) · SwaggerConfig(JWT bearer)
│  ├─ common/        ApiResponse · PageResponse · HealthController(GET /health)
│  └─ exception/     ErrorCode · BusinessException · GlobalExceptionHandler
├─ domain/
│  ├─ user/          회원가입 구현됨 — 신규 도메인의 견본
│  │  ├─ controller/ AuthController (POST /api/auth/signup)
│  │  ├─ service/    AuthService
│  │  ├─ repository/ UserRepository
│  │  ├─ entity/     User (Gender/Role enum 포함)
│  │  └─ dto/        SignupRequest
│  └─ diary · policy · place · post · image   (담당자별 작업 예정)
└─ resources/
   ├─ application.yml        공통 (profiles.default=dev, ddl-auto=validate)
   ├─ application-dev.yml    로컬 개발용 datasource (환경변수로 덮어쓰기 가능)
   └─ db/migration/          V1__init.sql (16개 테이블 + 카테고리 시드)
```

인증 예외 경로(permitAll): `/health`, `/api/auth/**`, Swagger 경로. 나머지는 인증 필요.

---

## 6. 로컬 실행

MySQL 8.x가 로컬 3306에 떠 있고 `survival_diary` 데이터베이스가 있어야 한다
(`CREATE DATABASE survival_diary CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`).
스키마는 서버 기동 시 Flyway가 자동 적용한다.

```bash
./gradlew bootRun
```

접속 정보가 기본값(root/root1234)과 다르면 환경변수로 주입한다:
`DB_PASSWORD=내비밀번호 ./gradlew bootRun` (Windows PowerShell: `$env:DB_PASSWORD='내비밀번호'; .\gradlew.bat bootRun`)

- 헬스체크: GET http://localhost:8080/health → `{"status":"UP"}`
- API 테스트는 **Swagger UI 사용을 권장** — Windows 콘솔 curl은 한글 body가 CP949로
  깨져 400/500이 날 수 있다 (UTF-8 파일 + `--data-binary @file` 로 우회 가능).
- 8080 포트가 이미 사용 중이면 이전 서버 프로세스가 남은 것 — 종료 후 재기동한다.

---

## 7. 이슈 트래커

작업 단위는 GitHub 이슈로 분배되어 있다 (`KwanEon/SurvivalDiary_WebBackend`).
관리자 웹 프론트엔드는 별도 저장소(`KwanEon/SurvivalDiary_WebFrontend`)에서 진행하며,
이슈도 백엔드/프론트엔드 저장소에 각각 구분해 등록한다.
서버 공통 세팅(#1~#4)은 완료. 인증(#5~#6) → 이미지 업로드(#7) → 커뮤니티(#8~#9) 순으로 진행하며,
커뮤니티 도메인 코드가 팀원 도메인 작업의 사용 예시가 된다.

---

## Git branch ownership rule

- Jade Cohen / ligr00vefe@naver.com 작업자는 `kimin`으로 식별한다.
- 모든 작업 브랜치는 반드시 `{name}/{type}/{task}` 형식을 사용한다.
- kimin 작업 브랜치는 반드시 `kimin/{type}/{task}` 형식을 사용한다.
- 허용 예시: `kimin/feat/signup-api`, `kimin/fix/auth-token`, `kimin/chore/initial-backend-snapshot`.
- `main`에는 절대 직접 커밋하거나 직접 push하지 않는다.
- 모든 변경 사항은 작업 브랜치에 push한 뒤 PR로만 `main`에 반영한다.
- 커밋 메시지는 Conventional Commits 형식을 사용한다. 예: `feat: add email signup api`.
