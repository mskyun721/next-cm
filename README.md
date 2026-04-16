# Spring Boot Kotlin Skeleton

Spring Boot 기반 Kotlin 프로젝트를 빠르게 시작하기 위한 **Boilerplate** 프로젝트입니다.
**Hexagonal Architecture + DDD** 패턴을 적용하였으며, **WebFlux + Coroutine** 기반의 비동기 REST API 구현, 체계적인 에러 처리, i18n 다국어 지원, jOOQ를
통한 DB 접근 등 실무에서 필요한 공통 기능들이 사전 구성되어 있습니다.

---

## 기술 스택

| 구분              | 버전 / 기술                                  |
|-----------------|------------------------------------------|
| **Java**        | 25                                       |
| **Kotlin**      | 2.3.20                                   |
| **Spring Boot** | 4.0.5                                    |
| **Gradle**      | 9.4.1 (Kotlin DSL)                       |
| **설정 형식**       | YAML                                     |
| **DB Access**   | jOOQ 3.19.31                             |
| **Reactive**    | WebFlux + Kotlin Coroutine               |
| **테스트**         | JUnit 5, MockK 1.14.9                    |
| **정적 분석**       | detekt 2.0.0-alpha.2 (ktlint wrapper 포함) |
| **개발 DB**       | H2 (PostgreSQL 호환 모드)                    |

---

## 프로젝트 구조

### 전체 디렉토리 트리

