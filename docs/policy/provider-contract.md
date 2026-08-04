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
| 인증 | 승인된 인증키로 목록 계약 테스트와 앱 통합 조회 성공 | 확정 |
| 인증키 파라미터 | `apiKeyNm` | 확정 |
| 반환 형식 | 요청 파라미터 `rtnType=xml` 또는 `rtnType=json` | 확정 |
| 목록·상세 구분 | `pageType=1` 목록, `pageType=2` 상세 | 확정 |
| 상세 식별자 | 문자열 `plcyNo` | 확정 |
| 페이지 파라미터 | `pageNum`, `pageSize` | 확정 |
| 페이지 기준 | `pageNum=1`부터 실제 목록 조회 성공 | MVP 확정 |
| 성공 응답 래퍼 | JSON 트리에서 정책 식별자를 안전하게 탐색 | 타입 DTO는 후속 |
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

2026-08-03 승인된 인증키로 서버의 실제 목록 계약 테스트와 앱의 목록·상세 통합 조회를 확인했다.
인증키는 서버 담당자의 실행 환경에만 등록하며 저장소와 일반 로그에는 기록하지 않는다.
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
2. `districtCode`가 없으면 2자리 `regionCode` 뒤에 `000`을 붙인 시·도 전체 코드를 사용한다.
3. 실제 계약 테스트에서 부산 전체 `26000`과 부산진구 `26230`을 각각 3페이지 조회했으며,
   두 요청 모두 60개를 반환하고 부산진구 결과 60개가 부산 전체 결과에 모두 포함됨을 확인했다.
4. 전국 정책은 요청 코드에 임의의 `ALL` 값을 보내지 않고 제공처 조회 결과와 응답 지역 범위로 판정한다.
5. 시군구를 선택한 경우 전국·시도 공통·선택한 시군구 정책만 포함하고 다른 시군구 전용 정책은 제외한다.
6. 세종을 포함한 다른 시·도의 전체 코드도 같은 형식을 사용하되 실제 데이터 변화는 명시적 계약 테스트로 확인한다.

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

### 10-1. 제공처 진단 로그

정책 제공처 호출 실패 시 인증키, 전체 요청 URL, 외부 응답 본문, 예외 메시지는 기록하지 않는다.
서버 로그의 `operation`, `reason`, `status`만으로 다음 원인을 구분한다.

| reason | 의미 | 우선 확인 항목 |
|---|---|---|
| `API_KEY_MISSING` | 실행 중인 서버 프로세스에 인증키가 없음 | `YOUTH_POLICY_API_KEY` 환경변수와 서버 재시작 |
| `AUTH_REJECTED` | 제공처가 인증 요청을 401 또는 403으로 거절 | 인증키 승인 상태와 발급 API 종류 |
| `PROVIDER_SERVER_ERROR` | 제공처가 5xx 응답을 반환 | 온통청년 장애 여부와 재시도 시점 |
| `CONNECT_TIMEOUT` | 제공처 연결 제한시간 초과 | 서버 PC의 DNS·방화벽·외부 통신 |
| `READ_TIMEOUT` | 연결 후 응답 제한시간 초과 | 제공처 응답 지연 여부 |
| `NETWORK_TIMEOUT` | 연결·읽기 단계를 구분할 수 없는 시간 초과 | 서버 네트워크와 제공처 응답 지연 |
| `DNS_FAILURE` | 제공처 도메인 해석 실패 | 서버 PC의 DNS 설정 |
| `CONNECTION_FAILURE` | 제공처 연결 거절 또는 네트워크 연결 실패 | 방화벽·프록시·외부 통신 |
| `RESOURCE_ACCESS_FAILURE` | 분류하지 못한 네트워크 접근 실패 | 서버 네트워크와 제공처 상태 |
| `NULL_RESPONSE` | 제공처 응답 본문이 없음 | 제공처 응답 상태와 계약 변경 여부 |
| `RESPONSE_PROCESSING_FAILURE` | 제공처 응답 변환 실패 | JSON 구조와 응답 Content-Type |
| `UNEXPECTED_HTTP_STATUS` | 별도로 분류하지 않은 HTTP 오류 | 상태 코드와 제공처 공지 |

