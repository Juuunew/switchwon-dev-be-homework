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
애플리케이션 시작 시 최초 1회 환율 데이터를 수집하고, 이후 1분마다 최신 환율을 다시 수집합니다.

> **외부 API 키**: ExchangeRate-API의 무료 키가 `application.yml`에 포함되어 있습니다.
> 다른 키로 실행해야 하는 경우 아래처럼 명령 앞에 환경 변수를 붙이면 해당 실행 프로세스에서만 `application.yml`의 기본 키보다 우선 적용됩니다.
> ```bash
> EXCHANGE_RATE_API_KEY=your_api_key ./gradlew bootRun
> ```
> 설정하지 않을 경우 `application.yml`의 기본 키를 사용합니다.
> API 호출 실패 시 Frankfurter → Mock Provider 순서로 자동 fallback합니다.

환율 관련 설정은 `application.yml`의 `exchange-rate` namespace 아래에서 관리합니다.

- `exchange-rate.collection.providers`: Provider fallback 순서
- `exchange-rate.initial-collection.enabled`: 애플리케이션 시작 시 최초 수집 실행 여부
- `exchange-rate.schedule.*`: 환율 수집 스케줄
- `exchange-rate.providers.exchange-rate-api.*`: ExchangeRate-API 설정
- `exchange-rate.providers.frankfurter.*`: Frankfurter API 설정

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
│   ├── enums/          # ErrorCode
│   └── exception/      # BusinessException, GlobalExceptionHandler
├── config/             # ClockConfig, TransactionConfig, RestClientConfig, SchedulingConfig, SwaggerConfig
├── currency/           # CurrencyCode, ForeignCurrency
├── exchangerate
│   ├── collection/     # ExchangeRateCollector (인터페이스)
│   │                   # ProviderFallbackExchangeRateCollector
│   │                   # ExchangeRateInitialCollector
│   │                   # ExchangeRateCollectionScheduler
│   │                   # ExchangeRatePersistenceService
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

#### 주문 내역 조회 (최신 주문순, 페이징)

```
GET /order/list?page=0&size=20
```

| 파라미터 | 기본값 | 설명 |
|---------|--------|------|
| `page` | 0 | 페이지 번호 (0부터 시작) |
| `size` | 20 | 페이지당 항목 수 |
| `sort` | `createdAt,desc` | 정렬 기준 |

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
    ],
    "totalPages": 1,
    "totalElements": 1,
    "page": 0,
    "size": 20
  }
}
```

---

## 설계 결정

### 환율 수집: Collector-Provider 분리

Scheduler는 "언제 수집할지"만 결정하고, Collector는 "어떻게 수집할지"(fallback 순서, 통화별 독립 처리)를 책임집니다.
수집 정책을 바꿔도 Scheduler는 변경할 필요가 없고, `ExchangeRateCollector` 인터페이스를 통해 fallback 전략 자체를 교체할 수 있습니다.

```
InitialCollector / Scheduler
  → ExchangeRateCollector
      └── ForeignCurrency.values() 순회
          └── 각 통화에 대해 Provider 순서대로 시도
              ├── ExchangeRateApiProvider (1순위, USD-base 응답 30초 캐시)
              ├── FrankfurterExchangeRateProvider (2순위)
              └── MockExchangeRateProvider (3순위)
          └── ExchangeRatePersistenceService에서 검증 후 저장
