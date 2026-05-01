# 실시간 환율 기반 외환 주문 시스템

USD, JPY, CNY, EUR 4개 통화에 대해 외부 환율 API를 연동하여 실시간 환율 정보를 수집하고,
KRW 기준 외화 매수/매도 주문을 처리하는 백엔드 시스템입니다.

---

## 기술 스택

- **Language**: Java 17
- **Framework**: Spring Boot 3.4.5
- **Database**: H2 (In-Memory)
- **ORM**: Spring Data JPA
- **Build**: Gradle
- **Docs**: Swagger (springdoc-openapi)
- **Code Style**: Naver Checkstyle

---

## 실행 방법

### 요구사항
- Java 17 이상

### 실행

```bash
./gradlew bootRun
```

별도의 환경 변수 설정 없이 바로 실행 가능합니다.
애플리케이션 시작 즉시 스케줄러가 동작하여 환율 데이터를 수집합니다.

> **외부 API 키**: ExchangeRate-API의 무료 키가 `application.yml`에 포함되어 있습니다.
> 환경 변수(`EXCHANGE_RATE_API_KEY`)로 키를 교체할 수 있으며, 설정하지 않을 경우 내장 키를 사용합니다.
> API 호출 실패 시 스케줄러는 에러 로그만 남기고 다음 주기에 재시도합니다.

### 테스트

```bash
./gradlew test
```

### 빌드 (테스트 + Checkstyle 포함)

```bash
./gradlew build
```

### 확인 가능한 URL

| 서비스 | URL |
|--------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console |

**H2 Console 접속 정보**
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (없음)

> H2 In-Memory DB 사용으로 애플리케이션 재시작 시 데이터가 초기화됩니다.

---

## 프로젝트 구조

```
src/main/java/com/switchwon/devbehomework
├── common
│   ├── dto/            # ApiResponse (공통 응답 wrapper)
│   ├── enums/          # CurrencyCode, ForeignCurrency, ErrorCode
│   └── exception/      # BusinessException, GlobalExceptionHandler
├── config/             # RestTemplate, Scheduling, Swagger 설정
├── exchangerate
│   ├── controller/     # GET /exchange-rate/latest
│   ├── dto/            # ExchangeRateResponse, ExchangeRateListResponse
│   ├── entity/         # ExchangeRate
│   ├── provider/       # ExchangeRateProvider (인터페이스), ExchangeRateApiProvider
│   ├── repository/     # ExchangeRateRepository
│   └── service/        # ExchangeRateService
├── order
│   ├── controller/     # POST /order, GET /order/list
│   ├── dto/            # OrderRequest, OrderCreateResponse, OrderDetailResponse, OrderListResponse
│   ├── entity/         # Order
│   ├── repository/     # OrderRepository
│   └── service/        # OrderService
└── scheduler/          # ExchangeRateScheduler (1분 주기)
```

---

## API 명세

### 공통 응답 형식

**성공**
```json
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": { }
}
```

**실패**
```json
{
  "code": "400",
  "message": "에러 메시지",
  "returnObject": null
}
```

### HTTP 상태 코드

| 상태 코드 | 설명 |
|-----------|------|
| 200 | 성공 |
| 400 | 잘못된 요청 (유효성 검사 실패, 잘못된 통화 코드, 동일 통화 주문 등) |
| 404 | 환율 정보 없음 |
| 500 | 외부 API 호출 실패 |

---

### 환율 API

#### 전체 통화 최신 환율 조회

```
GET /exchange-rate/latest
```

```json
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "exchangeRateList": [
      {
        "currency": "USD",
        "tradeStanRate": 1350.00,
        "buyRate": 1417.50,
        "sellRate": 1282.50,
        "dateTime": "2026-05-01T20:00:00"
      },
      {
        "currency": "JPY",
        "tradeStanRate": 900.00,
        "buyRate": 945.00,
        "sellRate": 855.00,
        "dateTime": "2026-05-01T20:00:00"
      }
    ]
  }
}
```

#### 특정 통화 환율 조회

```
GET /exchange-rate/latest/{currency}
```

- `currency`: `USD` | `JPY` | `CNY` | `EUR`
- 잘못된 통화 코드 입력 시 `400` 반환
- 환율 데이터 없을 시 `404` 반환

---

### 주문 API

#### 요청 유효성 규칙

