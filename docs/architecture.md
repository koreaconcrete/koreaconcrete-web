# Architecture Notes

## MVP 경계

이 MVP는 토목자재 B2B 견적 흐름을 우선합니다. 상품 탐색, 규격 선택, 운반비 추정, 견적요청, 상담요청, 관리자 처리를 포함하고 즉시 결제와 게시판/자료실은 제외합니다.

## 핵심 도메인

- `Product`는 카탈로그의 기본 상품입니다.
- `ProductVariant`는 폭/길이/높이/중량/단위 같은 규격 단위입니다.
- `ProductPrice`는 `PriceBook`과 variant에 연결되어 가격 이력을 쌓을 수 있는 구조입니다.
- `LoadingRule`과 `FreightRateRule`은 운반비 추정을 위한 MVP 룰입니다.
- `QuoteRequest`와 `Consultation`은 B2B 전환 이벤트입니다.

## 운반비 계산

1. 요청 preferred vehicle이 있으면 우선 사용합니다.
2. 없으면 variant의 LoadingRule 중 적재량이 큰 룰을 선택합니다.
3. destinationAddress에 destinationRegion 문자열이 포함된 활성 FreightRateRule을 찾습니다.
4. 룰이 없으면 300,000원 fallback을 사용하고 안내 notes를 반환합니다.
5. 계산 근거는 `FreightEstimate.calculationSnapshot`에 JSON 문자열로 저장합니다.

## 보안

JWT access token은 HMAC SHA-256으로 발급합니다. secret은 `JWT_SECRET` 환경변수로만 주입합니다. 관리자 API는 Spring Security matcher로 권한을 분리합니다.
