# 개발 메모

## IntelliJ 단축키
- 터미널: Alt + F12
- 프로젝트 창: Alt + 1
- 검색(전체): Shift 두 번
- 파일 내 검색: Ctrl + F
- 클래스로 이동: Ctrl + N
- 선언으로 이동: Ctrl + B
- 자동 import: Alt + Enter
- 코드 포맷: Ctrl + Alt + L
- 이름 변경(리팩터링): Shift + F6
- 실행: Shift + F10

## Gradle 명령어
- ./gradlew build       빌드
- ./gradlew bootRun     실행
- ./gradlew clean       빌드 산출물 삭제
(PowerShell이면 .\gradlew)

## 커밋 컨벤션 (Conventional Commits)
| 접두어 | 의미 |
|---|---|
| feat | 새 기능 |
| fix | 버그 수정 |
| docs | 문서만 변경 |
| refactor | 기능 변화 없는 구조 개선 |
| test | 테스트 코드 |
| chore | 빌드 설정, 의존성 등 |

## 트러블슈팅
### 심볼을 해결할 수 없습니다 (import 전부 빨간줄)
원인: Gradle 의존성 인덱싱 실패. 코드 문제 아님.
1. Gradle 탭 → 새로고침(↻)
2. ./gradlew build --refresh-dependencies
3. File → Invalidate Caches → Invalidate and Restart
4. Project Structure에서 SDK 21 확인

### PUT 응답엔 새 값, GET하면 옛날 값
원인: @Transactional 누락 → 영속성 컨텍스트가 없어 dirty checking 미작동.
자바 객체만 바뀌고 UPDATE 쿼리가 안 나감.

---

# 개념 정리

## 1. HTTP
- 요청은 텍스트 한 덩어리. 시작 줄(메서드+경로) → 헤더 → 빈 줄 → 바디
- 데이터 위치: 경로(@PathVariable) 또는 바디(@RequestBody)
- 바디 형식이 JSON. 위치와 형식은 별개 개념
- GET은 보통 바디 없음
- 상태 코드는 서버(=내 코드)가 정함. 200 정상, 400 잘못된 요청, 403 권한 없음, 404 자원 없음
- REST: 주소는 "무엇", 메서드는 "어떻게". /getMemo?id=5 (X) → GET /memos/5 (O)

## 2. Spring MVC
요청 흐름:
```
클라이언트 → HTTP 텍스트 → 톰캣 → DispatcherServlet
→ 핸들러 매핑으로 컨트롤러 찾기 → 메서드 실행 → 자바 객체 리턴
→ MessageConverter(Jackson)가 JSON 변환 → 응답 바디
→ 톰캣 → 클라이언트
```
- DispatcherServlet = 모든 요청을 받는 단 하나의 서블릿, 컨트롤러로 배차
- 핸들러 매핑: 앱 뜰 때 애노테이션 스캔해 "GET /memos → getMemos()" 표를 미리 생성
- main은 리턴되지만 톰캣이 별도 스레드로 대기 → 서버가 계속 떠 있음
- @RestController = @Controller + @ResponseBody (빈 등록 + JSON 변환)
- ResponseEntity = 상태 코드 + 헤더 + 바디를 담는 객체 (클래스)
- @ResponseBody = "리턴값을 뷰 이름이 아니라 바디에 직접 써라" (애노테이션)
- 클래스 @RequestMapping("/memos") + 메서드 @GetMapping("/{id}") → /memos/{id}
- {id}는 자리표시자. 패턴과 실제 주소 대조해서 값 추출

## 3. 계층 구조
```
[Controller]  HTTP 통역 — JSON↔객체, 상태 코드
↓
[Service]     판단 — 존재 확인, 권한 확인, @Transactional
↓
[Repository]  DB 접근 — 쿼리
↓
[DB]
```
나누는 이유 (관례 아님):
- 재사용: 서비스는 HTTP를 모르니 디스코드 봇 등에서도 호출 가능
- 테스트: 서버·토큰·HTTP 없이 메서드 한 줄로 규칙 검증 가능

## 4. DI와 빈
- 빈 = Spring이 만들어 관리하는 객체. ApplicationContext(컨테이너)에 보관
- 컨테이너는 보관만. 실행은 그 객체 위에서
- 빈은 앱 시작 시 생성, 싱글톤. 요청마다 만들어지지 않음
- **빈은 하나, 스레드는 요청마다** → 요청별 값은 파라미터로, 필드에 두면 안 됨
- 주입 순서: @Service 등으로 빈 등록 → 쓰는 쪽에서 final 필드 선언 → Spring이 생성자로 주입
- Lombok은 컴파일 때 생성자 작성, Spring은 실행 때 그 생성자로 주입. **서로 무관**
- @RequiredArgsConstructor는 final 필드만 골라 생성자 생성

