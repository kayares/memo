# Memo API

Spring Boot + JWT로 만든 메모 REST API. 백엔드 학습용 프로젝트.

## 기술 스택

- Java 21
- Spring Boot 3.5.14
- Spring Security
- Spring Data JPA
- H2 (in-memory)
- JJWT 0.12
- Gradle

## 주요 기능

- 회원가입 / 로그인 (JWT 발급)
- JWT 기반 인증 (모든 메모 API는 토큰 필요)
- 메모 CRUD
- 인가 체크 - 본인의 메모만 수정 / 삭제 가능
- 전역 예외 처리로 컨트롤러 계층 에러 응답 형식 통일

## 프로젝트 구조

```
com.kayares.memo
├── controller  HTTP 요청/응답 처리
├── service     비즈니스 로직, 트랜잭션 경계
├── repository  데이터 접근
├── domain      엔티티
├── dto         요청/응답 객체
├── exception   커스텀 예외 + 전역 핸들러
└── config      Security, JWT 설정
```

컨트롤러는 HTTP 번역만, 서비스는 규칙 판단만 담당하도록 분리했습니다.
서비스 계층은 `Authentication` 같은 웹 계층 타입에 의존하지 않고
문자열 파라미터를 받으며, DTO 변환은 컨트롤러에서 수행합니다.

## 실행 방법

**요구 사항** — JDK 21 이상

### 1. 저장소 클론

```bash
git clone https://github.com/kayares/memo.git
cd memo
```

### 2. 로컬 설정 파일 생성

`src/main/resources/application-local.properties.example`을 복사해
`application-local.properties`로 이름을 바꾸고,
`jwt.secret`을 32자 이상의 임의 문자열로 채웁니다.

### 3. 실행

```bash
./gradlew bootRun
```

기본 포트: `8080`

### 4. 주요 설정

- `spring.jpa.open-in-view=false` — 영속성 컨텍스트를 서비스 계층으로 제한
- `spring.jpa.hibernate.ddl-auto=create` — 실행 시 스키마 재생성 (학습용)
- H2 콘솔: `http://localhost:8080/h2-console`

## 아키텍처

### 요청 흐름 — 인증 성공

```mermaid
flowchart TD
    Client([클라이언트]) -->|GET /memos + JWT| Tomcat[톰캣]
    Tomcat <--> Filter[JwtAuthenticationFilter]
    Filter <-->|인증 정보 저장| Context[(SecurityContext)]

    Filter <-->|검증 성공| DS[DispatcherServlet]
    DS <-->|핸들러 조회| HM[HandlerMapping]
    DS <--> HA[HandlerAdapter]
    HA <-->|JSON 변환| Conv[HttpMessageConverter]
    HA <--> Ctrl[MemoController]
    Ctrl <--> Svc[MemoService]
    Svc <--> Repo[MemoRepository]
    Repo <--> DB[(DB)]

    HA -.->|400| DS
    Svc -.->|404| DS
    DS <-.->|예외 처리| GEH[GlobalExceptionHandler]

    style GEH fill:#e8f5e9
    style DS fill:#fff3e0
```

### 요청 흐름 — 인증 실패

```mermaid
flowchart TD
    Client([클라이언트]) -->|GET /memos<br/>토큰 없음 / 만료| Tomcat[톰캣]
    Tomcat -->|REQUEST 디스패치| Filter[Security 필터 체인]
    Filter -->|AccessDeniedException<br/>필터 내부에서 소비| Entry[Http403ForbiddenEntryPoint]
    Entry -->|sendError 403| Tomcat

    Tomcat -->|ERROR 디스패치| Filter2[Security 필터 체인<br/>재통과 · permitAll 필요]
    Filter2 --> DS[DispatcherServlet]
    DS --> BEC[BasicErrorController]
    BEC -->|Spring Boot 기본 형식| Client

    GEH[GlobalExceptionHandler<br/>호출 안 됨 · 전파된 예외 없음]:::off
    DS -.- GEH

    classDef off fill:#f5f5f5,stroke:#bbb,color:#999
```

> **실선** - 정상 요청·응답 경로 / **점선** - 예외 전파