```

한 통화의 수집 실패가 다른 통화에 영향을 주지 않으며, Sanity Check 실패 시에도 다음 Provider로 자동 전환됩니다.
외부 API 호출은 트랜잭션 밖에서 수행하고, 직전 환율 조회 및 저장만 트랜잭션으로 처리합니다.

### Provider 계약: `supports()` + `fetchRate(from, to)`

Provider가 전체 환율 목록을 한 번에 반환하면, 어떤 통화가 누락됐는지 Collector가 알기 어렵습니다.
통화쌍 단위로 `supports()` 체크 후 개별 요청하는 방식으로, 수집 성공/실패를 Collector 레벨에서 명확히 추적합니다.

통화 수만큼 API 호출이 증가하는 단점이 있지만, ExchangeRateApiProvider는 USD-base 응답을 30초 캐시하여 실제로는 1회 호출로 처리됩니다.

| Provider | 설명 | 우선순위 |
|----------|------|---------|
| ExchangeRateApiProvider | ExchangeRate-API v6 연동 | 1순위 |
| FrankfurterExchangeRateProvider | Frankfurter 공개 API 연동 | 2순위 |
| MockExchangeRateProvider | 고정 환율 반환 (API 장애 대비) | 3순위 |

### 크로스레이트 계산

ExchangeRate-API, Frankfurter 모두 USD 기준 응답을 반환합니다.
KRW/외화 환율을 직접 제공하지 않으므로, `KRW_per_USD / FOREIGN_per_USD`로 크로스레이트를 산출합니다.
Provider마다 base currency가 다를 수 있으므로, `unitRate`(외화 1단위의 KRW 가격)라는 공통 단위로 변환한 뒤 PersistenceService에서 rateUnit과 스프레드를 적용합니다.

| 항목 | 규칙 |
|------|------|
| 크로스레이트 | `unitRate = KRW_per_USD / FOREIGN_per_USD` |
| 기준율 | `baseRate = unitRate × rateUnit` (JPY: ×100) |
| 고객 매수 적용 환율 (buyRate) | `baseRate × 1.05` |
| 고객 매도 적용 환율 (sellRate) | `baseRate × 0.95` |
| 환율 저장 값 | 소수 둘째 자리 `HALF_UP` 반올림 |
| KRW 환산 금액 | 소수점 이하 버림 (Floor) |
| BigDecimal | 금융 연산 전체에 적용, double 파싱 없음 |

### Sanity Check: 3단계 변동폭 감시

수집된 환율이 직전 값 대비 일정 기준을 초과하면 이상 데이터로 판단합니다.

| 변동폭 | 처리 |
|--------|------|
| 5% 미만 | 정상 저장 |
| 5% 이상 15% 미만 | WARN 로그 기록 후 저장 |
| 15% 이상 | 해당 통화 수집 스킵 |

15% 이상일 때 "이전 값 유지" 대신 "스킵"을 선택한 이유는, 이상 데이터를 저장하면 이후 Sanity Check의 기준점이 오염되기 때문입니다.
스킵하고 다음 Provider에게 기회를 주는 것이 더 안전합니다.
5% 경고는 실제 시장 급변동일 수 있으므로 저장하되 로그로 감시합니다.

### 주문 테이블 분리 (`exchange_order_request` / `exchange_order`)

요청과 체결 주문을 하나의 테이블에 두면 실패 요청과 성공 주문이 섞여 스키마에 nullable 컬럼이 늘어납니다.
테이블을 분리해 각 테이블을 NOT NULL 위주로 단순하게 유지했으며,
성공 주문은 `exchange_order.request_id`(unique)로 원 요청과 1:1 연결됩니다.

JOIN 비용이 생기지만, 각 테이블이 단일 목적으로 단순해져 유지보수가 쉬워집니다.

### REQUIRES_NEW 트랜잭션 전략

주문 처리 실패 시 `FAILED` 상태를 기록하려면 롤백이 일어나도 요청 로그는 커밋되어야 합니다.
성공 시 2개(TX-1: RECEIVED 저장, TX-2: 주문 처리 + SUCCESS 갱신),
실패 시 최대 3개(TX-3: FAILED 갱신)의 독립 트랜잭션으로 분리하여 어느 단계에서 실패하더라도 요청 이력이 보존됩니다.

`@Transactional` 대신 `TransactionTemplate`을 사용한 이유는, 같은 메서드 안에서 여러 독립 트랜잭션이 필요하기 때문입니다.
프록시 기반 `@Transactional`은 자기 호출(self-invocation)에서 새 트랜잭션을 열지 못하므로, 프로그래밍 방식이 더 적합합니다.

### 환율 신선도: 주문 시점 검증

환율 수집은 배경 작업이라 실시간성 요구가 없고, 데이터 품질은 Sanity Check가 담당합니다.
그러나 수집 실패가 연속되면 DB에 오래된 환율만 남을 수 있습니다.
이때 오래된 환율로 주문이 체결되는 것을 막아야 하므로, 신선도는 "사용 시점"에 판단합니다.

DB에서 조회한 환율의 수집 시각이 기준 시간(기본 5분)을 초과하면 `503 RATE_STALE`을 반환합니다.

### fixedDelay 스케줄링

`fixedRate`는 이전 작업이 아직 끝나지 않았을 때 중복 실행 위험이 있습니다.
`fixedDelay`는 완료 후 대기하므로 Provider 응답이 느려져도 안전합니다.
수집 주기가 살짝 늘어날 수 있으나, 1분 간격 수집에서는 무시 가능한 수준입니다.

---

## 구현하지 않은 것

- **분산 스케줄러 중복 방지**: 단일 인스턴스 과제 범위에서 ShedLock 등은 오버엔지니어링
- **인증/세션**: 과제 요구사항 외
- **통합 테스트**: 단위 테스트로 비즈니스 로직 커버를 우선했으며, H2 환경에서 Repository 테스트의 가치가 낮음
- **지원 통화 확장**: 현재 `USD`, `JPY`, `CNY`, `EUR` 4개 (KRW 기준 주문만 가능)

---

## 테스트

| 구분 | 방식 | 주요 시나리오 |
|------|------|--------------|
| OrderController | `@WebMvcTest` | 매수/매도 주문, 유효성 실패, 주문 목록 조회 |
| OrderService | `@ExtendWith(MockitoExtension.class)` | buyRate/sellRate 적용, KRW 버림 처리, 동일 통화 예외, 환율 만료 예외, 감사 로그 상태 전이 |
| ProviderFallbackExchangeRateCollector | `@ExtendWith(MockitoExtension.class)` | fallback, 전체 실패, supports() 미지원, 저장 정책 거절 시 다음 Provider 시도 |
| ExchangeRatePersistenceService | `@ExtendWith(MockitoExtension.class)` | 스프레드 계산, JPY rateUnit, unitRate 검증, Sanity Check |
| ExchangeRateService | `@ExtendWith(MockitoExtension.class)` | 전체/단일 환율 조회, 환율 없을 시 예외 |
| ExchangeRateApiProvider / FrankfurterExchangeRateProvider | `@ExtendWith(MockitoExtension.class)` | USD/JPY 크로스레이트, supports(), API 오류 및 결측/0 응답 처리 |
| ExchangeRateInitialCollector / ExchangeRateScheduler | `@ExtendWith(MockitoExtension.class)` | 초기 수집 및 스케줄 수집 호출 검증 |

Repository 및 API 통합 테스트는 포함되지 않습니다.