| 필드 | 규칙 |
|------|------|
| `forexAmount` | 필수, 0보다 큰 양수 |
| `fromCurrency` | 필수, `KRW`/`USD`/`JPY`/`CNY`/`EUR` 중 하나 |
| `toCurrency` | 필수, `KRW`/`USD`/`JPY`/`CNY`/`EUR` 중 하나 |
| 통화 쌍 | 반드시 한쪽이 `KRW`여야 함 (예: KRW↔USD) |
| 동일 통화 | `fromCurrency == toCurrency` 불가 |

#### 외화 매수 (KRW → 외화)

```
POST /order
```

```json
// Request
{
  "forexAmount": 200,
  "fromCurrency": "KRW",
  "toCurrency": "USD"
}

// Response (적용 환율: buyRate)
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "fromAmount": 296086,
    "fromCurrency": "KRW",
    "toAmount": 200,
    "toCurrency": "USD",
    "tradeRate": 1480.43,
    "dateTime": "2026-05-01T20:00:00"
  }
}
```

#### 외화 매도 (외화 → KRW)

```
POST /order
```

```json
// Request
{
  "forexAmount": 133,
  "fromCurrency": "USD",
  "toCurrency": "KRW"
}

// Response (적용 환율: sellRate)
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "fromAmount": 133,
    "fromCurrency": "USD",
    "toAmount": 196104,
    "toCurrency": "KRW",
    "tradeRate": 1474.47,
    "dateTime": "2026-05-01T20:00:00"
  }
}
```

#### 주문 내역 조회

```
GET /order/list
```

```json
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "orderList": [
      {
        "id": 1,
        "fromAmount": 296086,
        "fromCurrency": "KRW",
        "toAmount": 200,
        "toCurrency": "USD",
        "tradeRate": 1480.43,
        "dateTime": "2026-05-01T20:00:00"
      }
    ]
  }
}
```

---

## 주요 구현 사항

### 환율 수집

- ExchangeRate-API (`v6.exchangerate-api.com`) 연동, USD 기준 환율을 크로스 레이트로 계산하여 KRW 기준 환율로 변환
- JPY: 100엔 단위 환산 적용
- 매 1분마다 스케줄러를 통해 환율 수집 및 DB 저장
- 수집 시 ±0.5% 랜덤 변동 적용 (주말·공휴일 환율 변동 없음 대응)
- API 호출 실패 시 에러 로그만 남기고 다음 주기에 재시도

### 환율 계산 규칙

| 항목 | 규칙 |
|------|------|
| 환율 소수점 | 둘째 자리까지 반올림 |
| JPY 단위 | 100엔 기준 환산 |
| KRW 환산 금액 | 소수점 이하 버림 (Floor) |
| 전신환 매입율 (buyRate) | 매매기준율 × 1.05 — 고객이 외화를 살 때 적용 |
| 전신환 매도율 (sellRate) | 매매기준율 × 0.95 — 고객이 외화를 팔 때 적용 |

### 설계

- **레이어드 아키텍처**: Controller → Service → Repository
- **ExchangeRateProvider 인터페이스**: 외부 API 교체에 유연한 구조 (전략 패턴 확장 가능)
- **응답 DTO**: 불변 데이터는 Java Record 적용
- **요청 DTO**: Bean Validation 포함 클래스 유지
- **Entity 정적 팩토리**: `Order.of()`, `ExchangeRate.of()`로 생성 책임 분리
- **전역 예외 처리**: `BusinessException`, `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`

---

## 테스트

### 테스트 구성

| 구분 | 방식 | 주요 시나리오 |
|------|------|--------------|
| Controller | `@WebMvcTest` | 매수/매도 주문, 유효성 실패, 잘못된 enum, 주문 목록 조회 |
| Service | `@ExtendWith(MockitoExtension.class)` | buyRate/sellRate 적용, KRW 버림 처리, 동일 통화·잘못된 통화 쌍 예외 |
| ExchangeRateService | `@ExtendWith(MockitoExtension.class)` | 환율 저장, 전체/단일 환율 조회, 환율 없을 시 예외 |
| ExchangeRateApiProvider | `MockRestServiceServer` | API 파싱, 스프레드 계산, JPY 100엔 환산, 서버 오류·실패 응답 처리 |

---

## 제약 사항

- H2 In-Memory DB 사용으로 애플리케이션 재시작 시 데이터 초기화됨
- 인증/세션 미구현 (과제 요구사항에 따라 제외)
- 지원 통화: `USD`, `JPY`, `CNY`, `EUR` (KRW 기준 주문만 가능)
- 랜덤 환율 변동(±0.5%)은 주말·공휴일 무변동 대응을 위한 데모용 처리
- 외부 환율 API 의존성 있음 (호출 실패 시 해당 주기 환율 수집 건너뜀)