`operation=SEARCH`는 목록 조회, `operation=DETAIL`은 상세 조회를 뜻한다.

## 11. 단계 2 진입 전 확인 항목

- [x] 공식 정책 endpoint 확인
- [x] 공식 요청 파라미터 12개 확인
- [x] 공식 응답 필드 60개 확인
- [x] JSON·XML 선택 가능 여부 확인
- [x] 잘못된 인증키의 403 JSON 응답 확인
- [x] 코드정의서의 취업·소득·신청 기간·정책 분류 확인
- [x] 온통청년 인증키 신청
- [x] 온통청년 인증키 승인
- [x] 실제 성공 목록의 최상위 필드와 정책 식별자 존재 확인
- [x] Android 앱을 통한 실제 목록·상세 응답 확인
- [ ] 성공 응답 Content-Type과 문자 인코딩 확인
- [ ] 성공 응답 래퍼와 전체 건수·페이지 필드 확인
- [ ] 일일·초당 호출 제한 확인
- [x] 시·도 전체 `zipCd` 요청 방식 확인(부산 `26000` 실제 계약 검증)
- [ ] 세종 `zipCd` 실제 응답 확인
- [ ] 정책 읽기 API의 비로그인 공개 여부 합의

MVP에서는 인증키와 정책 원문을 저장하지 않고 성공 응답의 구조와 정책 식별자만 안전한 계약 테스트로 확인한다.
정확한 래퍼 DTO, 전체 건수와 페이지 메타데이터는 디테일 단계에서 별도로 확정한다.

## 12. 단계 2 구현 상태

인증키 승인 전에 제공처 연동 기반을 구현하고, 승인 후 실제 목록·상세 흐름을 검증했다.

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
- [x] 제공처 응답을 앱용 정책 DTO로 변환
- [x] 앱에서 호출할 내부 정책 REST API 구현

승인된 인증키는 파일에 저장하지 않고 실행 환경에 다음과 같이 등록한다.

```text
YOUTH_POLICY_API_KEY={승인된 인증키}
```

MVP 클라이언트는 제공처 래퍼 변화에 대응할 수 있도록 JSON 트리에서 정책 식별자를 안전하게 탐색한다.
외부 응답 래퍼 DTO와 정확한 페이지 모델 교체는 내부 앱 API 계약을 유지한 채 디테일 단계에서 진행한다.

## 13. 단계 3 실시간 조회 계약

### 13.1 선택한 방식

맞춤 정책 1차 연동은 요청 시점마다 생존일기 백엔드가 온통청년 API를 호출한다.
외부 원문을 앱에 직접 반환하지 않고 `PolicySummary`, `PolicyDetail` 내부 DTO로 변환한다.
나중에 캐시나 DB 동기화 방식으로 변경해도 앱 API 계약을 유지하는 것이 목적이다.

맞춤 조건에는 나이·취업·소득 정보가 포함되므로 URL 쿼리 문자열 대신 JSON 본문을 사용한다.
정책 endpoint는 기존 보안 설정을 유지해 로그인 사용자만 호출할 수 있다.

```text
앱
  → POST /api/policies/search
  → 생존일기 백엔드
  → GET 온통청년 정책 API(최대 3페이지)
  → 조건 판정 및 내부 DTO 변환
  → ApiResponse<PolicySearchResponse>
```

### 13.2 맞춤 정책 검색

```http
POST /api/policies/search
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "age": 27,
  "regionCode": "11",
  "districtCode": "11680",
  "employmentStatus": "JOB_SEEKING",
  "incomeRange": "BELOW_100",
  "category": "HOUSING",
  "size": 20
}
```

