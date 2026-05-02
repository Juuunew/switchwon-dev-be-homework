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
│   ├── config/         # ClockConfig
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
    ├── dto/            # OrderRequest, OrderCreateResponse, OrderDetailResponse, OrderListResponse
    ├── entity/         # Order
    ├── repository/     # OrderRepository
    └── service/        # OrderService
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
| 503 | 환율 정보 만료 |
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
| 통화 쌍 | 반드시 한쪽이 `KRW`여야 함 |
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

// Response
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "fromAmount": 296086,
    "fromCurrency": "KRW",
    "toAmount": 200,
    "toCurrency": "USD",
    "tradeRate": 1480.43,
    "dateTime": "2026-05-02T20:00:00"
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

// Response
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "fromAmount": 133,
    "fromCurrency": "USD",
    "toAmount": 196104,
    "toCurrency": "KRW",
    "tradeRate": 1474.47,
    "dateTime": "2026-05-02T20:00:00"
  }
}
```

#### 주문 내역 조회

```
GET /order/list
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
                         ├── ExchangeRateApiProvider (1순위)
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
| 전신환 매입율 (buyRate) | `baseRate × 1.05` |
| 전신환 매도율 (sellRate) | `baseRate × 0.95` |
| KRW 환산 금액 | 소수점 이하 버림 (Floor) |
| BigDecimal | 금융 연산 전체에 적용, double 파싱 없음 |

### Sanity Check

수집된 환율이 직전 값 대비 일정 기준을 초과하면 이상 데이터로 판단합니다.

| 변동폭 | 처리 |
|--------|------|
| 5% 이상 | WARN 로그 기록 후 저장 |
| 15% 이상 | 해당 통화 수집 스킵 |

### 설계 결정

**`ExchangeRateCollector` 인터페이스 도입**

Fallback 전략 자체를 교체 가능하도록 수집 계층을 인터페이스로 분리했습니다.
Provider 목록을 Collector가 직접 보유하며, Scheduler는 `collectAll()`만 호출합니다.

**Provider 단위 계약 (`supports()` + `fetchRate(from, to)`)**

기존에 Provider가 전체 환율 목록을 반환하는 방식은 누락된 통화를 검증할 수 없는 문제가 있었습니다.
통화쌍 단위로 `supports()` 체크 후 개별 요청하는 방식으로 변경하여,
어떤 통화가 수집되지 않았는지 Collector 레벨에서 명확히 파악할 수 있습니다.

---

## 테스트

| 구분 | 방식 | 주요 시나리오 |
|------|------|--------------|
| Controller | `@WebMvcTest` | 매수/매도 주문, 유효성 실패, 주문 목록 조회 |
| OrderService | `@ExtendWith(MockitoExtension.class)` | buyRate/sellRate 적용, KRW 버림 처리, 동일 통화 예외 |
| ExchangeRateService | `@ExtendWith(MockitoExtension.class)` | 전체/단일 환율 조회, 환율 없을 시 예외 |
| ExchangeRateApiProvider | `@ExtendWith(MockitoExtension.class)` | USD/JPY 크로스레이트, supports(), API 오류 처리 |
| ExchangeRateScheduler | `@ExtendWith(MockitoExtension.class)` | Collector 호출 검증 |

---

## 제약 사항

- H2 In-Memory DB 사용으로 애플리케이션 재시작 시 데이터 초기화됨
- 인증/세션 미구현 (과제 요구사항에 따라 제외)
- 지원 통화: `USD`, `JPY`, `CNY`, `EUR` (KRW 기준 주문만 가능)
- 분산 환경에서의 스케줄러 중복 실행 방지 미적용 (단일 인스턴스 기준)
