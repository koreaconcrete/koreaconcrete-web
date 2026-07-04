# Korea Concrete Web

토목자재 B2B 견적형 커머스 MVP입니다. 상품 카탈로그, 규격 variant, 가격표, 상차/운반비 룰, 견적요청, 상담요청, 관리자 기능을 구현합니다.

## 기술 스택

- Backend: Java 21, Spring Boot 4.1, Spring WebMVC, Spring Security, Spring Data JPA/Hibernate
- DB: PostgreSQL 기준, dev/test는 H2 지원
- Migration: Flyway
- Auth: JWT access token, BCrypt password hash
- API 문서: springdoc-openapi Swagger UI
- Frontend: 순수 HTML/CSS/JavaScript

## 폴더 구조

- `src/main/java/com/koreaconcrete/civilshop`: Spring Boot API 서버
- `src/main/resources/db/migration`: Flyway migration
- `frontend`: 빌드 없는 정적 프론트엔드
- `docs`: 설계 메모
- `README.md`, `AGENTS.md`, `.env.example`: 실행/협업 문서

## 로컬 실행

개발용 H2로 바로 실행:

```bash
export SPRING_PROFILES_ACTIVE=dev
export JWT_SECRET=replace-with-at-least-32-random-characters
./gradlew bootRun
```

PostgreSQL 사용:

```bash
docker run --name koreaconcrete-postgres \
  -e POSTGRES_DB=koreaconcrete \
  -e POSTGRES_USER=koreaconcrete \
  -e POSTGRES_PASSWORD=koreaconcrete \
  -p 5432:5432 -d postgres:16

export DB_URL=jdbc:postgresql://localhost:5432/koreaconcrete
export DB_USERNAME=koreaconcrete
export DB_PASSWORD=koreaconcrete
export JWT_SECRET=replace-with-at-least-32-random-characters
./gradlew bootRun
```

Flyway migration은 애플리케이션 시작 시 자동 실행됩니다. Swagger UI는 `http://localhost:8080/swagger-ui/index.html`에서 확인합니다.

## 프론트엔드 실행

프론트엔드는 별도 서버 없이 Spring Boot가 제공합니다. `frontend/` 폴더의 정적 파일은 Gradle `processResources` 단계에서 `static/` 리소스로 복사됩니다.

백엔드를 실행한 뒤 아래 주소로 접속합니다.

로컬 테스트 URL:

- 프론트엔드: `http://localhost:8080/index.html`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- API Base URL: `http://localhost:8080/api/v1`

API 기본 URL은 `frontend/assets/js/config.js`에서 현재 origin 기준 `/api/v1`로 잡습니다. 필요하면 브라우저 콘솔에서 `localStorage.setItem("civilshop_api_base", "http://localhost:8080/api/v1")`로 바꿀 수 있습니다.

기능별 수동 점검 목록은 `docs/test-checklist.md`를 참고합니다.

## 환경변수

- `DB_URL`: PostgreSQL JDBC URL
- `DB_USERNAME`: DB 사용자
- `DB_PASSWORD`: DB 비밀번호
- `JWT_SECRET`: JWT HMAC secret. 코드에 하드코딩하지 않습니다.
- `JWT_EXPIRATION_SECONDS`: access token 만료 초
- `CORS_ALLOWED_ORIGINS`: 허용 origin 목록
- `SPRING_PROFILES_ACTIVE`: `dev`, `test`, 운영 기본값 등
- `APP_UPLOAD_PROVIDER`: `local` 또는 `gcs`
- `APP_GCS_BUCKET`: GCS 이미지 버킷 이름
- `APP_GCS_PUBLIC_URL_PREFIX`: GCS 이미지 공개 URL prefix. 비우면 `https://storage.googleapis.com/{bucket}` 사용

## Seed 데이터

`dev` 프로필에서만 `DataInitializer`가 실행됩니다.

- 관리자: `admin` / `Password1234!`
- 일반회원: `buyer` / `Password1234!`
- 카테고리: 그레이팅, 수로관, 맨홀, 경계석 하위 구조
- 상품: 그레이팅수로용, 플륨관, 화강경계석
- 가격표, DeliveryOption, LoadingRule, FreightRateRule 포함