```
springboot-kotlin-skeleton/
├── buildSrc/                           # Gradle 빌드 버전 중앙 관리
│   └── src/main/kotlin/
│       ├── BuildVersions.kt            # Java 버전 정의
│       ├── PluginVersions.kt           # 플러그인 버전 정의 (Spring Boot, Kotlin, detekt 등)
│       └── DependencyVersions.kt       # 라이브러리 버전 정의 (MockK 등)
├── config/
│   └── detekt/
│       └── detekt.yml                  # detekt 정적 분석 설정
├── src/
│   ├── main/
│   │   ├── kotlin/com/example/skeleton/
│   │   │   ├── KotlinBaseApplication.kt           # 애플리케이션 진입점
│   │   │   ├── adapter/                           # Adapter 계층 (외부 인터페이스)
│   │   │   │   ├── input/web/                     # 인바운드 어댑터 (REST API)
│   │   │   │   │   └── sample/
│   │   │   │   │       ├── SampleRouter.kt        # coRouter 기반 라우팅 정의
│   │   │   │   │       ├── SampleHandler.kt       # 요청 핸들러 (Controller 역할)
│   │   │   │   │       └── protocol/              # Request/Response DTO
│   │   │   │   │           ├── CreateSampleRequest.kt
│   │   │   │   │           ├── UpdateSampleRequest.kt
│   │   │   │   │           ├── SampleSearchRequest.kt
│   │   │   │   │           └── SampleResponse.kt
│   │   │   │   └── output/                        # 아웃바운드 어댑터
│   │   │   │       ├── persistence/               # DB 영속성 어댑터
│   │   │   │       │   ├── config/
│   │   │   │       │   │   └── DatabasePersistenceConfiguration.kt  # Read/Write 분리 설정
│   │   │   │       │   └── jooq/sample/
│   │   │   │       │       └── SamplePersistenceAdapter.kt          # jOOQ 기반 구현체
│   │   │   │       └── transaction/
│   │   │   │           └── TransactionalExecutorAdapter.kt          # 트랜잭션 실행기
│   │   │   ├── application/                       # Application 계층 (비즈니스 로직)
│   │   │   │   ├── port/
│   │   │   │   │   ├── input/sample/              # 인바운드 포트
│   │   │   │   │   │   ├── SampleUseCase.kt       # UseCase 인터페이스
│   │   │   │   │   │   └── model/                 # Command, Query 모델
│   │   │   │   │   │       ├── CreateSampleCommand.kt
│   │   │   │   │   │       ├── UpdateSampleCommand.kt
│   │   │   │   │   │       └── SampleSearchQuery.kt
│   │   │   │   │   └── output/                    # 아웃바운드 포트
│   │   │   │   │       ├── sample/
│   │   │   │   │       │   └── SamplePort.kt      # 영속성 포트 인터페이스
│   │   │   │   │       └── transaction/
│   │   │   │   │           └── TransactionalPort.kt # 트랜잭션 포트 인터페이스
│   │   │   │   └── service/
│   │   │   │       └── SampleService.kt           # UseCase 구현체
│   │   │   ├── domain/                            # Domain 계층 (핵심 도메인 모델)
│   │   │   │   └── sample/model/
│   │   │   │       ├── Sample.kt                  # 도메인 엔티티
│   │   │   │       └── SampleStatus.kt            # 도메인 Enum (GenericEnum + DisplayEnum)
│   │   │   └── common/                            # 공통 모듈
│   │   │       ├── CommonObjectMapper.kt          # Jackson ObjectMapper 설정
│   │   │       ├── config/                        # 공통 설정
│   │   │       │   ├── CommonConfiguration.kt     # ObjectMapper Bean 등록
│   │   │       │   ├── WebFluxConfiguration.kt    # WebFlux 설정 (Validator, API 버저닝 등)
│   │   │       │   ├── GlobalExceptionHandler.kt  # 전역 예외 핸들러
│   │   │       │   ├── GlobalErrorAttributes.kt   # 에러 응답 변환 로직
│   │   │       │   ├── TraceIdWebFilter.kt        # Trace ID 전파 필터
│   │   │       │   ├── TraceLoggingConfiguration.kt # Trace 로깅 설정
│   │   │       │   └── WebClientConfiguration.kt  # WebClient 설정
│   │   │       ├── constant/
│   │   │       │   └── CommonConstant.kt          # 프로젝트 전역 상수
│   │   │       ├── enums/
│   │   │       │   ├── EnumInterfaces.kt          # GenericEnum, DisplayEnum 인터페이스
│   │   │       │   └── DatePatternEnum.kt         # 날짜 포맷 패턴 Enum
│   │   │       ├── errors/                        # 에러 코드 체계
│   │   │       │   ├── ErrorCode.kt               # ErrorCode 인터페이스
│   │   │       │   ├── CommonErrorCode.kt         # 공통 에러 코드 Enum
│   │   │       │   ├── SampleErrorCode.kt         # 샘플 도메인 에러 코드 Enum
│   │   │       │   ├── ApiErrorResponse.kt        # 에러 응답/필드에러 DTO, ErrorSource Enum
│   │   │       │   └── ErrorConstants.kt          # 에러 관련 상수
│   │   │       ├── exception/                     # 예외 클래스
│   │   │       │   ├── DefaultException.kt        # 모든 커스텀 예외의 기반 클래스
│   │   │       │   ├── CommonExceptions.kt        # 공통 예외 (인증, 권한, Enum 변환 등)
│   │   │       │   ├── RequestValidationException.kt      # 요청 유효성 검증 예외 (기반)
│   │   │       │   ├── RequestValidationExceptions.kt     # 세부 유효성 검증 예외
│   │   │       │   └── SampleNotFoundException.kt         # 샘플 미존재 예외
│   │   │       ├── extensions/                    # Kotlin Extension Function
│   │   │       │   ├── EnumExtensions.kt          # Enum 변환 유틸 (byValue, requireByValue 등)
│   │   │       │   ├── ServerRequestExtensions.kt # ServerRequest 확장 (쿼리 바인딩, 헤더 등)
│   │   │       │   ├── ValidatorExtensions.kt     # Validator 확장 (validateOrThrow 등)
│   │   │       │   ├── JacksonExtensions.kt       # JSON 직렬화/역직렬화 확장
│   │   │       │   ├── CoroutineExtension.kt      # 다중 비동기 실행 유틸 (asyncAndAwait)
│   │   │       │   ├── CollectionExtensions.kt    # 컬렉션 확장 (unzipBy 등)
│   │   │       │   ├── LocalDateTimeExtensions.kt # 날짜/시간 확장
│   │   │       │   └── Tuples.kt                  # Tuple4 ~ Tuple8 데이터 클래스
│   │   │       └── utils/
│   │   │           └── MessageConverter.kt        # i18n 메시지 변환 유틸
│   │   └── resources/
│   │       ├── application.yml                    # 애플리케이션 설정
│   │       ├── db/
│   │       │   └── schema.sql                     # DDL 스키마 (jOOQ 코드 생성 + H2 초기화)
│   │       ├── errors/
│   │       │   └── error.properties               # 에러 메시지 (i18n)
│   │       ├── enums/
│   │       │   └── enum.properties                # Enum 다국어 라벨 (i18n)
│   │       ├── messages/
│   │       │   └── message.properties             # 일반 메시지 (i18n)
│   │       └── validations/
│   │           └── validation.properties          # 유효성 검증 메시지 (i18n)
│   └── test/
│       ├── kotlin/com/example/skeleton/
│       │   ├── KotlinBaseApplicationTests.kt                  # 컨텍스트 로드 테스트
│       │   ├── ApplicationHttpIntegrationTests.kt             # REST API 통합 테스트
│       │   ├── MessageI18nIntegrationTests.kt                 # i18n 메시지 테스트
│       │   ├── ReadWriteDatabaseRoutingIntegrationTests.kt    # Read/Write DB 라우팅 테스트
│       │   └── TransactionalExecutorIntegrationTests.kt       # Virtual Thread 트랜잭션 테스트
│       └── resources/
│           └── application-test.yml               # 테스트 전용 설정
├── build.gradle.kts                               # 빌드 설정
├── settings.gradle.kts                            # 프로젝트 설정
├── .editorconfig                                  # 코드 스타일 설정 (ktlint 연동)
└── gradlew / gradlew.bat                          # Gradle Wrapper
```

