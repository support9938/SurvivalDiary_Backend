# 온통청년 정책 API 제공처 계약

## 1. 문서 목적

이 문서는 생존일기 백엔드가 온통청년 정책 API를 호출할 때 사용하는 외부 계약과 앱 정책 조건의 변환 원칙을 기록한다.
외부 API 응답 DTO와 생존일기 내부 REST API DTO를 분리하고, 공식 문서에서 확인되지 않은 값은 추측하지 않는 것을 원칙으로 한다.

- 확인일: 2026-07-31
- 관련 이슈: [#23 청년정책 공공데이터 조회와 맞춤 필터 연동](https://github.com/support9938/SurvivalDiary_Backend/issues/23)
- 제공기관: 한국고용정보원 온통청년
- 공식 이용방법: https://www.youthcenter.go.kr/cmnFooter/openapiIntro/oaiGuide
- 공식 제공목록: https://www.youthcenter.go.kr/cmnFooter/openapiIntro/oaiDoc
- 공식 이용약관: https://www.youthcenter.go.kr/cmnFooter/termsInfo
- 공식 코드정의서: https://www.youthcenter.go.kr/downloadform/API코드정보.xlsx

## 2. 확인 결과 요약

| 항목 | 확인 결과 | 상태 |
|---|---|---|
| 통신 방식 | HTTPS | 확정 |
| 정책 API | `GET https://www.youthcenter.go.kr/go/ythip/getPlcy` | 확정 |
| 인증 | 온통청년 인증키 신청 완료, 승인 대기 | 진행 중 |
| 인증키 파라미터 | `apiKeyNm` | 확정 |
| 반환 형식 | 요청 파라미터 `rtnType=xml` 또는 `rtnType=json` | 확정 |
| 목록·상세 구분 | `pageType=1` 목록, `pageType=2` 상세 | 확정 |
| 상세 식별자 | 문자열 `plcyNo` | 확정 |
| 페이지 파라미터 | `pageNum`, `pageSize` | 확정 |
| 페이지 기준 | 공식 예시는 `pageNum=1`부터 시작 | 성공 응답으로 재확인 필요 |
| 성공 응답 래퍼 | 공개 문서에 전체 예시가 없어 확인 불가 | 인증키 필요 |
| 호출 제한 | 공개 이용방법·제공목록에 수치가 없음 | 승인 안내에서 확인 필요 |
| 문자 인코딩 | 공개 문서만으로 명시 확인 불가 | 성공 응답 헤더로 확인 필요 |

공식 이용방법은 XML 전송을 안내하지만, 현재 정책 API 문서에는 `rtnType`으로 XML과 JSON을 모두 선택할 수 있다고 명시돼 있다.
1차 구현은 추가 XML 라이브러리가 필요 없는 JSON을 사용한다.

## 3. 인증키 관리

인증키는 저장소와 로그에 남기지 않는다.

```text
환경변수: YOUTH_POLICY_API_KEY
설정 키: policy.provider.api-key
```

설정 예시에는 환경변수 이름만 기록하고 기본 인증키를 제공하지 않는다.
요청 URL 전체를 로그로 남기면 쿼리 문자열의 인증키가 노출될 수 있으므로 호스트, 경로, 상태 코드만 기록한다.

2026-07-31 인증키 신청을 완료했으며 현재 승인을 기다리고 있다.
개발 환경에는 아직 `YOUTH_POLICY_API_KEY`가 등록돼 있지 않다.
공식 테스트용 예시 값 `testKey`로 확인한 인증 실패 응답은 다음과 같다.

```http
HTTP/1.1 403 Forbidden
Content-Type: application/json
```

```json
{
  "errorCode": "e001",
  "errorMsg": "invalid api key."
}
```

`rtnType=xml`을 요청해도 인증 실패 응답은 JSON으로 반환됐다.
따라서 오류 응답은 정상 응답의 `rtnType`과 무관하게 JSON일 수 있도록 처리한다.

## 4. 외부 요청 계약

### 4.1 목록 조회

```http
GET https://www.youthcenter.go.kr/go/ythip/getPlcy
  ?apiKeyNm=${YOUTH_POLICY_API_KEY}
  &pageNum=1
  &pageSize=20
  &pageType=1
  &rtnType=json
  &zipCd=11680
  &lclsfNm=일자리
```

문자열 검색값은 URL 인코딩하며, 선택하지 않은 조건은 빈 문자열 대신 파라미터 자체를 생략한다.

### 4.2 상세 조회

공식 문서는 목록과 상세에 같은 endpoint를 사용하고 `pageType`과 `plcyNo`로 구분한다.

```http
GET https://www.youthcenter.go.kr/go/ythip/getPlcy
  ?apiKeyNm=${YOUTH_POLICY_API_KEY}
  &pageType=2
  &plcyNo={정책번호}
  &rtnType=json
```

정책 번호는 숫자로 변환하지 않고 문자열로 전달한다.

### 4.3 공식 요청 파라미터

| 이름 | 타입 | 필수 | 설명 | 생존일기 사용 |
|---|---|---:|---|---|
| `apiKeyNm` | String | Y | 발급받은 인증키 | 환경변수 주입 |
| `pageNum` | Number | N | 페이지 번호 | 내부 0-based 페이지를 외부 값으로 변환 |
| `pageSize` | Number | N | 페이지 크기 | 최대값은 승인 안내에서 확인 필요 |
| `pageType` | String | N | `1` 목록, `2` 상세 | 목록·상세 요청 구분 |
| `plcyNo` | String | N | 정책 번호 | 상세 조회 |
| `rtnType` | String | N | `xml` 또는 `json` | `json` 고정 |
| `plcyKywdNm` | String | N | 쉼표로 구분한 정책 키워드명 | 선택적 검색 |
| `plcyExplnCn` | String | N | 정책 설명 검색 | 현재 미사용 |
| `plcyNm` | String | N | 정책명 검색 | 키워드 검색 후보 |
| `zipCd` | String | N | 쉼표로 구분한 법정 시군구 5자리 코드 | 지역 후보 조회 |
| `lclsfNm` | String | N | 쉼표로 구분한 정책 대분류명 | 카테고리 후보 조회 |
| `mclsfNm` | String | N | 쉼표로 구분한 정책 중분류명 | 세부 카테고리 후보 조회 |

외부 API에는 나이, 취업 상태, 소득 조건을 직접 전달하는 요청 파라미터가 없다.
이 조건들은 응답의 구조화 필드와 자유 형식 설명을 이용해 백엔드에서 후처리해야 한다.

## 5. 외부 응답 계약

공식 문서에는 정책 응답 필드 60개가 등록돼 있다.
성공 응답의 최상위 래퍼와 페이지 메타데이터는 인증키로 받은 실제 응답을 확보한 뒤 확정한다.

### 5.1 생존일기에서 우선 사용하는 필드

| 외부 필드 | 공식 의미 | 내부 사용 |
|---|---|---|
| `plcyNo` | 정책 번호 | `policyId` |
| `plcyNm` | 정책명 | `title` |
| `plcyExplnCn` | 정책 설명 내용 | `summary`, `description` 후보 |
| `lclsfNm` | 정책 대분류명 | 카테고리 변환 |
| `mclsfNm` | 정책 중분류명 | 세부 카테고리 변환 |
| `plcyKywdNm` | 정책 키워드명 | 검색·카테고리 보조 |
| `plcySprtCn` | 정책 지원 내용 | `supportText` |
| `zipCd` | 정책 거주지역 코드 | 지역 일치 판정 |
| `sprtTrgtMinAge` | 지원 대상 최소 연령 | 나이 판정 |
| `sprtTrgtMaxAge` | 지원 대상 최대 연령 | 나이 판정 |
| `sprtTrgtAgeLmtYn` | 지원 대상 연령 제한 여부 | 나이 제한 없음 처리 |
| `jobCd` | 정책 취업 요건 코드 | 취업 상태 판정 |
| `schoolCd` | 정책 학력 요건 코드 | 학생 상태 보조 판정 |
| `earnCndSeCd` | 소득 조건 구분 코드 | 소득 판정 가능 여부 |
| `earnMinAmt` | 소득 최소 금액 | 소득 범위 후보 |
| `earnMaxAmt` | 소득 최대 금액 | 소득 범위 후보 |
| `earnEtcCn` | 소득 기타 내용 | `incomeText`와 확인 필요 상태 |
| `aplyPrdSeCd` | 신청 기간 구분 코드 | 특정 기간·상시·마감 구분 |
| `aplyYmd` | 신청 기간 | `applicationPeriodText` |
| `plcyAplyMthdCn` | 정책 신청 방법 | `applicationMethod` |
| `sbmsnDcmntCn` | 제출 서류 내용 | `documents` |
| `aplyUrlAddr` | 신청 URL 주소 | 공식 신청 링크 후보 |
| `refUrlAddr1` | 참고 URL 주소 | 참고 링크 |
| `refUrlAddr2` | 참고 URL 주소 | 참고 링크 |
| `sprvsnInstCdNm` | 주관 기관 코드명 | `agency` |
| `operInstCdNm` | 운영 기관 코드명 | `operatingAgency` |
| `sprvsnInstPicNm` | 주관 기관 담당자명 | 연락처가 아니므로 별도 보관 |
| `operInstPicNm` | 운영 기관 담당자명 | 연락처가 아니므로 별도 보관 |
| `lastMdfcnDt` | 최종 수정 일시 | `sourceUpdatedAt` |

### 5.2 나머지 공식 필드

```text
bscPlanCycl
bscPlanPlcyWayNo
bscPlanFcsAsmtNo
bscPlanAsmtNo
pvsnInstGroupCd
plcyPvsnMthdCd
plcyAprvSttsCd
sprvsnInstCd
operInstCd
sprtSclLmtYn
bizPrdSeCd
bizPrdBgngYmd
bizPrdEndYmd
bizPrdEtcCn
srngMthdCn
etcMttrCn
sprtSclCnt
sprtArvlSeqYn
mrgSttsCd
addAplyQlfcCndCn
ptcpPrpTrgtCn
inqCnt
rgtrInstCd
rgtrInstCdNm
rgtrUpInstCd
rgtrUpInstCdNm
rgtrHghrkInstCd
rgtrHghrkInstCdNm
plcyMajorCd
sBizCd
frstRegDt
```

사용하지 않는 필드도 외부 DTO에는 받을 수 있지만, 내부 REST 응답에 그대로 노출하지 않는다.

## 6. 공식 코드

### 6.1 정책 대분류

```text
일자리
주거
교육
복지문화
참여권리
```

### 6.2 정책 중분류

```text
취업
재직자
창업
주택 및 거주지
기숙사
전월세 및 주거급여 지원
미래역량강화
교육비지원
온라인교육
취약계층 및 금융지원
건강
예술인지원
문화활동
청년참여
정책인프라구축
청년국제교류
권익보호
```

### 6.3 취업 요건 코드

| 코드 | 의미 |
|---|---|
| `0013001` | 재직자 |
| `0013002` | 자영업자 |
| `0013003` | 미취업자 |
| `0013004` | 프리랜서 |
| `0013005` | 일용근로자 |
| `0013006` | (예비)창업자 |
| `0013007` | 단기근로자 |
| `0013008` | 영농종사자 |
| `0013009` | 기타 |
| `0013010` | 제한없음 |

### 6.4 신청 기간 코드

| 코드 | 의미 |
|---|---|
| `0057001` | 특정기간 |
| `0057002` | 상시 |
| `0057003` | 마감 |

### 6.5 소득 조건 코드

| 코드 | 의미 |
|---|---|
| `0043001` | 무관 |
| `0043002` | 연소득 |
| `0043003` | 기타 |

## 7. 앱 조건 변환 원칙

### 7.1 지역

앱은 `regionCode` 2자리와 선택적인 `districtCode` 5자리를 사용한다.
온통청년은 `zipCd`에 법정 시군구 5자리 코드를 받는다.

1. `districtCode`가 있으면 해당 5자리 코드를 후보 조회에 사용한다.
2. `districtCode`가 없으면 시·도 전체를 표현하는 5자리 코드가 실제 API에서 허용되는지 확인한다.
3. 세종특별자치시는 앱에서 하위 구·군을 선택하지 않으므로 별도 매핑을 검증한다.
4. 전국 정책은 요청 코드에 임의의 `ALL` 값을 보내지 않고 응답 지역 범위로 판정한다.
5. 시·도 전체 코드와 전국 정책 포함 방식은 인증키 성공 응답으로 확인하기 전 확정하지 않는다.

### 7.2 취업 상태

| 앱 값 | 외부 코드 후보 | 처리 |
|---|---|---|
| `employed` | `0013001` | 재직자와 직접 비교 |
| `jobSeeker` | `0013003` | 미취업자 후보로 비교하되 자유 형식 조건 확인 |
| `unemployed` | `0013003` | 미취업자와 직접 비교 |
| `student` | `schoolCd` | `jobCd`만으로 판정하지 않음 |

`0013010 제한없음`은 모든 앱 취업 상태와 일치한다.
문자열 조건만 있어 확정할 수 없으면 제외하지 않고 `CHECK_REQUIRED`로 반환한다.

### 7.3 소득

앱은 중위소득 50%, 100%, 150% 구간을 사용하지만 온통청년 구조화 필드는 연소득 금액이다.
가구원 수와 기준 중위소득 기준연도가 없으면 두 체계를 정확히 변환할 수 없다.

- `0043001 무관`은 모든 앱 소득 선택과 일치한다.
- 연소득 최소·최대 금액이 있어도 중위소득 구간으로 임의 환산하지 않는다.
- 자유 형식 또는 변환 불가능한 조건은 `CHECK_REQUIRED`로 반환한다.
- 정확한 소득 필터가 필요하면 앱 입력 모델에 연소득·가구원 수·기준연도를 추가하는 별도 계약 변경이 필요하다.

### 7.4 카테고리

| 앱 값 | 외부 분류 후보 | 상태 |
|---|---|---|
| `housing` | 대분류 `주거` | 직접 매핑 |
| `employment` | 대분류 `일자리` | 직접 매핑 |
| `culture` | 중분류 `예술인지원`, `문화활동` | 제한 매핑 |
| `asset` | 중분류 `취약계층 및 금융지원`, 관련 키워드 | 규칙 검증 필요 |
| `transport` | 공식 대·중분류에 직접 대응 없음 | 키워드 또는 앱 분류 변경 필요 |

교통과 자산형성을 근거 없이 넓은 분류에 포함하지 않는다.
실제 데이터 분포를 확인한 뒤 변환표를 확정한다.

## 8. 내부 응답 변환 주의사항

### 지원금

공식 응답에는 구조화된 총 지원금 필드가 없다.
`plcySprtCn`은 설명 문자열이고 `sprtSclCnt`는 지원 규모 수이므로 금액으로 사용하지 않는다.

- `supportText`: `plcySprtCn`
- `supportAmount`: 1차 구현에서는 `null`

금액 구조가 공식적으로 확인되기 전에는 문자열에서 숫자를 추출해 예상 총액을 만들지 않는다.

### 마감일

공식 응답에는 구조화된 신청 종료일 필드가 없고 `aplyYmd` 신청 기간 문자열과 `aplyPrdSeCd`만 있다.

- `applicationPeriodText`: `aplyYmd`
- `applicationEndDate`: 안전하게 날짜 범위를 파싱한 경우만 설정
- 상시 접수: 종료일 `null`
- 마감 또는 해석 불가: 원문을 보존하고 종료일 `null`

### 공식 URL

`aplyUrlAddr`는 신청 URL이고 `refUrlAddr1`, `refUrlAddr2`는 참고 URL이다.
온통청년 상세 페이지 URL 생성 규칙은 공개 API 문서에서 확인되지 않았으므로 정책 번호로 임의 생성하지 않는다.

## 9. 페이징과 필터 전략

온통청년 요청 파라미터만으로 나이·취업·소득 조건을 모두 필터링할 수 없다.
외부 페이지 한 장을 받은 뒤 필터링하면 내부 `totalElements`와 `hasNext`가 부정확해질 수 있다.

1차 구현 전에 다음 중 하나를 결정해야 한다.

1. 호출 제한 안에서 여러 외부 페이지를 조회한 후 내부에서 필터링·페이징한다.
2. 정책 데이터를 주기적으로 동기화해 내부 DB에서 필터링한다.
3. 목록은 외부 API가 지원하는 지역·분류 검색만 적용하고 나머지는 `CHECK_REQUIRED`로 표시한다.

인증키의 호출 제한과 실제 전체 건수 필드를 확인하기 전에는 1번의 최대 조회 페이지를 정하지 않는다.

## 10. 오류 처리 계약

| 상황 | 백엔드 변환 |
|---|---|
| 잘못된 앱 필터 | `Y004 INVALID_POLICY_FILTER`, HTTP 400 |
| 정책 없음 | `Y001 POLICY_NOT_FOUND`, HTTP 404 |
| 연결·읽기 타임아웃 | `Y002 POLICY_PROVIDER_UNAVAILABLE`, HTTP 503 |
| 제공처 5xx | `Y002 POLICY_PROVIDER_UNAVAILABLE`, HTTP 503 |
| 인증키 거절 | 서버 설정 문제로 기록하고 안전한 외부 장애 메시지 반환 |
| JSON 구조 오류·필수 식별자 누락 | `Y003 POLICY_PROVIDER_BAD_RESPONSE`, HTTP 502 |
| 빈 목록 | 성공 응답의 빈 페이지 |

외부 `errorCode`, `errorMsg` 원문과 인증키가 사용자 응답이나 일반 로그에 그대로 노출되지 않게 한다.

## 11. 단계 2 진입 전 확인 항목

- [x] 공식 정책 endpoint 확인
- [x] 공식 요청 파라미터 12개 확인
- [x] 공식 응답 필드 60개 확인
- [x] JSON·XML 선택 가능 여부 확인
- [x] 잘못된 인증키의 403 JSON 응답 확인
- [x] 코드정의서의 취업·소득·신청 기간·정책 분류 확인
- [x] 온통청년 인증키 신청
- [ ] 온통청년 인증키 승인
- [ ] 성공 목록 JSON 원문 확보
- [ ] 성공 상세 JSON 원문 확보
- [ ] 성공 응답 Content-Type과 문자 인코딩 확인
- [ ] 성공 응답 래퍼와 전체 건수·페이지 필드 확인
- [ ] 일일·초당 호출 제한 확인
- [ ] 시·도 전체 및 세종 `zipCd` 요청 방식 확인
- [ ] 정책 읽기 API의 비로그인 공개 여부 합의

인증키 승인 전에도 설정 클래스, 외부 클라이언트 인터페이스, 오류 변환, 문서 기반 fixture 테스트는 시작할 수 있다.
다만 성공 DTO와 페이징 구현은 실제 성공 응답을 확보하기 전 완료로 처리하지 않는다.

## 12. 단계 2 구현 상태

인증키 승인 전에 검증 가능한 제공처 연동 기반을 먼저 구현했다.

- [x] `policy.provider.*` 설정과 `YOUTH_POLICY_API_KEY` 환경변수 연결
- [x] 연결 2초·읽기 5초 타임아웃 설정
- [x] `RestClient` 기반 목록 요청(`pageType=1`) 구현
- [x] 문자열 정책 번호를 사용하는 상세 요청(`pageType=2`) 구현
- [x] 선택하지 않은 검색 조건의 쿼리 파라미터 생략
- [x] 인증키 누락·거절, 제공처 5xx, 잘못된 JSON의 내부 오류 변환
- [x] 공식 필드의 원문 타입을 보존하는 외부 정책 DTO
- [x] 요청 계약·오류 처리·fixture 역직렬화 테스트
- [ ] 실제 성공 목록 응답 래퍼 DTO 확정
- [ ] 실제 성공 상세 응답 래퍼 DTO 확정
- [ ] 제공처 응답을 앱용 정책 DTO로 변환
- [ ] 앱에서 호출할 내부 정책 REST API 구현

승인된 인증키는 파일에 저장하지 않고 실행 환경에 다음과 같이 등록한다.

```text
YOUTH_POLICY_API_KEY={승인된 인증키}
```

성공 응답 래퍼와 페이지 메타데이터가 확인되기 전까지 클라이언트는 JSON 트리를 반환한다.
승인 후 실제 목록·상세 응답 원문을 확보하면 외부 응답 래퍼 DTO로 교체하고 내부 변환 단계로 진행한다.
