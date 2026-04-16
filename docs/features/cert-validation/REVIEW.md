---
agent: reviewer
feature: cert-validation
status: approved
created: 2026-04-16
updated: 2026-04-16
links:
  prd: docs/features/cert-validation/PRD.md
  api: docs/features/cert-validation/API-SPEC.md
  decisions: docs/features/cert-validation/DECISIONS.md
---

# REVIEW: cert-validation

## 총평

전반적으로 헥사고날 아키텍처 경계가 올바르게 지켜졌고, 코루틴 브리지 패턴과 상태 전이 로직도 명확하다.
주요 개선 사항은 MEDIUM 2건, LOW 3건으로, HIGH 이슈는 없다.

---

## 1. 헥사고날 경계 준수 여부

### 통과 항목
- Domain (`Certificate`, `CertificateStatus`, `ValidationResult`) 이 외부 의존성 없이 순수 Kotlin data class로 구성됨.
- Port (`CertificateRepository`, `CertificateValidationPort`) 는 `application/port/out` 패키지에 인터페이스로 정의되고, Adapter 구현체가 이를 주입받는 방향이 올바름.
- `CertificateHandler` 가 UseCase 인터페이스만 참조하며, Service 구현체를 직접 참조하지 않음.
- `CertificateValidationPort.parse()` / `validate()` 가 동기 인터페이스로 선언되어 Application 레이어는 X.509 JCA 세부를 몰라도 됨.

### 지적 사항

**[MEDIUM-01] `CertificateValidationPort` 가 비어있지 않은 두 메서드 역할을 혼합**

`parse()` 와 `validate()` 는 책임이 다르다. `parse()` 는 PEM 파싱(Adapter 입력 변환)이고, `validate()` 는 도메인 검증 로직이다. 두 메서드를 하나의 Port로 묶으면 "파싱 전용 Adapter"와 "검증 전용 Adapter" 를 독립적으로 교체할 수 없게 된다.

```
현재: CertificateValidationPort { parse(), validate() }
권장: CertificateParsePort { parse() }  +  CertificateValidationPort { validate() }
```

현재 구현에서는 `X509CertificateValidator` 가 두 역할을 모두 수행하므로 Phase 1 범위에서는 허용 가능하다. Phase 2에서 CRL/OCSP를 별도 Adapter로 추가할 때 분리 리팩토링을 권장한다.

---

## 2. CertificateService 상태 전이 로직

```kotlin
// CertificateService.kt

private fun decideInitialStatus(certificate: Certificate, now: Instant): CertificateStatus = when {
    certificate.isExpired(now) -> CertificateStatus.EXPIRED
    certificate.isNotYetValid(now) -> CertificateStatus.PENDING
    else -> CertificateStatus.VALID
}

private fun statusFromValidation(
    current: CertificateStatus,
    result: ValidationResult,
    now: Instant,
    certificate: Certificate,
): CertificateStatus {
    if (current == CertificateStatus.REVOKED) return CertificateStatus.REVOKED
    if (certificate.isExpired(now)) return CertificateStatus.EXPIRED
    return if (result.valid) CertificateStatus.VALID else current
}
```

### 통과 항목
- REVOKED 상태는 어떤 조건에서도 덮어쓰지 않음 (단방향 전이 보장).
- 만료 시 EXPIRED 우선 처리.
- VALID → 검증 실패 시 현재 상태를 유지 (`else current`) 하여 무분별한 상태 저하를 방지.

### 지적 사항

**[MEDIUM-02] `statusFromValidation()` 에서 체인 검증 실패 시 VALID가 유지될 수 있음**

`result.valid == false` 이고 `current == CertificateStatus.VALID` 인 경우 `else current` 에 의해 VALID가 반환된다. 즉, 이미 VALID였던 인증서가 체인 검증에 실패해도 상태가 VALID로 유지된다. 의도적인 결정이라면 ADR에 명시가 필요하다.

```kotlin
// 현재 동작: result.valid = false, current = VALID → 반환: VALID
// 의문: 이미 VALID인 인증서가 재검증에서 실패하면 PENDING 또는 별도 INVALID 상태로 전이해야 하지 않는가?
```

PRD 또는 ADR에 "재검증 실패 시 VALID 유지" 정책이 명시되어 있지 않으므로, 정책 문서화 또는 코드 내 WHY 주석 추가 권장.

---

## 3. Fingerprint Race Condition 처리

```kotlin
// CertificateService.register()
return transactionalPort.execute {
    repository.findByFingerprint(parsed.fingerprint)?.let {
        throw CertificateAlreadyRegisteredException(parsed.fingerprint)
    }
    repository.save(toPersist)
}
```

### 통과 항목
- `findByFingerprint()` + `save()` 가 동일 트랜잭션 내에서 실행됨.
- DB UNIQUE 제약이 최종 방어선으로 작동함 (DECISIONS.md ADR-002 문서화됨).

### 지적 사항

**[LOW-01] SQLIntegrityConstraintViolationException → 409 매핑 누락**

ADR-002에서 인식된 이슈이나 코드상 처리가 없다. GlobalExceptionHandler에 `DataIntegrityViolationException` (Spring wraps) → 409 매핑이 없다면 500으로 응답된다. Phase 1 허용 범위이나 후속 티켓 생성 권장.

---

## 4. Coroutine ↔ Blocking jOOQ 브리지 적절성