---

## 아키텍처

### Hexagonal Architecture + DDD

```
┌──────────────────────────────────────────────────────────────────┐
│                        adapter (외부 인터페이스)                    │
│  ┌─────────────────────┐          ┌────────────────────────────┐ │
│  │   input/web         │          │   output/persistence       │ │
│  │  (Router + Handler) │          │  (jOOQ Persistence Adapter)│ │
│  └────────┬────────────┘          └────────────┬───────────────┘ │
│           │                                    │                 │
│           │    ┌───────────────────────┐        │                 │
│           │    │   application         │        │                 │
│           ├───▶│  ┌────────────────┐   │◀───────┤                 │
│           │    │  │  port/input    │   │        │                 │
│           │    │  │  (UseCase)     │   │        │                 │
│           │    │  └───────┬────────┘   │        │                 │
│           │    │          ▼            │        │                 │
│           │    │  ┌────────────────┐   │        │                 │
│           │    │  │  Service       │   │        │                 │
│           │    │  └───────┬────────┘   │        │                 │
│           │    │          ▼            │        │                 │
│           │    │  ┌────────────────┐   │        │                 │
│           │    │  │  port/output   │───┼────────┘                 │
│           │    │  │  (Port)        │   │                          │
│           │    │  └────────────────┘   │                          │
│           │    └───────────────────────┘                          │
│           │                                                      │
│           │    ┌───────────────────────┐                          │
│           │    │   domain              │                          │
│           └───▶│  (Entity, Enum)       │                          │
│                └───────────────────────┘                          │
└──────────────────────────────────────────────────────────────────┘
```

#### 계층 설명

| 계층              | 역할                                                | 주요 파일                                                                |
|-----------------|---------------------------------------------------|----------------------------------------------------------------------|
| **domain**      | 핵심 비즈니스 모델. 외부 의존성 없이 순수 Kotlin으로 구성              | `Sample.kt`, `SampleStatus.kt`                                       |
| **application** | 유스케이스 정의 및 비즈니스 로직 구현. Port 인터페이스를 통해 외부와 통신      | `SampleUseCase.kt`, `SampleService.kt`, `SamplePort.kt`              |
| **adapter**     | 외부 시스템과의 연결을 담당. Input(웹 요청) 과 Output(DB 접근)으로 구분 | `SampleRouter.kt`, `SampleHandler.kt`, `SamplePersistenceAdapter.kt` |
| **common**      | 프로젝트 전반에서 사용하는 공통 코드 (설정, 에러, Enum, 확장 함수 등)      | 하위 대부분의 파일                                                           |

