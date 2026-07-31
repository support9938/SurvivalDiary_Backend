# AGENTS.md

## 프로젝트 개요

- 생존일기는 사용자의 지출과 예산을 관리하고, 절약 정보·청년 정책·생활 정보를 탐색하며 커뮤니티에서 실용적인 정보를 공유하는 서비스다.
- 이 저장소는 Spring Boot REST API와 데이터베이스 마이그레이션을 담당한다. Flutter 앱과 웹 프론트엔드 변경은 각각의 저장소에서 작업한다.

## 저장소 정보

- Repository: `https://github.com/support9938/SurvivalDiary_Backend`
- 주요 경로: `src/main/java/`, `src/main/resources/`, `src/test/java/`
- 데이터베이스 변경은 Flyway 마이그레이션으로 관리한다.

## 작업 규칙

- 작업 전 `README.md`, 관련 GitHub Issue, 기존 API 계약을 확인한다.
- 브랜치는 `{name}/{type}/{task}` 형식을 사용한다. 예: `alex/feat/email-login-api`.
- 한 브랜치에는 하나의 기능 또는 하나의 이슈 범위만 담는다.
- `main`에 직접 커밋하거나 푸시하지 않는다. 작업 브랜치를 푸시하고 `main` 대상 PR로 반영한다.
- 커밋 메시지와 PR 제목·본문은 한글로 작성한다. Conventional Commit 접두사(`feat:`, `fix:`, `docs:` 등)를 사용할 때에도 접두사 뒤 설명은 한글로 쓴다.
- 환경 변수, 데이터베이스 비밀번호, JWT 비밀키, API 키는 커밋하지 않는다.

## 백엔드 구현 규칙

- 도메인별 `controller` → `service` → `repository` 흐름을 유지하고 요청·응답 DTO를 분리한다.
- API 응답과 예외는 공통 응답 규격을 사용한다.
- 인증이 필요한 API는 보안 설정과 `@AuthenticationPrincipal` 사용 방식을 일관되게 유지한다.
- 스키마 변경은 새 Flyway 마이그레이션 파일로 추가하고 기존 마이그레이션을 수정하지 않는다.

## 검증

- 관련 Gradle 테스트 또는 컴파일 검증을 실행한다.
- API·DB 변경은 성공, 입력 검증 실패, 권한 실패, 중복 요청의 동작을 확인한다.
