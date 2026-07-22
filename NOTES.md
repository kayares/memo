# 개발 메모

## IntelliJ 단축키
- 터미널: Alt + F12
- 프로젝트 창: Alt + 1
- 검색(전체): Shift 두 번
- 파일 내 검색: Ctrl + F
- 클래스로 이동: Ctrl + N
- 자동 import: Alt + Enter
- 코드 포맷: Ctrl + Alt + L
- 이름 변경(리팩터링): Shift + F6
- 실행: Shift + F10

## 트러블슈팅
### 심볼을 해결할 수 없습니다 (import 전부 빨간줄)
원인: Gradle 의존성 인덱싱 실패. 코드 문제 아님.
1. Gradle 탭 → 새로고침(↻)
2. ./gradlew build --refresh-dependencies
3. File → Invalidate Caches → Invalidate and Restart
4. Project Structure에서 SDK 21 확인

## Gradle 명령어
- ./gradlew build       빌드
- ./gradlew bootRun     실행
- ./gradlew clean       빌드 산출물 삭제