---

## 주요 기능 상세

### 1. Gradle 버전 중앙 관리 (`buildSrc`)

모든 플러그인 및 라이브러리 버전을 `buildSrc` 하위의 Object로 중앙 관리합니다.

```kotlin
// buildSrc/src/main/kotlin/PluginVersions.kt
object PluginVersions {
    const val KOTLIN = "2.3.20"
    const val SPRING_BOOT = "4.0.5"
    const val DETEKT = "2.0.0-alpha.2"
    const val JOOQ = "3.19.31"
    // ...
}

// build.gradle.kts 에서 사용
plugins {
    kotlin("jvm") version PluginVersions.KOTLIN
    id("org.springframework.boot") version PluginVersions.SPRING_BOOT
}
```

- `BuildVersions.kt` — Java 버전 (25)
- `PluginVersions.kt` — Gradle 플러그인 버전
- `DependencyVersions.kt` — 3rd-party 라이브러리 버전

### 2. WebFlux + Coroutine 기반 REST API

Reactor 대신 **Kotlin Coroutine**을 우선 적용하여, `coRouter`와 `suspend` 함수 기반의 비동기 REST API를 구현합니다.

```kotlin
// SampleRouter.kt - coRouter 사용
@Bean
fun sampleRoutes(): RouterFunction<ServerResponse> = coRouter {
    (accept(MediaType.APPLICATION_JSON) and version(API_VERSION_V1) and "/sample/{version}").nest {
        GET("samples", handler::searchSamples)
        GET("samples/{id}", handler::getSample)
        POST("samples", handler::createSample)
        PUT("samples/{id}", handler::updateSample)
        DELETE("samples/{id}", handler::deleteSample)
    }
}

// SampleHandler.kt - suspend 함수로 핸들러 구현
suspend fun getSample(request: ServerRequest): ServerResponse {
    val id = request.pathVariable("id").toLongOrNull()
        ?: throw InvalidPathParameterException("id")
    val result = sampleUseCase.getSample(id)
    return ServerResponse.ok().bodyValueAndAwait(SampleResponse.from(result))
}
```

#### API 버저닝

Spring Boot 4의 `ApiVersionConfigurer`를 활용하여 URL 경로 세그먼트 기반 API 버저닝을 적용합니다.

```kotlin
override fun configureApiVersioning(configurer: ApiVersionConfigurer) {
    configurer.usePathSegment(1).setVersionRequired(true)
}
```

- 요청 예시: `GET /sample/1.0/samples`

### 3. Read/Write 분리 데이터소스

`LazyConnectionDataSourceProxy`를 사용하여 **Read/Write 데이터소스를 분리**합니다.

```kotlin
@Bean("dataSource")
@Primary
fun dataSource(writeDataSource: DataSource, readDataSource: DataSource): DataSource =
    LazyConnectionDataSourceProxy(writeDataSource).apply {
        setReadOnlyDataSource(readDataSource)
        setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)
    }
```

- `writeTransactionTemplate` — 쓰기 트랜잭션 (write DataSource 사용)
- `readTransactionTemplate` — 읽기 트랜잭션 (read DataSource 사용)
- **Virtual Thread** 기반의 `CoroutineDispatcher`로 블로킹 JDBC 호출을 비동기 처리

```kotlin
@Bean("databaseCoroutineDispatcher", destroyMethod = "close")
fun databaseCoroutineDispatcher(): ExecutorCoroutineDispatcher =
    Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
```

### 4. jOOQ를 이용한 DB 접근

- DDL 파일(`src/main/resources/db/schema.sql`)로부터 jOOQ 코드를 자동 생성 (KotlinGenerator)
- 생성된 코드는 `build/generated-src/jooq/main`에 위치
- H2를 PostgreSQL 호환 모드로 사용하여 개발 환경에서 테스트 가능

