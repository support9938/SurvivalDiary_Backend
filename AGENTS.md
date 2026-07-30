# AGENTS.md

## Repository and delivery rules

- Repository: `https://github.com/KwanEon/SurvivalDiary_WebFrontend`
- Scope: web frontend only. Keep Flutter app and Spring Boot API changes in their respective repositories.
- Create every work branch as `{name}/{type}/{task}` using the actual owner name; for example, `alex/docs/repository-rules`.
- Use Conventional Commit messages such as `docs: document repository delivery rules`.
- Never commit or push directly to `main`. Push the work branch to this repository and open a pull request targeting `main`.
- Do not commit generated build output, local editor settings, or secret files.

이 문서는 Codex, Claude Code를 포함한 AI 에이전트와 팀원이 공통으로 따라야 할 작업 규칙입니다.

## 프로젝트 목표

- 생존일기 웹 프런트엔드는 청년의 지출 관리, 정책 탐색, 절약 장소 검색, 정보 공유를 돕습니다.
- 팀은 신입 개발자로 구성되어 있으므로 이해하기 쉬운 React 기본 기능을 우선합니다.
- 복잡한 상태 관리, 과도한 추상화, AI 추천, 실시간 처리 기능은 명시적으로 합의하기 전까지 추가하지 않습니다.

## 현재 상태

- React + TypeScript + Vite UI 프로토타입입니다.
- 실제 인증, 저장, API 호출은 아직 구현하지 않았습니다.
- 백엔드는 Spring Boot, 모바일 앱은 Flutter입니다.

## 작업 전 확인

1. 루트 `README.md`의 기능 범위와 우선순위를 읽습니다.
2. 맡은 GitHub Issue와 기능 폴더를 확인합니다.
3. 작업 중인 다른 브랜치가 같은 파일을 수정하는지 확인합니다.
4. 기능 범위를 넓혀야 하면 임의로 구현하지 말고 별도 이슈를 제안합니다.

## 기능 소유 경계

| 기능         | 수정 기본 범위                       |
| ------------ | ------------------------------------ |
| 홈·하루 예산 | `src/features/dashboard/**`          |
| 지출 등록    | `src/features/expense-entry/**`      |
| 지출 통계    | `src/features/expense-statistics/**` |
| 정책 추천    | `src/features/policies/**`           |
| 절약 지도    | `src/features/savings-map/**`        |
| 커뮤니티     | `src/features/community/**`          |

- 담당자는 원칙적으로 자신의 기능 폴더만 수정합니다.
- 다른 기능 폴더 내부의 컴포넌트, 타입, 목업 데이터를 직접 import하지 않습니다.
- 두 기능에서 공통으로 쓰일 코드가 생기면 먼저 중복을 허용합니다. 실제로 두 곳 이상에서 안정적으로 사용된 뒤 `src/shared` 이동을 검토합니다.
- 라우트 추가가 필요할 때만 `src/app/App.tsx`를 최소 범위로 수정합니다.

## 스타일 규칙

- 색상, 여백, 반경, 그림자는 `src/shared/styles/tokens.css` 변수를 사용합니다.
- 기능별 CSS 클래스는 기능 이름 접두사를 사용합니다.
  - 예: `policy-card`, `savings-map__marker`, `community-post`
- 기능 CSS는 해당 기능의 `styles` 폴더에 둡니다.
- 공통 토큰에 없는 값이 필요하면 기존 변수로 표현할 수 있는지 먼저 확인합니다.
- 접근성을 위해 버튼에는 `type`을 명시하고 아이콘 단독 버튼에는 `aria-label`을 추가합니다.
- 데스크톱뿐 아니라 760px 이하 화면도 깨지지 않도록 작성합니다.

## TypeScript·React 규칙

- 새 코드는 `.ts`, `.tsx`만 사용합니다.
- `any`를 사용하지 않습니다.
- 페이지는 `pages`, 목업 데이터는 `mocks.ts`, 기능 전용 스타일은 `styles`로 구분합니다.
- UI 상태는 먼저 `useState`로 해결합니다. 여러 페이지에서 실제로 공유되기 전에는 전역 상태 라이브러리를 추가하지 않습니다.
- 서버 데이터는 기능 폴더 안의 `api`와 `types`에 둡니다.
- 컴포넌트가 너무 커질 때만 `components` 폴더로 분리합니다.
- 버튼과 입력을 실제 기능처럼 보이게 만들더라도 API 계약이 정해지기 전 임의의 서버 호출을 추가하지 않습니다.

## API 연동 규칙

- API 기본 주소는 `VITE_API_BASE_URL`을 사용합니다.
- 공공데이터 API는 React에서 직접 호출하지 않고 Spring Boot를 거칩니다.
- API 키, 토큰, 개인정보를 저장소에 커밋하지 않습니다.
- 금액은 정수 원 단위, 날짜는 ISO 8601 형식을 사용합니다.
- Flutter 결제 알림 내역은 Flutter → Spring Boot → React 순서로 전달합니다.

## 브랜치와 커밋

- 브랜치: `이름/feat/기능명`, `이름/fix/수정명`
- 한 브랜치에서 한 기능 또는 한 이슈만 처리합니다.
- 커밋 예:
  - `feat(policy): 정책 상세 화면 추가`
  - `fix(map): 모바일 필터 줄바꿈 수정`
  - `docs: 지출 API 계약 문서 추가`
- 공통 파일 변경은 기능 변경과 별도 커밋으로 분리합니다.
- 생성 파일, 빌드 결과물, `.env`는 커밋하지 않습니다.

## 완료 기준

- GitHub Issue의 완료 조건을 모두 충족합니다.
- 맡은 기능 폴더 밖의 불필요한 변경이 없습니다.
- TypeScript 오류가 없습니다.
- 데스크톱과 모바일에서 주요 UI가 겹치지 않습니다.
- 빈 목록, 로딩, 오류 상태가 필요한 기능은 각 상태 UI를 포함합니다.
- README 또는 이슈에 API 의존성과 미완료 범위를 기록합니다.
