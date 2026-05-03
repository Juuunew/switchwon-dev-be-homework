# 최신 환율 기반 외환 주문 시스템

USD, JPY, CNY, EUR 4개 통화에 대해 외부 환율 API를 연동하여 주기적으로 환율 정보를 수집하고,
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
> API 호출 실패 시 Frankfurter → Mock Provider 순서로 자동 fallback합니다.

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
│   ├── config/         # ClockConfig, TransactionConfig
│   ├── dto/            # ApiResponse (공통 응답 wrapper)
│   ├── enums/          # ErrorCode
│   └── exception/      # BusinessException, GlobalExceptionHandler
├── config/             # RestClientConfig, SchedulingConfig, SwaggerConfig
├── currency/           # CurrencyCode, ForeignCurrency
├── exchangerate
│   ├── collection/     # ExchangeRateCollector (인터페이스)
│   │                   # ProviderFallbackExchangeRateCollector
│   │                   # ExchangeRateCollectionScheduler
│   ├── controller/     # GET /exchange-rate/latest
│   ├── dto/            # ExchangeRateResponse, ExchangeRateListResponse
│   ├── entity/         # ExchangeRateEntity
│   ├── provider/       # ExchangeRateProvider (인터페이스), ProviderRate
│   │                   # ExchangeRateApiProvider, FrankfurterExchangeRateProvider
│   │                   # MockExchangeRateProvider, ProviderConfig
│   ├── repository/     # ExchangeRateRepository
│   └── service/        # ExchangeRateService
└── order
    ├── controller/     # POST /order, GET /order/list
    ├── dto/            # OrderRequest, OrderResponse, OrderListResponse
    ├── entity/         # ExchangeOrderRequestEntity, ExchangeOrderEntity
    ├── enums/          # OrderDirection, OrderRequestStatus
    ├── repository/     # ExchangeOrderRequestRepository, ExchangeOrderRepository
    └── service/        # OrderService
```

---

## DB 스키마

### exchange_rate

환율 수집 이력. 통화쌍별로 수집마다 row가 추가됩니다.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| from_currency | VARCHAR(10) | 외화 (USD/JPY/CNY/EUR) |
| to_currency | VARCHAR(10) | 기준 통화 (현재 KRW 고정) |
| base_rate | DECIMAL(12,2) | 기준율 |
| buy_rate | DECIMAL(12,2) | 고객 매수 적용 환율 |
| sell_rate | DECIMAL(12,2) | 고객 매도 적용 환율 |
| provider | VARCHAR(50) | 수집 provider 이름 |
| date_time | DATETIME | 수집 시각 |

인덱스: `idx_er_from_to_datetime (from_currency, to_currency, date_time)` — 통화쌍별 최신 환율 조회에 사용

### exchange_order_request

주문 요청 감사 로그. Bean Validation을 통과해 서비스에 진입한 모든 요청이 기록됩니다.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| forex_amount | DECIMAL(19,2) | 요청 외화 수량 |
| from_currency | VARCHAR(10) | 출발 통화 |
| to_currency | VARCHAR(10) | 도착 통화 |
| status | VARCHAR(10) | RECEIVED / SUCCESS / FAILED |
| failure_code | VARCHAR(50) | 실패 코드 (nullable) |
| failure_message | VARCHAR(255) | 실패 메시지 (nullable) |
| requested_at | DATETIME | 요청 수신 시각 |
| completed_at | DATETIME | 처리 완료 시각 (nullable) |

### exchange_order

체결된 주문. `request_id`로 원 요청과 1:1 연결됩니다.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| request_id | BIGINT UNIQUE | exchange_order_request.id 참조 |
| direction | VARCHAR(4) | BUY / SELL |
| currency | VARCHAR(3) | 외화 코드 |
| forex_amount | DECIMAL(19,2) | 외화 수량 |
| krw_amount | DECIMAL(19,0) | KRW 환산 금액 (Floor) |
| trade_rate | DECIMAL(12,2) | 적용 환율 |
| rate_date_time | DATETIME | 환율 수집 시각 |
| created_at | DATETIME | 주문 생성 시각 |

---

## API 명세

### 공통 응답 형식

아래 형식은 비즈니스 오류 및 유효성 검사 실패에 적용됩니다.

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
| 400 | 잘못된 요청 (유효성 검사 실패, 지원하지 않는 통화 코드, 동일 통화 주문 등) |
| 404 | 환율 정보 없음 |
| 503 | 환율 정보 만료 (최신 환율 수집 후 5분 초과) |

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
        "dateTime": "2026-05-02T20:00:00"
      }
    ]
  }
}
```