```kotlin
// CertificateService.validate()
val certificate = transactionalPort.executeReadOnly { repository.findById(id) }
    ?: throw CertificateNotFoundException(id)

val result = validationPort.validate(certificate)  // blocking call, NOT wrapped

if (newStatus != certificate.status) {
    transactionalPort.execute { repository.updateStatus(id, newStatus) }
}
```

### 통과 항목
- jOOQ Repository 호출은 모두 `transactionalPort.execute { }` / `executeReadOnly { }` 로 감싸여 virtual-thread dispatcher에서 실행됨 (ADR-001).
- `CertificateService` 자체는 `@Service` 만 달고 dispatcher 지정 없이 coRouter에 위임함으로써 Spring WebFlux 이벤트 루프를 블로킹하지 않음.

### 지적 사항

**[LOW-02] `validationPort.validate()` 가 트랜잭션 컨텍스트 밖에서 호출됨**

`X509CertificateValidator.validate()` 는 CPU-bounded blocking 작업(PEM 파싱, SHA-256, PKIX 체인 검증)을 수행한다. 현재 구현에서는 `transactionalPort` 밖에서 직접 호출되므로, coRouter 디스패처(기본: `Schedulers.boundedElastic`) 에서 실행된다. `X509CertificateValidator.trustAnchors` 의 `lazy` 초기화는 thread-safe하지만, CertPath 검증 내부의 CRL 구현이 블로킹 I/O를 수행할 경우 이벤트 루프 블로킹 위험이 있다. `isRevocationEnabled = false` 이므로 현재는 안전하나, Phase 2에서 OCSP 활성화 시 `withContext(Dispatchers.IO)` 감싸기가 필요함.

---

## 5. 코드 품질 및 스타일

### 통과 항목
- `val` 우선 사용, 매직 넘버 상수화 (`SERIAL_RADIX`, `SAN_VALUE_INDEX`, `LIST_DELIMITER`, `MILLIS_PER_SECOND`).
- Early return 패턴 준수 (`statusFromValidation` 상단 가드).
- 함수 길이 모두 30줄 이하.
- WHY 주석이 적절히 배치됨.
- `@Suppress("UNUSED_PARAMETER")` 에 사유 명시.

### 지적 사항

**[LOW-03] `CertificatePersistenceAdapter.toLocalDateTime()` 밀리초 → LocalDateTime 변환 정밀도 손실**

```kotlin
private fun toLocalDateTime(epochMillis: Long): LocalDateTime =
    LocalDateTime.ofEpochSecond(epochMillis / MILLIS_PER_SECOND, 0, ZoneOffset.UTC)
```

나노초(두 번째 인자) 를 `0` 으로 고정하여 밀리초 이하 정밀도가 소실된다. `createdAt`, `updatedAt` 은 `Instant.now()` 에서 생성되며 나노초 정밀도를 가지므로 DB round-trip 후 값이 달라질 수 있다. `Instant` → `LocalDateTime` 변환 시 `LocalDateTime.ofInstant(instant, ZoneOffset.UTC)` 사용을 권장한다.

---

## 6. 테스트 커버리지

| 대상 | 커버리지 | 비고 |
|---|---|---|
| `Certificate` 도메인 | 충분 | `isExpired`, `isNotYetValid`, `daysUntilExpiry`, `withStatus` 모두 테스트 |
| `CertificateService` | 충분 | 등록/조회/검증/삭제 핵심 경로 + 예외 경로 포함 |
| `X509CertificateValidator` | 부분 | 오류 경로 3건만; 정상 경로(유효 인증서 파싱)는 환경 의존으로 제외됨 (테스트 주석으로 사유 명시됨) |
| `CertificatePersistenceAdapter` | 없음 | Integration test (Testcontainers) 필요. Phase 2 권장 |
| `CertificateHandler` | 없음 | WebFlux `WebTestClient` 슬라이스 테스트 권장 |

**[LOW-04] `CertificateServiceTest` 에서 `assertThrows` + `runBlocking` 혼용**

```kotlin
assertThrows(CertificateAlreadyRegisteredException::class.java) {
    kotlinx.coroutines.runBlocking {
        service.register(...)
    }
}
```

`runTest` 와 `assertThrows` 를 혼용하면 코루틴 예외 전파가 불안정할 수 있다. `assertThrows<CertificateAlreadyRegisteredException> { ... }` 를 `runTest` 내부에서 `kotlin.test.assertFailsWith<T>()` 로 대체하는 것이 권장된다.

---

## 종합 판정

| 심각도 | 건수 | 항목 |
|---|---|---|
| HIGH | 0 | — |
| MEDIUM | 2 | MEDIUM-01 (Port 분리), MEDIUM-02 (재검증 실패 정책) |
| LOW | 4 | LOW-01 (DB 예외 매핑), LOW-02 (validate 블로킹), LOW-03 (정밀도 손실), LOW-04 (테스트 혼용) |

HIGH 이슈 없음 → **Backend Agent 반려 없이 통과**. MEDIUM/LOW 항목은 후속 Sprint에서 처리 권장.

## Quality Gate
- [x] 헥사고날 경계 준수 여부 검토
- [x] CertificateService 상태 전이 로직 검토
- [x] fingerprint race-condition 처리 검토
- [x] coroutine ↔ blocking jOOQ 브리지 적절성 검토
- [x] 코드 품질·스타일 전반 검토
- [x] status: approved