```kotlin
// SamplePersistenceAdapter.kt - jOOQ 사용 예시
override fun findAll(): List<Sample> = dslContext
    .selectFrom(SAMPLES)
    .orderBy(SAMPLES.ID.asc())
    .fetch(::toSample)
```

### 5. 에러 처리 체계

체계적인 에러 처리 파이프라인을 구현하여 일관된 에러 응답을 제공합니다.

#### 에러 처리 흐름

```
예외 발생 → GlobalExceptionHandler → GlobalErrorAttributes → JSON 에러 응답
```

#### 에러 응답 구조

```json
{
    "status": 404,
    "code": "ESMP001",
    "message": "샘플을 찾을 수 없습니다. ID: 42",
    "path": "/sample/1.0/samples/42",
    "traceId": "a1b2c3d4e5f6..."
}
```

유효성 검증 실패 시 `errors` 필드가 추가됩니다:

```json
{
    "status": 400,
    "code": "EKCP010",
    "message": "유효성 검사에 실패했습니다...",
    "path": "/sample/1.0/samples",
    "traceId": "...",
    "errors": [
        {
            "source": "BODY",
            "field": "name",
            "message": "must not be blank"
        },
        {
            "source": "BODY",
            "field": "age",
            "message": "must be greater than or equal to 0"
        }
    ]
}
```

#### 예외 클래스 계층

```
DefaultException (추상 기반 클래스)
├── CommonExceptions
│   ├── UnauthorizedException          (401)
│   ├── InvalidTokenException          (401)
│   ├── PermissionDeniedException      (403)
│   └── InvalidEnumValueException      (500, DB enum 변환 실패)
├── RequestValidationException         (요청 유효성 검증 기반)
│   └── SingleFieldRequestValidationException (단일 필드 검증)
│       ├── InvalidRequestFieldException
│       │   ├── RequiredHeaderException
│       │   ├── InvalidHeaderValueException
│       │   ├── RequiredQueryParameterException
│       │   ├── InvalidPathParameterException
│       │   │   └── InvalidEnumPathParameterException
│       │   └── QueryParameterBindingException
│       └── RequiredRequestBodyException
└── SampleNotFoundException            (404, 샘플 미존재)
```

#### ErrorCode 인터페이스

모든 에러 코드는 `ErrorCode` 인터페이스를 구현한 Enum으로 관리합니다:

```kotlin
interface ErrorCode {
    val code: String   // 에러 코드 (예: "EKCP001")
    val label: String  // 메시지 리소스 키 (i18n 지원)
}
```

| Enum              | 접두사    | 용도                                |
|-------------------|--------|-----------------------------------|
| `CommonErrorCode` | `EKCP` | 공통 에러 (400, 401, 403, 404, 500 등) |
| `SampleErrorCode` | `ESMP` | 샘플 도메인 에러                         |

> **새 도메인 추가 시**: `ErrorCode`를 구현하는 Enum을 생성하고, `errors/error.properties`에 메시지를 등록합니다.

#### 처리되는 예외 유형

`GlobalErrorAttributes`에서 다음 예외들을 자동으로 처리합니다:

| 예외                             | 설명                            |
|--------------------------------|-------------------------------|
| `DefaultException` 하위 클래스      | 커스텀 비즈니스 예외                   |
| `WebExchangeBindException`     | Spring Validation 실패 `@Valid` |
| `ConstraintViolationException` | Bean Validation 실패            |
| `ServerWebInputException`      | JSON 파싱, 타입 불일치 등             |
| `InvalidMediaTypeException`    | 지원하지 않는 Content-Type          |
| `DataBufferLimitException`     | 요청 데이터 크기 초과                  |
| `ResponseStatusException`      | Spring 기본 HTTP 상태 예외          |

### 6. Enum 처리

#### 두 가지 Enum 인터페이스

| 인터페이스             | 용도                             | 주요 속성                              |
|-------------------|--------------------------------|------------------------------------|
| **`GenericEnum`** | DB 저장용. `value` 필드가 DB에 저장되는 값 | `value: String`                    |
| **`DisplayEnum`** | API 응답용. 다국어 라벨과 정렬 우선순위 제공    | `label`, `priority`, `displayable` |

