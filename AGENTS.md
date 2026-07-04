# AGENTS.md

## Repo 구조

- Spring Boot API: `src/main/java/com/koreaconcrete/civilshop`
- Flyway migration: `src/main/resources/db/migration`
- 정적 프론트엔드: `frontend`
- 설계 문서: `docs`

## Backend 실행

```bash
export SPRING_PROFILES_ACTIVE=dev
export JWT_SECRET=replace-with-at-least-32-random-characters
./gradlew bootRun
```

## Frontend 실행

별도 프론트 서버를 띄우지 않는다. `frontend/` 정적 파일은 Gradle `processResources`에서 Spring Boot `static/` 리소스로 복사되고, 백엔드 실행 후 `http://localhost:8080/index.html`로 확인한다.

## Test/Build

```bash
./gradlew test
./gradlew build
```

## 코드 스타일

- Java 21, Spring Boot 4.1 스타일을 따른다.
- 도메인별 패키지(`auth`, `user`, `category`, `product`, `pricing`, `freight`, `cart`, `quote`, `consultation`, `search`, `admin`)를 유지한다.
- DTO는 API 경계를 명확히 하기 위해 entity를 직접 노출하지 않는다.
- 프론트엔드는 React/Vue/Angular/Next.js 없이 순수 HTML/CSS/JS만 사용한다.

## API 응답 규칙

- Base URL: `/api/v1`
- 목록 응답은 `items`, `page`, `size`, `total`, `hasNext` 구조를 사용한다.
- 에러 응답은 `code`, `message`, `details` 구조를 사용한다.
- 금액은 KRW 정수, 치수/중량/수량은 `BigDecimal` 기준이다.

## DB migration 규칙

- schema 변경은 Flyway migration으로 추가한다.
- 운영 데이터 seed는 migration에 넣지 않는다.
- 개발 seed는 `DataInitializer`에서 `dev` 프로필로만 실행한다.

## 보안상 금지사항

- 비밀번호 평문 저장 금지.
- JWT secret, DB password 등 민감정보 하드코딩 금지.
- 관리자 API public 노출 금지.
- 개인정보가 포함된 견적/상담/회원 정보 접근 시 감사 로그를 고려한다.

## MVP 제외 규칙

자료실/게시판 기능은 MVP에서 제외한다. `Board`, `Post`, `FileAsset`, `BoardKey`, `FileOwnerType` 엔티티, API, 페이지, 메뉴를 만들지 않는다. `ProductMedia`는 상품 이미지/상세 이미지/외부 미디어 메타데이터 용도로 유지한다.

## 완료 조건

- Backend app 실행 가능
- Frontend 정적 페이지 실행 가능
- Flyway migration 가능
- dev profile seed 생성
- Swagger UI 접근 가능
- JWT 로그인/인증 동작
- 주요 Public/Admin API 동작 및 관리자 권한 체크
- 게시판/자료실 계열 기능 미포함
