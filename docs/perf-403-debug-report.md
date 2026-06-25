# perf-client 403 디버깅 보고서

작성일: 2026-06-25

---

## 증상

`make perf-client` 실행 시 k6 setup 단계에서 즉시 실패.

```
ERRO[0001] Error: [setup] 로그인 실패 (user1@test.com) status=403
    at login (file:///scripts/last-watched-baseline.js)
    hint="script exception"
```

---

## 원인 분석

### 직접 원인: Spring Security CSRF 필터

`SecurityConfig.java`에서 `CookieCsrfTokenRepository` + `SpaCsrfTokenRequestHandler` 조합으로 CSRF 보호가 활성화되어 있다.

`/api/auth/login`은 `permitAll()` 처리되어 있어 인증 없이 접근 가능하지만, **CSRF 필터는 인증 여부와 무관하게 모든 POST 요청에 적용된다.** k6가 CSRF 토큰 없이 POST를 날리므로 403이 반환된다.

```java
// SecurityConfig.java
.csrf(csrf -> csrf
    .csrfTokenRepository(csrfTokenRepository)
    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
)
```

### DB/Spring 문제가 아닌 이유

- DB에 `user1@test.com` ~ `user1000@test.com` 데이터 존재 확인
- Spring Boot 정상 기동 확인
- 에러 코드가 401(인증 실패)이 아닌 **403(접근 거부)** → CSRF 필터에서 차단됨

---

## 시도한 해결책

### 시도 1: `/api/auth/csrf` 에서 쿠키 읽기

`csrfRes.cookies['XSRF-TOKEN']`으로 쿠키를 읽어 헤더에 첨부.

```js
const csrfToken = csrfRes.cookies['XSRF-TOKEN']
    ? csrfRes.cookies['XSRF-TOKEN'][0].value
    : null;
```

**결과: 실패** — `/api/auth/csrf`는 쿠키를 `Set-Cookie` 응답 헤더로 전달하므로 k6의 `.cookies` 프로퍼티에서 잡히지 않았음.

---

### 시도 2: `Set-Cookie` 헤더 직접 파싱

응답 헤더에서 정규식으로 직접 토큰 값을 추출.

```js
const setCookie = csrfRes.headers['Set-Cookie'] || '';
const match = setCookie.match(/XSRF-TOKEN=([^;]+)/);
const csrfToken = match ? match[1] : null;
```

**결과: 실패 (현재 상태)** — 여전히 403 반환. 토큰 파싱은 되는 것으로 보이나 서버에서 유효하지 않은 것으로 판단하는 중.

---

## 미해결 가설

현재 `SpaCsrfTokenRequestHandler`의 동작 방식 재검토가 필요하다.

```java
// 헤더로 토큰이 오면 plain 핸들러 사용
public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
    if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
        return plain.resolveCsrfTokenValue(request, csrfToken);  // raw 값 기대
    }
    return xor.resolveCsrfTokenValue(request, csrfToken);        // XOR 인코딩 값 기대
}
```

`plain` 핸들러(`CsrfTokenRequestAttributeHandler`)는 요청 헤더의 토큰을 **세션/쿠키에 저장된 raw 토큰과 직접 비교**한다.  
그런데 `/api/auth/csrf` 엔드포인트에서 반환하는 `csrfToken.getToken()`이 XOR 인코딩된 값일 경우, 헤더로 보내도 raw 값과 불일치하여 403이 발생할 수 있다.

### 확인이 필요한 것

1. `/api/auth/csrf` 응답 쿠키 값이 raw 토큰인지 XOR 인코딩된 토큰인지
2. k6에서 실제로 `X-XSRF-TOKEN` 헤더가 요청에 포함되는지 (`csrfToken`이 null인 경우 헤더 미포함)
3. `CookieCsrfTokenRepository`가 내부적으로 저장하는 값과 `/api/auth/csrf`가 반환하는 값이 일치하는지

---

## 다음 시도 방향

**방향 A: 서버에서 확인**  
실제 브라우저로 `/api/auth/csrf` 호출 후 반환되는 쿠키 값과, 이후 로그인 요청 시 전송되는 `X-XSRF-TOKEN` 헤더 값을 비교하여 raw/XOR 여부 확인.