| 애노테이션 | 계층 | 추가 기능 |
|---|---|---|
| @RestController | 컨트롤러 | JSON 변환 |
| @Service | 서비스 | 없음 |
| @Repository | 리포지토리 | DB 예외 변환 |
| @Component | 아무거나 | 없음 |

- Spring Data JPA: JpaRepository 상속 인터페이스를 보고 프록시 구현체를 자동 생성·등록

## 5. JPA
- JPA는 규격(인터페이스), Hibernate는 구현체. 실제로 SQL 만드는 건 Hibernate
- @Entity = 클래스↔테이블, 필드↔컬럼, 객체 하나↔행 하나
- @Id @GeneratedValue = PK, 자동 생성

### 영속성 컨텍스트
- 트랜잭션 동안 엔티티를 보관하는 1차 캐시. 트랜잭션과 생성·소멸 동시
- 같은 id 재조회 → 쿼리 안 나감, 같은 객체 반환 (a == b)

### dirty checking
- 조회 시점 값을 스냅샷으로 복사 → 트랜잭션 끝날 때 현재 값과 비교 → 다르면 UPDATE
- setTitle()은 그냥 자바 setter. DB와 무관. Hibernate가 옆에서 감지하는 것
- save() 호출 불필요
- @Transactional 없으면 컨텍스트가 없어 전부 무효

| 작업 | 쿼리 시점 |
|---|---|
| 조회 | 즉시 (캐시에 없을 때만) |
| 수정 | 트랜잭션 종료 시 (쓰기 지연) |
| 저장 | 보통 종료 시 |

### 트랜잭션
- 여러 DB 작업을 하나의 단위로 묶음. 전부 성공 또는 전부 실패
- 예: 계좌 이체 — 출금과 입금이 함께 되거나 함께 취소

### N+1
- @ManyToOne 걸린 메모 100개 순회 → 1 + 100 = 101번 쿼리
- 해결: fetch join, @EntityGraph

## 6. Security와 JWT
```
로그인: ID/PW → 해시 비교 → 토큰 발급(서명 포함)
이후 요청: Authorization 헤더에 토큰
→ JwtAuthenticationFilter (서명 검증 → 만료 확인 → SecurityContextHolder에 저장)
→ 컨트롤러 (Authentication 파라미터로 주입)
```
- 필터에서 인증하는 이유: 모든 요청이 지나는 단일 지점. 컨트롤러마다 검사하면 누락 위험
- SecurityContextHolder는 ThreadLocal 사용 → 요청 스레드마다 격리
- 저장 안 하면 컨트롤러 Authentication이 null → NPE
- SecurityConfig에서 로그인·회원가입 permitAll 필수. 안 하면 토큰 받을 방법이 없음

### JWT 구조
`헤더.내용.서명` — 각각 Base64
- Base64는 암호화 아님. 전송 중 깨지지 않게 하는 포장. 누구나 디코딩 가능
- **비밀번호 등 민감 정보 넣지 말 것**
- 서명 = HMAC-SHA256(헤더+내용, 비밀키). 해시라 역변환 불가
- 내용을 바꾸면 서명이 안 맞음. 올바른 서명은 비밀키 없이 못 만듦
- **기밀성이 아니라 무결성 보장** — 읽기 가능, 위조 불가
- 비밀키 유출 = 전부 뚫림 → application-local.properties, 커밋 금지

### 인증 vs 인가
- 인증(Authentication) = 누구냐. 토큰 검증. 실패 시 401
- 인가(Authorization) = 뭘 할 수 있냐. 소유자 확인. 실패 시 403

### 세션 vs JWT
| | 세션 | JWT |
|---|---|---|
| 저장 위치 | 서버 | 클라이언트 |
| 서버 확장 | 세션 공유 필요 | 그냥 늘리면 됨 |
| 강제 로그아웃 | 쉬움 | 어려움 (만료까지 유효) |

JWT 단점: 무효화가 어려움 → 만료 짧게 + 리프레시 토큰, 또는 Redis 블랙리스트

### 비밀번호
- 해시로 저장. 되돌리는 게 아니라 입력값을 다시 해시해서 비교
- BCrypt는 솔트를 섞어 같은 비밀번호도 사용자마다 다른 해시
- passwordEncoder.matches(입력, 저장된해시)