- **400 / 404** — 예외가 `DispatcherServlet`까지 전파 → `GlobalExceptionHandler` → 프로젝트 `ErrorResponse` 형식
- **인증 실패** — 예외가 Security 필터 내부에서 소비됨 → `sendError` → `/error` ERROR 디스패치 → `BasicErrorController` 기본 형식

### ERD

```mermaid
erDiagram
    USERS ||--o{ MEMO : writes

    USERS {
        bigint id PK
        varchar_50 username UK "NOT NULL"
        varchar password "NOT NULL"
    }

    MEMO {
        bigint id PK
        varchar_100 title "NOT NULL"
        varchar_1000 content "NOT NULL"
        datetime created_at "NOT NULL"
        bigint user_id FK "NOT NULL"
    }
```

> `MemoResponse.username`은 `MEMO.user_id`로 연결된 `USERS`를 조인해 가져옵니다.

## API 명세

| Method | Endpoint      | 설명               | 인증 |
|--------|---------------|--------------------|------|
| POST   | /users/signup | 회원가입           | ❌   |
| POST   | /users/login  | 로그인 (JWT 발급)  | ❌   |
| POST   | /memos        | 메모 생성          | ✅   |
| GET    | /memos        | 메모 전체 조회     | ✅   |
| GET    | /memos/{id}   | 메모 단건 조회     | ✅   |
| PUT    | /memos/{id}   | 메모 수정 (본인만) | ✅   |
| DELETE | /memos/{id}   | 메모 삭제 (본인만) | ✅   |

인증이 필요한 요청은 헤더에 다음을 포함합니다:

```
Authorization: Bearer {your-jwt-token}
```

### 회원가입

**요청**

```http
POST /users/signup
Content-Type: application/json

{
  "username": "alice",
  "password": "password1234"
}
```

**응답** `201 Created`

```json
{
  "id": 1,
  "username": "alice"
}
```

### 로그인

**요청**

```http
POST /users/login
Content-Type: application/json

{
  "username": "alice",
  "password": "password1234"
}
```

**응답** `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

이후 요청의 `Authorization` 헤더에 이 토큰을 사용합니다.

### 메모 생성

**요청**

```http
POST /memos
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

{
  "title": "첫 메모",
  "content": "메모 내용입니다."
}
```

**응답** `201 Created`

```json
{
  "id": 1,
  "title": "첫 메모",
  "content": "메모 내용입니다.",
  "createdAt": "2026-07-27T14:58:06.674852",
  "username": "alice"
}
```

### 메모 전체 조회

**요청**

```http
GET /memos
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**응답** `200 OK`

```json
[
  {
    "id": 1,
    "title": "첫 메모",
    "content": "메모 내용입니다.",
    "createdAt": "2026-07-27T14:58:06.674852",
    "username": "alice"
  }
]
```

### 메모 단건 조회

**요청**