```kotlin
// SampleStatus — GenericEnum + DisplayEnum 동시 구현
enum class SampleStatus(
    override val value: String,      // DB 저장값 ("active", "inactive", ...)
    override val label: String,      // i18n 키 ("enum.SampleStatus.ACTIVE")
    override val priority: Int,      // 정렬 우선순위
    override val displayable: Boolean = true,
) : GenericEnum, DisplayEnum {
    ACTIVE("active", "enum.SampleStatus.ACTIVE", 1),
    INACTIVE("inactive", "enum.SampleStatus.INACTIVE", 2),
    SUSPENDED("suspended", "enum.SampleStatus.SUSPENDED", 3),
}
```

#### Enum 변환 유틸리티 (`EnumExtensions.kt`)

```kotlin
// DB value → Enum 변환 (nullable)
byValue<SampleStatus>("active")        // → SampleStatus.ACTIVE?

// DB value → Enum 변환 (실패 시 예외)
requireByValue<SampleStatus>("active") // → SampleStatus.ACTIVE

// Display label → Enum 변환
byLabel<SampleStatus>("enum.SampleStatus.ACTIVE") // → SampleStatus.ACTIVE?
```

#### Enum 바인딩 위치별 처리

| 위치                  | 처리 방식                                              | 실패 시 예외                                   |
|---------------------|----------------------------------------------------|-------------------------------------------|
| **Path Variable**   | `request.enumPathVariable<SampleStatus>("status")` | `InvalidEnumPathParameterException` (400) |
| **Query Parameter** | Spring WebDataBinder 자동 변환 (`enum name` 기반)        | `QueryParameterBindingException` (400)    |
| **Request Body**    | Jackson 자동 변환 (`enum name` 기반)                     | `ServerWebInputException` (400)           |
| **DB → Enum**       | `requireByValue<SampleStatus>(dbValue)`            | `InvalidEnumValueException` (500)         |

### 7. i18n 다국어 지원

Spring `MessageSource`를 활용한 다국어 지원 체계입니다.

#### 메시지 리소스 파일 구조

```yaml
# application.yml
spring:
    messages:
        basename: messages/message,validations/validation,enums/enum,errors/error
        fallback-to-system-locale: false
    web:
        locale: ko_KR
```

| 파일                                  | 용도          | 예시                                                      |
|-------------------------------------|-------------|---------------------------------------------------------|
| `errors/error.properties`           | 에러 메시지      | `common.CommonErrorCode.NOT_FOUND=존재하지 않는 API 입니다.`     |
| `enums/enum.properties`             | Enum 다국어 라벨 | `enum.SampleStatus.ACTIVE=활성`                           |
| `validations/validation.properties` | 유효성 검증 메시지  | `validation.constraints.Range={min} 이상 {max} 이하여야 합니다.` |
| `messages/message.properties`       | 일반 메시지      | (확장용)                                                   |

#### MessageConverter 유틸리티

```kotlin
// 코드 기반 메시지 조회
MessageConverter.getMessage("enum.SampleStatus.ACTIVE")              // → "활성"
MessageConverter.getMessage("error.label", arrayOf(42), Locale.KOREA) // → 파라미터 치환된 메시지

// ErrorCode에서 직접 메시지 조회
SampleErrorCode.SAMPLE_NOT_FOUND.getMessage(arrayOf(42L))            // → "샘플을 찾을 수 없습니다. ID: 42"
```

### 8. 요청 처리 확장 함수

Handler에서 반복되는 요청 처리 패턴을 Kotlin Extension Function으로 간결하게 제공합니다.

#### ServerRequest 확장

```kotlin
// Query Parameter → DTO 바인딩
val searchRequest = request.bindQueryParams<SampleSearchRequest>()

// Path Variable에서 Enum 바인딩
val status = request.enumPathVariable<SampleStatus>("status")

// 필수 헤더 추출 (없으면 RequiredHeaderException 발생)
val modifiedBy = request.headerOrThrow("X-Modified-By")

// Body → DTO + Validation 한번에 처리
val body = request.awaitBodyValidated<CreateSampleRequest>(validator)
```

