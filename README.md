# Memo API

Spring Boot + JWT로 만든 간단한 메모 REST API. 백엔드 학습용 프로젝트.

## 기술 스택

- Java 21
- Spring Boot 3.5
- Spring Security
- Spring Data JPA
- H2 (in-memory)
- JJWT 0.12
- Gradle

## 주요 기능

- 회원가입 / 로그인 (JWT 발급)
- JWT 기반 인증 (모든 메모 API는 토큰 필요)
- 메모 CRUD (생성, 조회, 수정, 삭제)
- 인가 체크 — 본인의 메모만 수정/삭제 가능

## 실행 방법

### 1. 저장소 클론

```bash
git clone https://github.com/kayares/memo.git
cd memo
```

### 2. 로컬 설정 파일 생성

`src/main/resources/application-local.properties.example`을 복사해
`src/main/resources/application-local.properties`로 이름을 바꾸세요.

`jwt.secret` 값을 32자 이상의 임의 문자열로 채워주세요.

### 3. 실행

```bash
./gradlew bootRun
```

기본 포트: `8080`

## API 명세

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST   | /users/signup    | 회원가입 | ❌ |
| POST   | /users/login     | 로그인 (JWT 발급) | ❌ |
| POST   | /memos           | 메모 생성 | ✅ |
| GET    | /memos           | 메모 전체 조회 | ✅ |
| GET    | /memos/{id}      | 메모 단건 조회 | ✅ |
| PUT    | /memos/{id}      | 메모 수정 (본인만) | ✅ |
| DELETE | /memos/{id}      | 메모 삭제 (본인만) | ✅ |

인증이 필요한 요청은 헤더에 다음을 포함해야 합니다:

```
Authorization: Bearer {your-jwt-token}
```

## 배운 점

- Spring Security 필터 체인과 커스텀 필터 등록
- JWT의 구조와 서명 검증 원리
- JPA 연관관계 매핑 (`@ManyToOne`)과 지연 로딩
- `@Transactional`과 더티 체킹의 동작 방식
- 인증(Authentication)과 인가(Authorization)의 분리