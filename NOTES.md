# 개발 메모

## IntelliJ 단축키
- 터미널: Alt + F12
- 프로젝트 창: Alt + 1
- 검색(전체): Shift 두 번
- 파일 내 검색: Ctrl + F
- 클래스로 이동: Ctrl + N
- 선언으로 이동: Ctrl + B
- 자동 import: Alt + Enter
- import 정리: Ctrl + Alt + O
- 코드 포맷: Ctrl + Alt + L
- 이름 변경(리팩터링): Shift + F6
- 실행: Shift + F10

## Gradle 명령어
- ./gradlew build       빌드
- ./gradlew bootRun     실행 (종료: Ctrl + C)
- ./gradlew test        테스트
- ./gradlew clean       빌드 산출물 삭제
(PowerShell이면 .\gradlew)

## 커밋 컨벤션 (Conventional Commits)
| 접두어   | 의미                     |
|----------|--------------------------|
| feat     | 새 기능                  |
| fix      | 버그 수정                |
| docs     | 문서만 변경              |
| refactor | 기능 변화 없는 구조 개선 |
| test     | 테스트 코드              |
| chore    | 빌드 설정, 의존성 등     |

커밋 단위 기준: 되돌려도 앱이 정상 동작하는 상태여야 함.

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

### "메서드가 한 번도 사용되지 않습니다" 경고
오탐. @ExceptionHandler, @GetMapping, @Bean 등은 Spring이 리플렉션으로 호출.
정적 분석으로는 호출부를 찾을 수 없음.

---

# 개념 정리

## 전체 요청 경로
```
[클라이언트]
↓ 요청
[리버스 프록시 (Nginx 등, 선택)]
↓
[WAS (톰캣) — 서블릿 컨테이너]
↓
[필터 체인]  ← JwtAuthenticationFilter
↓
[DispatcherServlet]
↓ 핸들러 매핑으로 컨트롤러 찾기
[Controller]
↓
[Service]  ← @Transactional 시작 - 영속성 컨텍스트 생성
↓
[Repository]
↓
[DB]
```

응답은 같은 경로를 역순으로 거슬러 올라감.
DispatcherServlet에서 MessageConverter(Jackson)가 JSON 변환 후
톰캣이 클라이언트로 전송.

- 스프링 컨테이너(ApplicationContext)는 경로상의 단계가 아님. 빈 보관 창고. WAS 안에 존재.
- 컨테이너 두 종류: 서블릿 컨테이너(톰캣) / 스프링 컨테이너(ApplicationContext)
- 핸들러 = 컨트롤러 메서드. 핸들러 매핑은 목적지를 찾는 과정
- Spring MVC의 서블릿은 DispatcherServlet 하나 (프론트 컨트롤러 패턴)
- 필터는 DispatcherServlet 바깥 → 필터 예외는 @RestControllerAdvice가 못 잡음

## 1. HTTP
- 요청은 텍스트 한 덩어리. 시작 줄(메서드+경로) → 헤더 → 빈 줄 → 바디
- 데이터 위치: 경로(@PathVariable) 또는 바디(@RequestBody)
- 바디 형식이 JSON. 위치와 형식은 별개
- GET은 보통 바디 없음
- 상태 코드는 서버(=내 코드)가 정함
- REST: 주소는 "무엇", 메서드는 "어떻게". /getMemo?id=1 (X) → GET /memos/1 (O)

### 상태 코드
| 코드 | 의미                                   |
|------|----------------------------------------|
| 200  | 성공                                   |
| 204  | 성공, 응답 바디 없음 (삭제)            |
| 400  | 요청 형식 오류 (@Valid 실패)           |
| 401  | 인증 안 됨 — "너 누군지 모르겠다"      |
| 403  | 인가 실패 — "너인 건 알겠는데 안 된다" |
| 404  | 자원 없음                              |
| 409  | 현재 상태와 충돌 (아이디 중복)         |
| 500  | 서버 오류                              |

2xx 성공 / 4xx 클라이언트 잘못 / 5xx 서버 잘못