#### Validator 확장

```kotlin
// 유효성 검증 실패 시 ConstraintViolationException 발생
validator.validateOrThrow(target)
```

### 9. 비동기 병렬 처리 유틸리티

`asyncAndAwait` 확장 함수로 최대 8개의 비동기 작업을 병렬로 실행하고 결과를 취합합니다:

```kotlin
// 2개 병렬 실행 → Pair<T1, T2>
val (users, orders) = asyncAndAwait(
    { userService.getUsers() },
    { orderService.getOrders() },
)

// 4개 병렬 실행 → Tuple4<T1, T2, T3, T4>
val (a, b, c, d) = asyncAndAwait(
    { serviceA.call() },
    { serviceB.call() },
    { serviceC.call() },
    { serviceD.call() },
)
```

> Kotlin 표준 라이브러리의 `Pair`와 `Triple` 외에 `Tuple4` ~ `Tuple8`을 직접 정의하여 사용합니다.

### 10. Trace ID 전파

모든 요청에 대해 `traceId`를 자동 생성하거나 전파합니다.

#### Trace ID 우선순위

1. `X-Trace-Id` 헤더
2. `traceparent` 헤더 (W3C Trace Context, 32자리 hex)
3. `X-B3-TraceId` 헤더 (Zipkin B3)
4. 자동 생성 (UUID)

#### 동작 방식

- 요청 시 `TraceIdWebFilter`에서 `traceId`를 추출/생성하여 Exchange Attribute에 저장
- 응답 헤더에 `X-Trace-Id`로 반환
- Micrometer Context Propagation을 통해 Reactor/Coroutine 컨텍스트에 MDC 전파
- 로그 패턴: `%5p [traceId:%X{traceId:-}]`

### 11. 정적 분석 (detekt + ktlint)

detekt를 정적 분석 도구로 사용하며, ktlint 규칙을 detekt wrapper plugin으로 통합 적용합니다.

#### 설정 구조

| 파일                         | 역할                                                              |
|----------------------------|-----------------------------------------------------------------|
| `config/detekt/detekt.yml` | detekt 고유 규칙 설정 (복잡도, 네이밍, 스타일 등)                               |
| `.editorconfig`            | ktlint 포맷팅 규칙 + IntelliJ 설정 (들여쓰기, import 정렬, trailing comma 등) |

#### 주요 설정

- **코드 스타일**: `intellij_idea` (IntelliJ 포매터와 호환)
- **최대 줄 길이**: 120자
- **Trailing comma**: 선언부(declaration site)에서만 권장
- **Wildcard import**: IntelliJ 기본값(동일 패키지 5개 이상 시 사용) 유지
- detekt 보고서: Markdown 형식으로 출력

```bash
# detekt 실행
./gradlew detekt
```

---

## 빠른 시작

### 사전 요구 사항

- **JDK 25** 이상
- **Gradle** 9.4.1 (Wrapper 포함)

### 빌드 및 실행

```bash
# 1. jOOQ 코드 생성 + 빌드
./gradlew build

# 2. 애플리케이션 실행
./gradlew bootRun
```

애플리케이션은 `http://localhost:18080` 에서 실행됩니다.

### API 테스트 예시