**방향 B: k6에서 디버깅 로그 추가**  
`csrfToken` 값이 실제로 파싱되는지 `console.log`로 확인.

```js
console.log(`[debug] csrfToken: ${csrfToken}`);
console.log(`[debug] Set-Cookie: ${setCookie}`);
```

**방향 C: 부하 테스트 전용 CSRF 예외 처리**  
`application-perf.yaml` 프로파일을 만들어 부하 테스트 환경에서만 CSRF를 비활성화.

```yaml
# application-perf.yaml
spring:
  security:
    csrf:
      enabled: false  # 부하 테스트 전용
```

> 단, 이 방법은 보안 설정 변경이므로 perf 프로파일이 운영에 절대 사용되지 않도록 격리 필요.

---

## 해결 (2026-06-25 갱신)

"미해결 가설(토큰 raw/XOR)"은 헛다리였다. **CSRF 토큰을 맞출 필요 자체가 없었다.** 채택한 방향은 보고서의 **방향 C**다.

### 1. 서버: perf 프로파일에서 CSRF 비활성화 (근본 해결)

CSRF 비활성화 코드(`SecurityConfig`의 `@Profile("perf")` 필터체인)와 `application-perf.yaml`, `.env.perf`의 `SPRING_PROFILES_ACTIVE=dev,perf` 는 이미 작성돼 있었으나, **실측 대상 백엔드가 `dev` 프로파일만으로 떠 있어서** perf 체인이 활성화되지 않았던 것이 직접 원인이었다(403 지속).

→ 백엔드를 `SPRING_PROFILES_ACTIVE=dev,perf` 로 재기동하면 CSRF 필터가 통째로 빠진다.
(로컬 IntelliJ 실행 시 Run Config의 Active profiles 를 `dev,perf` 로, 또는 `make perf-server`(docker, `.env.perf`) 사용.)

### 2. k6 스크립트: 로그인 응답 규약 갱신

로그인 응답은 `200 + body.accessToken` 이 아니라 **`204 No Content` + HttpOnly 쿠키(`accessToken`)** 다.
세 스크립트(`last-watched-baseline.js`, `learning-event-perf.js`, `coupon-fcfs-perf.js`)의 `login()` 을:

- 상태코드 `200` → `200/204` 허용
- 토큰 추출 `JSON.parse(res.body).accessToken` → `res.cookies['accessToken'][0].value`

로 수정했다. (다중 `Set-Cookie` 는 `res.headers` 로는 하나만 잡히므로 `res.cookies` 로 읽는 것이 정석 — 시도 2가 실패했던 이유.)
JWT 필터는 `Authorization: Bearer` → 없으면 `accessToken` 쿠키 순으로 읽으므로, 추출한 토큰을 그대로 Bearer 로 보내는 기존 구조는 유지된다.

### 남은 이슈(별개): 시드 데이터 미스매치

`make perf-client` setup(로그인)은 통과하나, 본 시나리오의 `status=200` 체크가 MISS 단계에서 대량 실패한다.
→ 스크립트는 `TOTAL_COURSES = 100`(bulk 시드)를 가정하는데 현재 DB는 `simple` 시드(강좌 18·유저 87·진행이력 243)라 존재하지 않는 강좌 요청이 404가 된다.
→ 깔끔한 측정을 위해서는 `APP_DATA_INIT_MODE=bulk` 로 재시드하거나, 스크립트의 `TOTAL_COURSES`/`USER_POOL` 을 실제 시드 규모에 맞춘다.

## 현재 상태 요약

| 항목 | 상태 |
|---|---|
| Spring Boot 기동 | ✅ 정상 |
| DB 테스트 유저 | ✅ 존재 확인 |
| CSRF 원인 파악 | ✅ perf 프로파일 미활성 |
| CSRF 해결 | ✅ `dev,perf` 재기동 + 스크립트 쿠키 토큰화 |
| 로그인 성공(setup) | ✅ 해결 (403 → 204) |
| 본 시나리오 클린 측정 | ⚠️ 시드 데이터 bulk 재시드 필요(별개 이슈) |