#### 특정 통화 환율 조회

```
GET /exchange-rate/latest/{currency}
```

- `currency`: `USD` | `JPY` | `CNY` | `EUR` (대문자 enum 값만 허용)
- 잘못된 통화 코드 입력 시 `400` 반환
- 환율 데이터 없을 시 `404` 반환

---

### 주문 API

매수/매도는 동일한 `POST /order` endpoint를 사용하며 `fromCurrency`, `toCurrency` 조합으로 방향을 판별합니다.

#### 요청 유효성 규칙

| 필드 | 규칙 |
|------|------|
| `forexAmount` | 필수, 0보다 큰 양수 |
| `fromCurrency` | 필수, `KRW`/`USD`/`JPY`/`CNY`/`EUR` 중 하나 (대문자만 허용) |
| `toCurrency` | 필수, `KRW`/`USD`/`JPY`/`CNY`/`EUR` 중 하나 (대문자만 허용) |
| 통화 쌍 | 반드시 한쪽이 `KRW`여야 함 |
| 동일 통화 | `fromCurrency == toCurrency` 불가 |

#### 외화 매수 (KRW → 외화)

**Request**
```json
{
  "forexAmount": 200,
  "fromCurrency": "KRW",
  "toCurrency": "USD"
}
```

**Response**
```json
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "id": 1,
    "fromAmount": 283500,
    "fromCurrency": "KRW",
    "toAmount": 200,
    "toCurrency": "USD",
    "tradeRate": 1417.50,
    "dateTime": "2026-05-02T20:00:00"
  }
}
```

#### 외화 매도 (외화 → KRW)

**Request**
```json
{
  "forexAmount": 133,
  "fromCurrency": "USD",
  "toCurrency": "KRW"
}
```

**Response**
```json
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "id": 2,
    "fromAmount": 133,
    "fromCurrency": "USD",
    "toAmount": 170572,
    "toCurrency": "KRW",
    "tradeRate": 1282.50,
    "dateTime": "2026-05-02T20:00:00"
  }
}
```

#### 주문 내역 조회 (최신 주문순)

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
        "id": 2,
        "fromAmount": 133,
        "fromCurrency": "USD",
        "toAmount": 170572,
        "toCurrency": "KRW",
        "tradeRate": 1282.50,
        "dateTime": "2026-05-02T20:00:00"
      }
    ]
  }
}
```

---

## 주요 구현 사항

### 환율 수집 구조

환율 수집은 `ExchangeRateCollector` 인터페이스를 통해 추상화되어 있으며,
`ProviderFallbackExchangeRateCollector`가 통화별로 독립적인 fallback 수집을 담당합니다.

```
Scheduler → ExchangeRateCollector
               └── ForeignCurrency.values() 순회
                    └── 각 통화에 대해 Provider 순서대로 시도
                         ├── ExchangeRateApiProvider (1순위, USD-base 응답 30초 캐시)
                         ├── FrankfurterExchangeRateProvider (2순위)
                         └── MockExchangeRateProvider (3순위)