## 2. Spring MVC
- @RestController = @Controller + @ResponseBody (빈 등록 + JSON 변환)
- @ResponseBody = "리턴값을 뷰 이름이 아니라 바디에 직접 써라" (애노테이션)
- ResponseEntity = 상태 코드 + 헤더 + 바디를 담는 객체 (클래스)
- 클래스 @RequestMapping("/memos") + 메서드 @GetMapping("/{id}") → /memos/{id}
- {id}는 자리표시자. 패턴과 실제 주소 대조해서 값 추출
- main은 리턴되지만 톰캣이 별도 스레드로 대기 → 서버가 계속 떠 있음
- 뷰 방식(@Controller, HTML 리턴)과 REST API 방식(@RestController, JSON)의 차이

## 3. 계층 구조
```
[Controller]    HTTP 통역 — JSON ↔ 객체, 상태 코드
↓
[Service]       판단 — 존재 확인, 권한 확인, @Transactional
↓
[Repository]    DB 접근 — 쿼리
↓
[DB]
```
나누는 이유 (관례 아님):
- 재사용: 서비스는 HTTP를 모르니 다른 진입점(봇 등)에서도 호출 가능
- 테스트: 서버·토큰·HTTP 없이 메서드 한 줄로 규칙 검증 가능

원칙:
- 서비스 파라미터는 Authentication이 아니라 username 문자열
- 서비스 리턴은 엔티티(Memo). DTO 변환은 컨트롤러가
- @Transactional은 서비스에. 컨트롤러에 두면 서비스를 직접 부르는 쪽엔 트랜잭션이 없음

## 4. DI와 빈
- 빈 = Spring이 만들어 관리하는 객체. ApplicationContext에 보관
- 컨테이너는 보관만. 실행은 그 객체 위에서
- 빈은 앱 시작 시 생성, 싱글톤. 요청마다 만들어지지 않음
- **빈은 하나, 스레드는 요청마다** → 요청별 값은 파라미터로, 필드에 두면 안 됨
- 주입 순서: @Service 등으로 빈 등록 → 쓰는 쪽에서 final 필드 선언 → 생성자로 주입
- Lombok은 컴파일 때 생성자 작성, Spring은 실행 때 그 생성자로 주입. **서로 무관**
- @RequiredArgsConstructor는 final 필드만 골라 생성자 생성
- static 필드는 주입 안 됨 (객체보다 먼저 존재)

| 애노테이션      | 계층       | 추가 기능    |
|-----------------|------------|--------------|
| @RestController | 컨트롤러   | JSON 변환    |
| @Service        | 서비스     | 없음         |
| @Repository     | 리포지토리 | DB 예외 변환 |
| @Component      | 아무거나   | 없음         |

- Spring Data JPA: JpaRepository 상속 인터페이스를 보고 프록시 구현체를 자동 생성·등록
- 서비스 클래스는 자동 생성 없음. 직접 작성.

## 5. JPA
- JPA는 규격(인터페이스), Hibernate는 구현체. 실제로 SQL 만드는 건 Hibernate
- @Entity = 클래스 ↔ 테이블, 필드 ↔ 컬럼, 객체 하나 ↔ 행 하나
- @Id @GeneratedValue = PK, 자동 생성

### 영속성 컨텍스트
- 트랜잭션 동안 엔티티를 보관하는 1차 캐시. 트랜잭션과 생성·소멸 동시
- 같은 id 재조회 → 쿼리 안 나감, 같은 객체 반환 (a == b)

### dirty checking
- 조회 시점 값을 스냅샷으로 복사 → 트랜잭션 끝날 때 현재 값과 비교 → 다르면 UPDATE
- setTitle()은 그냥 자바 setter. DB와 무관. Hibernate가 옆에서 감지하는 것
- save() 호출 불필요
- @Transactional 없으면 컨텍스트가 없어 전부 무효

| 작업 | 쿼리 시점                    |
|------|------------------------------|
| 조회 | 즉시 (캐시에 없을 때만)      |
| 수정 | 트랜잭션 종료 시 (쓰기 지연) |
| 저장 | 보통 종료 시                 |