```bash
# 샘플 생성
curl -X POST http://localhost:18080/sample/1.0/samples \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","age":30,"status":"ACTIVE"}'

# 전체 조회
curl http://localhost:18080/sample/1.0/samples

# 조건 검색
curl "http://localhost:18080/sample/1.0/samples?minAge=25&maxAge=35&status=ACTIVE"

# 상태별 검색 (Path Variable Enum)
curl http://localhost:18080/sample/1.0/samples/status/ACTIVE

# 단건 조회
curl http://localhost:18080/sample/1.0/samples/1

# 수정
curl -X PUT http://localhost:18080/sample/1.0/samples/1 \
  -H "Content-Type: application/json" \
  -H "X-Modified-By: tester" \
  -d '{"name":"Updated","age":31,"status":"INACTIVE"}'

# 삭제
curl -X DELETE http://localhost:18080/sample/1.0/samples/1
```

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 정적 분석
./gradlew detekt
```

---

## 테스트

| 테스트 파일                                     | 설명                                     |
|--------------------------------------------|----------------------------------------|
| `KotlinBaseApplicationTests`               | 스프링 컨텍스트 로드 확인                         |
| `ApplicationHttpIntegrationTests`          | REST API CRUD 및 에러 응답 통합 테스트 (12개 케이스) |
| `MessageI18nIntegrationTests`              | i18n 메시지 변환 테스트                        |
| `ReadWriteDatabaseRoutingIntegrationTests` | Read/Write DB 라우팅 검증                   |
| `TransactionalExecutorIntegrationTests`    | Virtual Thread 기반 트랜잭션 실행 검증           |

---

## 새 도메인 추가 가이드

`sample` 도메인을 참고하여 새 도메인을 추가하는 순서:

### 1. Domain 계층

```
domain/{도메인}/model/
├── {Entity}.kt          # 도메인 엔티티
└── {Status}Enum.kt      # GenericEnum + DisplayEnum 구현
```

### 2. Application 계층

```
application/port/input/{도메인}/
├── {도메인}UseCase.kt          # UseCase 인터페이스
└── model/
    ├── Create{Entity}Command.kt
    ├── Update{Entity}Command.kt
    └── {Entity}SearchQuery.kt

application/port/output/{도메인}/
└── {도메인}Port.kt              # 영속성 포트 인터페이스

application/service/
└── {도메인}Service.kt           # UseCase 구현체
```

### 3. Adapter 계층

```
adapter/input/web/{도메인}/
├── {도메인}Router.kt            # coRouter 라우팅
├── {도메인}Handler.kt           # 핸들러
└── protocol/
    ├── Create{Entity}Request.kt
    ├── Update{Entity}Request.kt
    └── {Entity}Response.kt

adapter/output/persistence/jooq/{도메인}/
└── {도메인}PersistenceAdapter.kt # jOOQ 구현체
```

### 4. 에러 코드 및 메시지

```kotlin
// common/errors/{도메인}ErrorCode.kt
enum class { 도메인 }ErrorCode(override val code: String, override val label: String) : ErrorCode {
    { ENTITY } _NOT_FOUND ("E{PREFIX}001", "{domain}.{도메인}ErrorCode.{ENTITY}_NOT_FOUND"),
}
```

```properties
# resources/errors/error.properties
{domain}.{도메인}ErrorCode.{ENTITY}_NOT_FOUND={엔티티}를 찾을 수 없습니다. ID: {0}
```

### 5. DB 스키마

`src/main/resources/db/schema.sql`에 테이블 DDL 추가 후 `./gradlew jooqCodegen`으로 코드 재생성.

---

## 공통 패키지 (`common`) 요약

| 패키지                 | 내용                                                                                             |
|---------------------|------------------------------------------------------------------------------------------------|
| `common.config`     | WebFlux, 에러 핸들링, ObjectMapper, Trace, WebClient 설정                                             |
| `common.constant`   | 프로젝트 전역 상수 (`MAX_BUFFER_SIZE`, `API_VERSION_V1`, `DEFAULT_LOCALE`)                             |
| `common.enums`      | Enum 인터페이스 (`GenericEnum`, `DisplayEnum`), 날짜 패턴 Enum                                          |
| `common.errors`     | `ErrorCode` 인터페이스, 에러 코드 Enum, 에러 응답 DTO, 상수                                                   |
| `common.exception`  | 예외 클래스 계층 (`DefaultException` 기반)                                                              |
| `common.extensions` | Kotlin 확장 함수 (Enum, ServerRequest, Validator, Jackson, Coroutine, Collection, DateTime, Tuple) |
| `common.utils`      | `MessageConverter` (i18n 메시지 처리)                                                               |

---

## 라이선스

이 프로젝트는 자유롭게 사용할 수 있는 Boilerplate 프로젝트입니다.