```

- 한 통화의 수집 실패가 다른 통화에 영향을 주지 않음
- Provider 우선순위는 `exchange-rate.collection.providers` 설정으로 제어
- 수집 주기: `fixedDelay` 방식 (이전 수집 완료 후 1분 대기)

### Provider 구성

| Provider | 설명 | 우선순위 |
|----------|------|---------|
| ExchangeRateApiProvider | ExchangeRate-API v6 연동 | 1순위 |
| FrankfurterExchangeRateProvider | Frankfurter 공개 API 연동 | 2순위 |
| MockExchangeRateProvider | 고정 환율 반환 (API 장애 대비) | 3순위 |

### 환율 계산 규칙

| 항목 | 규칙 |
|------|------|
| 크로스레이트 | `unitRate = KRW_per_USD / FOREIGN_per_USD` |
| 기준율 | `baseRate = unitRate × rateUnit` (JPY: ×100) |
| 고객 매수 적용 환율 (buyRate) | `baseRate × 1.05` |
| 고객 매도 적용 환율 (sellRate) | `baseRate × 0.95` |
| 환율 저장 값 | 소수 둘째 자리 `HALF_UP` 반올림 |
| KRW 환산 금액 | 소수점 이하 버림 (Floor) |
| BigDecimal | 금융 연산 전체에 적용, double 파싱 없음 |

### Sanity Check

수집된 환율이 직전 값 대비 일정 기준을 초과하면 이상 데이터로 판단합니다.

| 변동폭 | 처리 |
|--------|------|
| 5% 이상 15% 미만 | WARN 로그 기록 후 저장 |
| 15% 이상 | 해당 통화 수집 스킵 |

### 주문 감사 로그

Bean Validation을 통과해 서비스에 진입한 주문 요청은 처리 결과와 관계없이 `exchange_order_request` 테이블에 기록됩니다.

| 상태 | 시점 |
|------|------|
| `RECEIVED` | 요청 수신 즉시 |
| `SUCCESS` | 주문 처리 완료 |
| `FAILED` | 처리 중 오류 발생 (오류 코드/메시지 포함) |

처리 도중 서버가 종료되더라도 `RECEIVED` 상태로 요청이 남아 추적 가능합니다.

### 환율 신선도 검증

수집 실패 시 기존 환율이 DB에 남을 수 있으므로, 주문 시점에서 freshness를 재검증합니다.
DB에서 조회한 환율의 수집 시각이 기준 시간(기본 5분)을 초과하면 `503 RATE_STALE`을 반환합니다.

---

## 설계 결정

### `ExchangeRateCollector` 인터페이스 도입

Scheduler는 수집 시점만 관리하고, Collector는 통화별 fallback과 저장 정책을 책임집니다.
수집 정책 변경이 Scheduler에 전파되지 않도록 분리했으며, fallback 전략 자체를 교체 가능합니다.

### Provider 단위 계약 (`supports()` + `fetchRate(from, to)`)

Provider가 전체 환율 목록을 반환하면 어떤 통화가 누락됐는지 판단하기 어렵습니다.
통화쌍 단위로 `supports()` 체크 후 개별 요청하는 방식으로 수집 성공/실패를 Collector 레벨에서 명확히 추적합니다.

### 주문 테이블 분리 (`exchange_order_request` / `exchange_order`)

요청과 체결 주문을 하나의 테이블에 두면 실패 요청과 성공 주문이 섞여 스키마에 nullable 컬럼이 늘어납니다.
테이블을 분리해 각 테이블을 NOT NULL 위주로 단순하게 유지했으며,
성공 주문은 `exchange_order.request_id`(unique)로 원 요청과 1:1 연결됩니다.

### REQUIRES_NEW 트랜잭션 전략

주문 처리 실패 시 `FAILED` 상태를 기록하려면 롤백이 일어나도 요청 로그는 커밋되어야 합니다.
성공 시 2개(TX-1: RECEIVED 저장, TX-2: 주문 처리 + SUCCESS 갱신),
실패 시 최대 3개(TX-3: FAILED 갱신)의 독립 트랜잭션으로 분리하여 어느 단계에서 실패하더라도 요청 이력이 보존됩니다.

---

## 테스트

| 구분 | 방식 | 주요 시나리오 |
|------|------|--------------|
| OrderController | `@WebMvcTest` | 매수/매도 주문, 유효성 실패, 주문 목록 조회 |
| OrderService | `@ExtendWith(MockitoExtension.class)` | buyRate/sellRate 적용, KRW 버림 처리, 동일 통화 예외, 환율 만료 예외, 감사 로그 상태 전이 |
| ProviderFallbackExchangeRateCollector | `@ExtendWith(MockitoExtension.class)` | 스프레드 계산, JPY rateUnit, fallback, 전체 실패, supports() 미지원, Sanity Check |
| ExchangeRateService | `@ExtendWith(MockitoExtension.class)` | 전체/단일 환율 조회, 환율 없을 시 예외 |
| ExchangeRateApiProvider | `@ExtendWith(MockitoExtension.class)` | USD/JPY 크로스레이트, supports(), API 오류 처리 |
| ExchangeRateScheduler | `@ExtendWith(MockitoExtension.class)` | Collector 호출 검증 |

Repository 및 API 통합 테스트는 포함되지 않습니다.

---

## 제약 사항

- H2 In-Memory DB 사용으로 애플리케이션 재시작 시 데이터 초기화됨
- 인증/세션 미구현 (과제 요구사항에 따라 제외)
- 지원 통화: `USD`, `JPY`, `CNY`, `EUR` (KRW 기준 주문만 가능)
- 분산 환경에서의 스케줄러 중복 실행 방지 미적용 (단일 인스턴스 기준)