| 필드 | 필수 | 규칙 |
|---|---:|---|
| `age` | Y | 만 18~39세 |
| `regionCode` | Y | 숫자 2자리 |
| `districtCode` | N | 숫자 5자리, 앞 2자리가 `regionCode`와 일치 |
| `employmentStatus` | Y | `EMPLOYED`, `JOB_SEEKING`, `UNEMPLOYED`, `STUDENT` |
| `incomeRange` | N | `BELOW_50`, `BELOW_100`, `BELOW_150`, `NO_LIMIT` |
| `category` | N | `HOUSING`, `EMPLOYMENT`, `ASSET`, `CULTURE`, `TRANSPORT` |
| `size` | N | 기본 20, 최대 20 |

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "policyId": "R202607310001",
        "category": "전월세 및 주거급여 지원",
        "categoryType": "HOUSING",
        "title": "청년 주거 지원",
        "summary": "정책 설명",
        "supportAmount": null,
        "supportText": "지원 내용",
        "applicationPeriodText": "20260701~20260731",
        "target": "만 19~34세",
        "agency": "주관 기관",
        "eligibilityStatus": "CHECK_REQUIRED",
        "eligibilityReasons": [
          "중위소득 조건을 공고문에서 확인해야 합니다."
        ]
      }
    ],
    "partialResult": true,
    "checkedProviderPages": 3
  }
}
```

`partialResult=true`는 요청한 개수보다 일치 결과가 많거나, 최대 3페이지를 확인한 뒤에도
온통청년에 다음 후보 페이지가 있을 가능성을 뜻한다.
외부 전체 건수 계약이 확인되지 않았으므로 내부 `totalElements`, `totalPages`를 임의로 만들지 않는다.

### 13.3 정책 상세

```http
GET /api/policies/{policyId}
Authorization: Bearer {accessToken}
```

정책 번호는 문자열로 유지한다.
상세 응답은 신청 방법, 제출 서류, 주관·운영 기관, 공식 신청 URL과 참고 URL을 포함한다.
URL은 `http` 또는 `https` 형식이 유효한 경우만 반환한다.

### 13.4 조건 판정

- 구조화 필드로 명확하게 불일치하면 목록에서 제외한다.
- 소득 등 정확하게 변환할 수 없는 조건은 제외하지 않고 `CHECK_REQUIRED`로 포함한다.
- `CHECK_REQUIRED` 정책은 `eligibilityReasons`로 사용자가 확인할 항목을 안내한다.
- 구조화된 지원금 필드가 없으므로 `supportAmount`는 `null`이다.
- 신청 기간은 안전한 종료일 파싱 전까지 `applicationPeriodText` 원문으로 반환한다.

### 13.5 실제 제공처 계약 테스트

실제 인증키가 있는 서버에서만 다음 테스트를 명시적으로 실행한다.
일반 단위 테스트나 키가 없는 개발 환경에서는 자동으로 건너뛴다.

```powershell
$env:RUN_YOUTH_POLICY_LIVE_TEST='true'
.\gradlew.bat test --tests "com.survivaldiary.domain.policy.client.YouthPolicyLiveContractTest"
Remove-Item Env:RUN_YOUTH_POLICY_LIVE_TEST
```

테스트는 인증키나 정책 원문 값을 출력하지 않는다.
최상위 JSON 타입, 최상위 필드 이름, 변환된 정책 개수와 정책 번호 존재 여부만 확인한다.

### 13.6 실제 통합 확인과 후속 사항

- [x] 서버에서 실제 성공 목록 계약 테스트 실행
- [x] 성공 응답의 최상위 JSON 타입·필드와 정책 식별자 확인
- [x] Android 앱에서 실제 목록·상세 응답 확인
- [ ] 성공 응답의 타입 래퍼 DTO와 정확한 페이지 메타데이터 확정
- [ ] 온통청년 호출 제한 확인 후 최대 3페이지 값 재검토
- [ ] 폐지·미등록 지역 코드 검증 자료를 백엔드에 둘지 결정
- [x] 앱에서 `CHECK_REQUIRED`와 `partialResult` 표시 구현
- [x] 시·도 전체 코드를 제공처에 전달하고 다른 시군구 전용 정책을 제외

## 14. 정책 MVP 마감

### MVP 완료 범위

- [x] 공식 데이터 제공처·이용 조건·인증키 관리 원칙 기록
- [x] 승인된 인증키의 실행 환경 주입과 누락·거절 진단
- [x] 나이·지역·취업·소득·분야 조건을 받는 내부 검색 API
- [x] 온통청년 실시간 목록·상세 조회와 앱 전용 DTO 변환
- [x] 구조화 근거가 부족한 조건의 `CHECK_REQUIRED` 처리
- [x] 최대 3페이지 확인과 `partialResult` 응답
- [x] 빈 결과·인증 실패·제공처 장애·잘못된 응답·정책 없음 처리
- [x] 신청 URL과 참고 URL의 역할 분리
- [x] fixture·필터 조합·오류·컨트롤러·앱 통합 흐름 검증

백엔드 이슈 #23의 완료 조건인 데이터 출처와 약관 기록, 필터·상세·빈 결과 테스트,
외부 장애 안내 정의를 충족했다. 앱에 공개하는 `PolicySummary`와 `PolicyDetail` 계약은
정책 MVP 기준으로 유지하며 외부 제공처 DTO 변경을 앱에 직접 노출하지 않는다.

### 디테일 단계로 이동한 항목

1. 제공처 성공 응답의 타입 래퍼 DTO와 정확한 전체 건수·페이지 모델
2. 호출 제한 확인 후 최대 조회 페이지와 재시도 정책 조정
3. 시도 전체·세종·폐지 지역 코드의 제공처 검증 자료
4. 신청 기간 원문의 안전한 날짜 구조화와 정렬 정확도 개선
5. 근거가 확인된 지원금 구조화
6. 캐시 또는 주기적 DB 동기화 구조
7. 정책 읽기 API의 비로그인 공개 여부

DB 구조, 갱신·삭제 정책, 캐시와 공통 API 계약을 변경하는 항목은 구현 전에 별도 선택 절차를 거친다.

## 15. 디테일 1단계 — 사용자 맞춤 조건 저장 계약

### 15.1 저장 구조

- Flyway V8에서 `user_policy_preferences`를 추가한다.
- `user_id`를 PK이자 `users.user_id` FK로 사용해 사용자당 기본 조건을 한 행만 저장한다.
- 회원 탈퇴 시 정책 기본 조건도 `ON DELETE CASCADE`로 삭제한다.
- 나이는 저장하지 않고 `users.birth_date`와 서버 현재 날짜로 계산한다.
- 시군구·소득·정책 분야는 선택 조건이므로 `null`을 허용한다.

### 15.2 내부 API

| 메서드 | 경로 | 역할 |
|---|---|---|
| `GET` | `/api/users/me/policy-preferences` | 로그인 사용자의 기본 조건 조회 |
| `PUT` | `/api/users/me/policy-preferences` | 기본 조건 전체 저장 또는 교체 |

저장된 조건이 없는 상태는 정상적인 최초 이용 상태이므로 404를 사용하지 않고 다음처럼 반환한다.

```json
{
  "success": true,
  "data": {
    "saved": false,
    "age": 26
  }
}
```

`PUT`의 선택 필드를 생략하거나 `null`로 보내면 기존 선택값을 초기화한다. 삭제 API는 이번 단계에
추가하지 않았으며, 기본 조건 전체 삭제 정책은 실제 요구가 생길 때 별도로 결정한다.

### 15.3 요청부터 응답까지

1. JWT 필터가 토큰에서 `userId`를 꺼낸다.
2. 서비스가 해당 사용자의 존재 여부를 확인한다.
3. 시도 코드와 시군구 코드의 상위 관계를 검증한다.
4. 저장 행이 없으면 생성하고, 있으면 같은 행을 전체 교체한다.
5. 생년월일에서 계산한 만 나이와 저장 조건을 공통 `ApiResponse`로 반환한다.

### 15.4 실패와 보안

- 다른 사용자 ID를 요청 본문이나 URL로 받지 않고 JWT의 사용자 ID만 사용한다.
- 존재하지 않는 토큰 사용자는 `U005`로 처리한다.
- 시도와 시군구 코드가 불일치하면 `Y004`로 저장 전에 거절한다.
- 선택 조건을 비웠을 때 빈 문자열을 저장하지 않고 `null`로 저장한다.
- 인증키·토큰·정책 원문은 기본 조건 테이블에 저장하지 않는다.

### 15.5 앱 인수인계

서버 담당자는 V8 적용 후 GET에서 `saved=false`, PUT 이후 GET에서 동일 조건이 반환되는지 확인한다.
이 계약이 배포된 다음 앱의 자동 추천 흐름을 사용할 수 있다.