```http
GET /memos/1
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**응답** `200 OK`

```json
{
  "id": 1,
  "title": "첫 메모",
  "content": "메모 내용입니다.",
  "createdAt": "2026-07-27T14:58:06.674852",
  "username": "alice"
}
```

메모 수정(`PUT /memos/{id}`)의 요청·응답 형식은 생성과 동일하며,
작성자가 아닌 경우 403을 반환합니다.
삭제(`DELETE /memos/{id}`)는 성공 시 본문 없이 204를 반환합니다.

## 에러 응답

컨트롤러 계층에서 발생한 에러는 `@RestControllerAdvice`가 처리하며,
동일한 형식으로 반환됩니다:

```json
{
  "status": 404,
  "message": "메모를 찾을 수 없습니다: 999"
}
```

| 코드 | 상황                                        |
|------|---------------------------------------------|
| 400  | 요청 값 검증 실패                           |
| 401  | 로그인 실패 (아이디 없음 / 비밀번호 불일치) |
| 403  | 타인의 메모 수정·삭제 시도                  |
| 404  | 존재하지 않는 메모 또는 사용자              |
| 409  | 아이디 중복                                 |

로그인 실패 시 아이디가 없는 경우와 비밀번호가 틀린 경우에
동일한 메시지를 반환합니다.
두 경우를 구분하면 특정 아이디의 존재 여부가 노출되기 때문입니다.

### 인증 실패 응답

토큰이 없거나 유효하지 않은 요청은 Security 필터 단계에서 차단됩니다.
`ExceptionTranslationFilter`가 `AccessDeniedException`을 필터 내부에서 소비하고
`Http403ForbiddenEntryPoint`가 `sendError(403)`을 호출하므로,
`DispatcherServlet`까지 전파되는 예외가 존재하지 않습니다.
이후 톰캣이 ERROR 디스패치로 `/error`를 호출하고
`BasicErrorController`가 응답을 생성하기 때문에,
`@RestControllerAdvice`가 관여하지 못하고 Spring Boot 기본 형식이 반환됩니다.

`403 Forbidden`

```json
{
  "timestamp": "2026-07-27T06:25:08.334+00:00",
  "status": 403,
  "error": "Forbidden",
  "path": "/memos"
}
```

401이 아닌 403인 것은 `formLogin`과 `httpBasic`을 사용하지 않아
Spring Security의 기본 진입점이 `Http403ForbiddenEntryPoint`로
설정되기 때문입니다. 응답 형식과 상태 코드를 통일하려면
`AuthenticationEntryPoint`를 직접 구현해 등록해야 합니다.

## 트러블 슈팅

### PUT 응답은 정상인데 조회하면 예전 값이 반환되는 문제

**증상**
메모 수정 API 호출 시 응답에는 수정된 값이 담기지만,
이후 GET으로 조회하면 수정 전 값이 반환됐습니다.

**원인**
수정 메서드에 `@Transactional`이 없었습니다.
JPA의 더티 체킹은 영속성 컨텍스트 안에서만 동작하는데,
트랜잭션이 없으면 조회 직후 엔티티가 준영속 상태가 되어
값을 변경해도 UPDATE 쿼리가 실행되지 않습니다.
응답에 새 값이 보인 것은 자바 객체만 변경됐기 때문입니다.

**해결**
서비스 계층의 수정 메서드에 `@Transactional` 추가했습니다.

### 500 에러가 403으로 반환되어 원인 파악이 막힌 문제

**증상**
`GET /memos` 호출 시 403 Forbidden이 반환됐습니다.
JWT는 정상이었고 같은 토큰으로 다른 엔드포인트는 문제없이 동작했습니다.

**원인**
Spring Boot는 처리되지 않은 예외가 발생하면 내부적으로 `/error`로 forward 합니다.
그런데 `SecurityConfig`의 `anyRequest().authenticated()`가
이 내부 forward 요청까지 인증 대상으로 검사하면서 실제 500이 403으로 덮였습니다.
403은 증상이었고, 진짜 예외는 그 뒤에 가려져 있었습니다.

**해결**
`/error` 경로를 인증 예외 대상에 추가했습니다.

```java
.requestMatchers("/error").permitAll()
```

적용 후 실제 예외인 `LazyInitializationException`이 드러났습니다.

### 메모 목록 조회 시 LazyInitializationException이 발생하는 문제

**증상**
`spring.jpa.open-in-view=false`로 변경한 뒤 메모 목록 조회가 실패했습니다.

```
LazyInitializationException: could not initialize proxy - no Session
```

**원인**
`Memo.user`는 `fetch = LAZY`라 조회 시점에는 프록시만 채워집니다.
실제 값은 `MemoResponse` 변환 과정의 `memo.getUser().getUsername()`에서
처음 필요해지는데, 이 시점은 컨트롤러 — 즉 트랜잭션 밖입니다.
`open-in-view=false`에서는 영속성 컨텍스트가 서비스 메서드 종료 시 닫히므로
프록시가 DB에 접근할 수 없었습니다.

기본값인 `true`에서는 예외 없이 동작하지만,
메모 N개에 대해 목록 조회 1회 + 작성자 조회 N회의 쿼리가 발생합니다.
같은 원인이 설정에 따라 성능 저하 또는 예외로 다르게 나타난 것입니다.

| open-in-view  | 결과                                        |
|---------------|---------------------------------------------|
| true (기본값) | 쿼리 N+1회. 동작하지만 문제가 드러나지 않음 |
| false         | 즉시 예외 발생. 문제가 조기에 노출됨        |

**해결**
`MemoRepository`의 조회 메서드에 `@EntityGraph`를 적용했습니다.

```java
@EntityGraph(attributePaths = "user")
List<Memo> findAll();
```

LEFT JOIN으로 user를 함께 조회해 쿼리가 1회로 줄었고,
프록시가 아닌 실제 객체가 채워져 트랜잭션 밖에서도 안전해졌습니다.

`fetch = EAGER`는 전역 적용이라 user가 필요 없는 조회까지 조인이 발생해 제외했고,
JPQL fetch join은 동일한 효과지만 메서드 시그니처 변경 없이 적용 가능한
`@EntityGraph`를 선택했습니다.

## 배운 점

**계층 분리의 이유**
처음에는 컨트롤러에 조회, 권한 확인, 수정이 모두 들어있었습니다.
서비스로 추출하면서, 서비스가 HTTP를 모르는 상태로 유지되어야
다른 진입점에서도 재사용할 수 있고,
서버 없이 테스트할 수 있다는 것을 이해했습니다.

**예외 처리의 위치**
서비스는 "권한이 없다"는 사실만 던지고,
HTTP 상태 코드로의 번역은 `@RestControllerAdvice`가 담당하도록 분리했습니다.
덕분에 서비스 계층에 HTTP 관련 코드가 남지 않습니다.

**JWT의 보장 범위**
JWT의 페이로드는 Base64 인코딩일 뿐 암호화가 아니며,
서명이 보장하는 것은 기밀성이 아니라 무결성이라는 점을 확인했습니다.
민감 정보를 페이로드에 담지 않아야 하는 이유입니다.

**인증과 인가의 구분**
로그인 실패(401)와 타인의 리소스 접근(403)을
서로 다른 예외 타입으로 분리해 처리했습니다.

**에러 응답과 실제 원인의 불일치**
Security 필터는 최초 요청뿐 아니라 서버 내부 forward에도 적용됩니다.
클라이언트가 받은 상태 코드가 실제 예외와 다를 수 있다는 것을 전제하고
디버깅해야 한다는 것을 배웠습니다.

**설정이 문제를 만드는 것과 드러내는 것의 차이**
`open-in-view=false`는 문제를 만든 설정이 아니라 숨어 있던 문제를 드러낸 설정이었습니다.
DB 커넥션 점유 시간을 줄이는 이점도 있어 실무에서 권장되는 이유를 이해했습니다.

**객체 생성 시각과 영속화 시각의 구분**
처음에는 생성자에서 `createdAt`을 채웠습니다.
이 경우 기록되는 것은 자바 객체가 만들어진 시각이며,
`@PrePersist`로 옮기면 DB에 저장되는 시각이 기록됩니다.
지금은 두 시점이 사실상 같지만,
객체를 만들어두고 나중에 저장하는 경로가 생기면 달라집니다.
생성일자가 무엇의 생성인지 정해야 하는 문제였고,
영속화 시점을 기준으로 삼았습니다.

## 한계 및 미구현

학습 범위를 좁히기 위해 의도적으로 제외한 항목들입니다.

**테스트 코드 없음**
Postman 수동 검증으로 대체했습니다. 단위 테스트와 `@SpringBootTest` 기반 통합 테스트는 다음 프로젝트에서 다룰 예정입니다.

**H2 인메모리 DB**
애플리케이션 재시작 시 데이터가 초기화됩니다. 운영 환경을 가정한다면 MySQL 등으로 교체하고 마이그레이션 도구가 필요합니다.

**리프레시 토큰 미구현**
액세스 토큰 만료 시 재로그인해야 합니다. 토큰 재발급과 로그아웃 처리(블랙리스트)는 포함하지 않았습니다.

**페이지네이션·검색 없음**
전체 조회가 모든 메모를 한 번에 반환합니다. 데이터가 늘어나면 `Pageable` 적용이 필요합니다.

**예상치 못한 예외에 대한 공통 핸들러 미적용**
`@RestControllerAdvice`는 정의된 예외만 처리하며, 그 외의 예외는 형식화되지 않은 응답으로 나갑니다. 학습 단계에서는 문제가 감춰지지 않도록 의도적으로 두었습니다.

**인증 실패 응답 형식 미통일**
Security 필터 단계에서 차단된 요청은 `@RestControllerAdvice`를 거치지 않아 Spring Boot 기본 형식으로 반환됩니다. `AuthenticationEntryPoint`와 `AccessDeniedHandler` 구현으로 통일할 수 있습니다.