### 트랜잭션
- 여러 DB 작업을 하나의 단위로 묶음. 전부 성공 또는 전부 실패
- 예: 계좌 이체 — 출금과 입금이 함께 되거나 함께 취소

### N+1
- @ManyToOne 걸린 메모 100개 순회 → 1 + 100 = 101번 쿼리
- 해결: fetch join, @EntityGraph

### 기타
- spring.jpa.open-in-view 기본값 true → 영속성 컨텍스트가 응답까지 열려 있음.
  실무에선 보통 false로 끔

## 6. Security와 JWT
```
로그인: ID/PW → 해시 비교 → 토큰 발급(서명 포함)
이후 요청: Authorization 헤더에 "Bearer {토큰}"
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
- HTTP 401의 공식 명칭이 "Unauthorized"지만 실제 의미는 "인증 안 됨".
  명세 자체의 네이밍 오류. 클래스 이름 지을 때 주의.

### 세션 vs JWT
|               | 세션           | JWT                    |
|---------------|----------------|------------------------|
| 저장 위치     | 서버           | 클라이언트             |
| 서버 확장     | 세션 공유 필요 | 그냥 늘리면 됨         |
| 강제 로그아웃 | 쉬움           | 어려움 (만료까지 유효) |

JWT 단점: 무효화가 어려움 → 만료 짧게 + 리프레시 토큰, 또는 Redis 블랙리스트

### 비밀번호
- 해시로 저장. 되돌리는 게 아니라 입력값을 다시 해시해서 비교
- BCrypt는 솔트를 섞어 같은 비밀번호도 사용자마다 다른 해시
- passwordEncoder.matches(입력, 저장된해시)
- 로그인 실패 시 "아이디 없음"과 "비밀번호 틀림"을 구분하지 말 것
  → 계정 열거(account enumeration) 공격에 악용됨

## 7. 예외 처리
- @RestControllerAdvice = 클래스 단위. 빈 등록 + 전역 예외 담당 선언
- @ExceptionHandler = 메서드 단위. 담당할 예외 타입 지정
  (@RestController + @GetMapping 관계와 동일)
- 예외는 컨트롤러 뚫고 DispatcherServlet까지 → 타입으로 핸들러 조회 → 호출
- 안 잡으면 500 + 스택 트레이스 노출 위험
- 서비스는 예외만 던짐, 상태 코드는 핸들러가 결정 (서비스는 HTTP 모름)
- @ExceptionHandler는 타입만 보고 매칭. 남이 만든 예외도 잡힘
  (MethodArgumentNotValidException 등)

### 예외 클래스 작성
- 예외는 빈이 아님. 그냥 자바 객체.
- @RequiredArgsConstructor 쓰면 super()가 안 불려서 메시지가 null → 생성자 직접 작성
- RuntimeException 자체는 메시지 문자열 보관이 전부.
  실제 동작은 throw 키워드에 있음 (즉시 중단 + 호출자로 거슬러 올라감)
- 새로 만드는 건 사실상 "이름 하나"

### RuntimeException을 상속하는 이유
1. throws 선언 불필요 (체크 예외는 컴파일러가 강제)
2. **Spring 기본 롤백 대상.** 체크 예외는 롤백 안 되고 커밋됨
   → @Transactional(rollbackFor = Exception.class)로 변경 가능

### 자바 표준 예외를 핸들러에 등록하지 말 것
IllegalArgumentException 등은 라이브러리 내부에서도 터짐.
전부 같은 상태 코드로 뭉뚱그려짐 → 상황별 커스텀 예외를 만들 것

## 기타
- 생성자를 하나도 안 쓰면 자바가 기본 생성자를 자동 생성
  → @NoArgsConstructor는 없어도 동작. 다른 생성자를 추가할 때를 대비한 보험
- Jackson은 기본 생성자로 빈 객체를 만든 뒤 리플렉션으로 필드를 채움
- 엔티티를 그대로 응답하면 비밀번호 해시 등이 노출 → DTO로 변환
- .stream().map(X::new).toList() = for문으로 하나씩 변환해 새 리스트 만드는 것
- 선언 타입은 List, 구현은 ArrayList가 관례. .toList()는 불변 리스트