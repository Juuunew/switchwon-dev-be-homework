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
> API 호출 실패 시 Frankfurter API → Mock Provider 순서로 fallback합니다.

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
│   ├── controller/     # GET /exchange-rate/latest
│   ├── dto/            # ExchangeRateResponse, ExchangeRateListResponse
│   ├── entity/         # ExchangeRateEntity
│   ├── provider/       # ExchangeRateProvider (인터페이스), ProviderRate
│   │                   # ExchangeRateApiProvider, FrankfurterExchangeRateProvider
│   │                   # MockExchangeRateProvider, ProviderConfig
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
        "dateTime": "2026-05-02T18:00:00"
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
    "dateTime": "2026-05-02T18:00:00"
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
    "dateTime": "2026-05-02T18:00:00"
  }
}
```

#### 주문 내역 조회

```
GET /order/list
```

---

## 주요 구현 사항

### 환율 수집

- 3개 Provider를 우선순위 순서로 시도: ExchangeRate-API → Frankfurter → Mock
- `application.yml`의 `exchange-rate.collection.providers` 설정으로 순서 제어
- 각 Provider는 `supports(from, to)` 메서드로 처리 가능 여부를 명시
- USD 기준 크로스 레이트 계산: `unitRate = KRW_per_USD / FOREIGN_per_USD`
- JPY: 100엔 단위 환산 적용
- 매 1분마다 스케줄러를 통해 환율 수집 및 DB 저장
- 환율 데이터에 수집 출처(`provider`), 통화 방향(`fromCurrency`, `toCurrency`) 저장

### Provider 구성

| Provider | 설명 | 우선순위 |
|----------|------|---------|
| ExchangeRateApiProvider | ExchangeRate-API v6 연동 | 1순위 |
| FrankfurterExchangeRateProvider | Frankfurter 공개 API 연동 | 2순위 |
| MockExchangeRateProvider | 고정 환율 반환 (API 장애 대비) | 3순위 |

### 환율 계산 규칙

| 항목 | 규칙 |
|------|------|
| 환율 소수점 | 둘째 자리까지 반올림 |
| JPY 단위 | 100엔 기준 환산 |
| KRW 환산 금액 | 소수점 이하 버림 (Floor) |
| 전신환 매입율 (buyRate) | 매매기준율 × 1.05 |
| 전신환 매도율 (sellRate) | 매매기준율 × 0.95 |

### 설계

- **레이어드 아키텍처**: Controller → Service → Repository
- **통화 타입 분리**: `CurrencyCode` (전체 통화) / `ForeignCurrency` (외화, rateUnit 포함)
- **RestClient**: Spring Boot 3.2+ RestClient 사용, 3초 연결 / 5초 읽기 타임아웃
- **BigDecimal**: 금융 연산 전체에 `BigDecimal` 사용, `double` 파싱 없음
- **응답 DTO**: 불변 데이터는 Java Record 적용
- **전역 예외 처리**: `BusinessException`, `MethodArgumentNotValidException`, `HttpMessageNotReadableException`

---

## 테스트

| 구분 | 방식 | 주요 시나리오 |
|------|------|--------------|
| Controller | `@WebMvcTest` | 매수/매도 주문, 유효성 실패, 주문 목록 조회 |
| OrderService | `@ExtendWith(MockitoExtension.class)` | buyRate/sellRate 적용, KRW 버림 처리, 동일 통화 예외 |
| ExchangeRateService | `@ExtendWith(MockitoExtension.class)` | 전체/단일 환율 조회, 환율 없을 시 예외 |
| ExchangeRateApiProvider | `@ExtendWith(MockitoExtension.class)` | USD/JPY 크로스레이트, supports(), API 오류 처리 |

---

## 제약 사항

- H2 In-Memory DB 사용으로 애플리케이션 재시작 시 데이터 초기화됨
- 인증/세션 미구현 (과제 요구사항에 따라 제외)
- 지원 통화: `USD`, `JPY`, `CNY`, `EUR` (KRW 기준 주문만 가능)