## 테스트와 빌드

```bash
./gradlew test
./gradlew build
```

현재 테스트 범위:

- 회원가입 비밀번호 hash
- 로그인 JWT 발급
- 상품 목록/상세 조회
- 운반비 fallback 계산
- 견적요청 생성 및 QuoteItem 저장
- 상담요청 생성
- 관리자 API 익명 접근 거부

## 주요 API

- Auth: `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`, `GET /api/v1/users/me`, `PATCH /api/v1/users/me`
- Categories: `GET /api/v1/categories/tree`, `GET /api/v1/categories/{id}`, `POST/PATCH /api/v1/admin/categories`
- Products: `GET /api/v1/products`, `GET /api/v1/products/search`, `GET /api/v1/products/{id}`, 관리자 상품/variant 생성/수정/상태 변경
- Prices: `GET /api/v1/products/{id}/prices`, 관리자 price book/product price 생성/수정
- Freight: `GET /api/v1/products/{id}/loading-rules`, `POST /api/v1/freight/estimate`, 관리자 상차/운반비 룰 관리
- Cart: `GET/POST/PATCH/DELETE /api/v1/cart`, `POST /api/v1/cart/to-quote`
- Quotes: `POST /api/v1/quotes`, `GET /api/v1/quotes/me`, 관리자 견적 목록/상세/상태/발송
- Consultations: `POST /api/v1/consultations`, `POST /api/v1/consultations/sms`, `POST /api/v1/consultations/call-request`, 관리자 상담 관리
- Search/Admin: 인기/최근 검색어, 대시보드, 회원, 검색 로그, 감사 로그

## 프론트엔드 페이지

- Public: `index.html`, `products.html`, `product-detail.html`, `cart.html`, `quote-new.html`, `login.html`, `signup.html`, `mypage.html`
- Admin: `admin.html`, `admin-categories.html`, `admin-products.html`, `admin-product-form.html`, `admin-prices.html`, `admin-freight.html`, `admin-quotes.html`, `admin-quote-detail.html`, `admin-consultations.html`, `admin-users.html`

## 구현된 기능

- JWT 인증, 관리자 권한 분리, BCrypt password hash
- 트리형 카테고리
- 상품, 규격 variant, 스펙, ProductMedia, 연관/대체 관계 구조
- 가격표와 variant별 가격
- 상차수량 및 지역/차량 기반 운반비 fallback 계산
- 장바구니, 견적요청, 상담요청
- 검색 로그, 인기/최근 검색어
- 관리자 대시보드와 주요 관리 화면
- Flyway migration과 dev seed

## 제외된 기능

자료실, 게시판, 공지사항, FAQ, 도면자료, 물가자료, 카탈로그 게시판, 게시글 첨부파일, 게시판 관리자 기능은 구현하지 않았습니다. `Board`, `Post`, `FileAsset`, `BoardKey`, `FileOwnerType` 엔티티도 만들지 않았습니다.

## 설계상 가정

- MVP 운반비는 주소 문자열의 시/도 포함 여부로만 룰을 매칭합니다.
- 실제 결제는 제외하고 견적요청/상담 중심으로 설계했습니다.
- 로그인 사용자의 장바구니는 서버 API(`/api/v1/cart`)에 저장됩니다. localStorage 장바구니 값은 비로그인 fallback 용도로만 남겨져 있습니다.
- 운영 환경에서는 `JWT_SECRET`, DB 정보, CORS origin을 반드시 외부 환경변수로 제공합니다.

## 남은 TODO

- 관리자 상품 form에서 variant/spec/media/relation을 한 화면에서 편집하는 고급 UI
- 가격/운반비 룰 목록 및 수정 UI 확장
- 견적서 PDF/메일 발송
- 파일 업로드가 아닌 상품 이미지 관리용 별도 스토리지 연동
- 실제 거리 기반 운반비 계산 또는 지도 API 연동

## 다음 단계 추천

1. 관리자 상품/variant 편집 UX를 보강합니다.
2. PostgreSQL 개발 DB에서 Flyway migration을 검증합니다.
3. 실제 운영 도메인과 CORS origin을 확정합니다.
