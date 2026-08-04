# 웹 SNS 로그인 설정

웹은 OAuth Authorization Code를 받은 뒤 백엔드가 공급자 토큰으로 교환한다. 카카오 Client Secret과 네이버 Client Secret은 웹 브라우저에 전달하지 않는다.

백엔드 환경 변수 또는 `application-secret.yml`:

```yaml
oauth:
  kakao:
    rest-api-key: ${KAKAO_REST_API_KEY}
    client-secret: ${KAKAO_CLIENT_SECRET:}
  naver:
    client-id: ${NAVER_LOGIN_CLIENT_ID}
    client-secret: ${NAVER_LOGIN_CLIENT_SECRET}
```

```text
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

공급자 콘솔 콜백 주소:

- 카카오: `http://localhost:5173/auth/callback/kakao`
- 네이버: `http://localhost:5173/auth/callback/naver`

운영 배포 시 위 주소를 실제 HTTPS 웹 도메인으로 추가하고 CORS 허용 주소도 같은 도메인으로 변경한다.
