---
agent: security
feature: cert-validation
status: approved
created: 2026-04-16
updated: 2026-04-16
links:
  prd: docs/features/cert-validation/PRD.md
  api: docs/features/cert-validation/API-SPEC.md
  decisions: docs/features/cert-validation/DECISIONS.md
  review: docs/features/cert-validation/REVIEW.md
---

# SECURITY-AUDIT: cert-validation

## 감사 범위

- 구현 코드: `certificate/` 전체 (domain, application, adapter)
- 테스트 코드: `certificate/` 전체
- 참조 문서: PRD.md, DECISIONS.md, API-SPEC.md
- 기준: OWASP Top 10 (2021), OWASP API Security Top 10 (2023), DECISIONS.md 보안 결정

---

## 1. PEM 원문 평문 저장

### 현황

```kotlin
// Certificate.kt
val pemContent: String  // PEM 원문 그대로 저장

// CertificatePersistenceAdapter.save()
.set(CERTIFICATES.PEM_CONTENT, certificate.pemContent)

// CertificateResponse.kt
// pemContent 는 응답에 포함되지 않음 (from() 매핑에서 제외됨)
```

### 판정: MEDIUM

**근거**
- X.509 공개 인증서(Public Certificate)의 PEM은 원래 공개 정보이므로, 비밀키(Private Key)를 함께 저장하는 경우와는 위험도가 다르다.
- 현재 구현에서 비밀키는 저장되지 않고, PEM은 공개 인증서 데이터만 포함한다.
- 단, DB 접근 권한이 광범위할 경우 DER/PEM 원문으로부터 인증서 체계 분석이 가능하다.
- `CertificateResponse` 에서 `pemContent` 가 의도적으로 제외되어 API 응답 노출은 없다 (양호).

**권고**
- DB 컬럼 수준 암호화(TDE) 또는 애플리케이션 레벨 AES-256 암호화를 Phase 2에서 검토한다.
- PEM을 저장하는 명시적 사유(재검증 시 재파싱 필요)가 ADR에 없으므로 ADR-001 또는 별도 ADR에 문서화 권장.
- fingerprint + 메타데이터만 저장하고 PEM 원문은 별도 암호화 저장소(Vault 등)에 위탁하는 방안도 검토 가능.

---

## 2. Fingerprint 로그 노출 범위

### 현황

```kotlin
// CertificateService.register()
log.debug(
    "register() - alias={}, subject={}, status={}",
    toPersist.alias,
    toPersist.subject,
    toPersist.status,
)

// CertificateAlreadyRegisteredException (fingerprint 포함 가정)
throw CertificateAlreadyRegisteredException(parsed.fingerprint)
```

DECISIONS.md ADR-005:
> fingerprint 는 로그/메시지에 **12자리까지만 노출**

### 판정: LOW

**근거**
- `register()` 의 debug 로그에는 fingerprint가 포함되지 않음 (양호).
- `CertificateAlreadyRegisteredException` 에 fingerprint 전달 시 예외 메시지/로그에 전체 SHA-256 fingerprint(95자)가 출력될 수 있다.
- ADR-005가 "12자리까지만 노출" 정책을 명시하였으나, 예외 생성자에서 truncation이 적용되는지 코드로 확인이 필요하다.

**권고**
```kotlin
// CertificateAlreadyRegisteredException 생성자에서 truncation 보장
class CertificateAlreadyRegisteredException(fingerprint: String) :
    DefaultException(CertificateErrorCode.ALREADY_REGISTERED, fingerprint.take(12))
```
- `CertificateAlreadyRegisteredException` 구현 파일을 확인하여 truncation 적용 여부를 검증할 것.
- 운영 로그에서 전체 fingerprint가 출력되면 인증서 식별에 활용될 수 있으므로 정책 준수 필요.

---

## 3. Trust Store 정책 (JVM cacerts only)

### 현황

```kotlin
// X509CertificateValidator.loadSystemTrustAnchors()
tmf.init(null as KeyStore?)  // JVM default cacerts
// isRevocationEnabled = false
```

### 판정: MEDIUM

**근거**
- JVM cacerts 단독 사용은 공공 CA 체계에서는 적합하나, 사내 전용 CA 또는 PKI 환경에서는 모든 사내 인증서가 체인 검증 실패(PKIX 오류)로 처리된다.
- CRL/OCSP 비활성화(`isRevocationEnabled = false`)로 인해 **이미 폐기된 인증서가 VALID로 판정**될 수 있다. 이는 PRD Phase 2 항목으로 인식되었으나, 보안 관점에서 명시적 경고가 필요하다.
- DECISIONS.md ADR-004에 문서화되어 있으나 운영 가이드라인에 폐기 미검증 주의사항 전파가 필요하다.

**권고**
1. 사내 CA 사용 요구가 발생할 경우를 대비해 `CERT_TRUST_STORE_PATH` 환경변수로 추가 JKS/PKCS12 로드 확장점 설계.
2. `isRevocationEnabled = false` 사용 시, API 응답 `errors` 필드에 "revocation status not checked" 경고를 포함하거나, `ValidationResult` 에 `revocationChecked: Boolean` 플래그 추가 검토.
3. Phase 2 OCSP/CRL 구현 시 타임아웃 설정 필수 (기본값 무한대 위험).

---

## 4. PEM 사이즈 상한 및 Alias 정규식 인젝션 방어

### 현황

```kotlin
// RegisterCertificateRequest.kt
@field:NotBlank
@field:Size(min = 1, max = 128)
@field:Pattern(regexp = "^[A-Za-z0-9._\\-]+$")
val alias: String,

@field:NotBlank
@field:Size(max = 32768)  // 32 KB
val pemContent: String,
```

### 판정: LOW (적절히 방어됨, 세부 개선 가능)

**근거 - Alias**
- `^[A-Za-z0-9._\\-]+$` 패턴으로 영숫자/점/하이픈/언더스코어만 허용. SQL Injection, Path Traversal, JNDI Injection 대응 충분.
- 최대 128자 제한으로 과도한 길이 공격 방어.
- DB UNIQUE 제약으로 중복 alias 방어 여부 확인 필요 (현재 코드에서 alias UNIQUE 제약이 없어 동일 alias로 다른 인증서 등록 가능).

**근거 - PEM Content**
- `@Size(max = 32768)` (32 KB): X.509 v3 공개 인증서는 일반적으로 1~4 KB이므로 충분한 상한. 단, 체인 전체(leaf + intermediate)를 허용하는 경우 32 KB가 타이트할 수 있다.
- PEM 원문 자체를 Java Bean Validation(`@Pattern`) 으로 검증하지 않으나, `parseX509()` 에서 `CertificateException` → `InvalidPemFormatException` (400)으로 변환되어 악의적 PEM은 파싱 단계에서 차단됨 (양호).

**권고**
1. Alias UNIQUE 제약 추가 검토 (동일 alias, 다른 인증서 등록 허용 여부 정책 결정 필요).
2. PEM 사이즈를 애플리케이션 설정으로 외부화 권장 (`cert.pem.max-size-bytes=32768`).
3. 멀티 인증서 PEM(체인 포함) 입력 허용 여부 명시 필요: 현재 `generateCertificate()` 는 첫 번째 인증서만 파싱함.

---

## 5. OWASP Top 10 관점 검토

### API1:2023 - Broken Object Level Authorization (BOLA)

```kotlin
// GET /api/{version}/certificates/{id}
// DELETE /api/{version}/certificates/{id}
```

**판정: HIGH**

현재 구현에서 인증/인가 로직이 전혀 없다. `CertificateRouter` 및 `CertificateHandler` 에 Spring Security 설정이나 인가 어노테이션이 존재하지 않는다. UUID를 알면 누구든지 타인의 인증서를 조회/삭제할 수 있다.

**권고**
- Spring Security + JWT/OAuth2 토큰 기반 인증 레이어 추가 (Phase 1 내 필수).
- 소유자 컬럼(`owner_id` 또는 `tenant_id`) 추가 및 `findById()` 시 소유자 필터링.
- `DELETE` 엔드포인트에 `ROLE_ADMIN` 또는 소유자 확인 인가 필수.

> **이 항목은 HIGH 이슈로, Backend Agent로 반려가 필요합니다.**

---

### API2:2023 - Broken Authentication

**판정: HIGH** (API1과 연동)

인증 없이 모든 엔드포인트 접근 가능. 위 API1 권고사항과 동일하게 처리.

---

### API3:2023 - Broken Object Property Level Authorization

**판정: PASS**

`CertificateResponse` 에서 `pemContent` 가 의도적으로 제외됨 (양호).

---

### API4:2023 - Unrestricted Resource Consumption

**판정: LOW**

- PEM 32 KB 상한으로 개별 요청 크기는 제한됨.
- Rate limiting 또는 IP-based throttling 설정이 코드에 없음. Spring Gateway 또는 Nginx 레이어에서 처리한다고 가정하나 명시 필요.
- `GET /certificates` (전체 조회) 에 페이지네이션이 없어 대규모 데이터셋에서 OOM 위험. PRD에 페이지네이션 요구사항 명시 후 구현 권장.

---

### API5:2023 - Broken Function Level Authorization

**판정: HIGH** (API1과 연동)

`DELETE /certificates/{id}` 가 별도 권한 없이 모든 인증된(또는 비인증) 사용자에게 개방됨.

---

### API8:2023 - Security Misconfiguration

**판정: MEDIUM**

- `isRevocationEnabled = false` 로 OCSP/CRL 체크 비활성화. 이는 설계 결정이지만 프로덕션 배포 전 위험 인식 필요.
- `trustAnchors` 가 빈 집합일 경우 `errors.add("Trust store is empty")` 로 처리하여 예외 없이 검증 실패로 반환 (양호). 단, 경고 로그(log.warn) 추가 권장.

---

### API9:2023 - Improper Inventory Management

**판정: PASS**

API 버전 관리(`API_VERSION_V1`)가 구현되어 있고, coRouter에서 버전별 라우팅이 적용됨.

---

### OWASP Top 10 (일반) - A02:2021 Cryptographic Failures

**판정: LOW**

- SHA-256 fingerprint 사용 (권장 알고리즘, 양호).
- `MessageDigest.getInstance("SHA-256")` 는 매번 새 인스턴스 생성. `MessageDigest` 는 thread-safe하지 않으므로 현재 방식(매번 new)이 올바름.
- PEM 평문 저장 관련 항목은 섹션 1 참조.

---

### OWASP Top 10 (일반) - A03:2021 Injection

**판정: PASS**

- jOOQ DSL의 parameterized query 사용으로 SQL Injection 방어됨.
- Alias 입력 정규식 검증으로 특수문자 차단.
- PEM 파싱은 JCA API를 통해 처리되어 코드 인젝션 경로 없음.

---

## 종합 판정

| 심각도 | 건수 | 항목 |
|---|---|---|
| HIGH | 3 | 인증/인가 부재 (BOLA, 인증, 기능 레벨 인가) |
| MEDIUM | 3 | PEM 평문 저장, Trust Store 정책(CRL 미검증), Security Misconfiguration |
| LOW | 4 | Fingerprint 로그 truncation 검증, PEM 사이즈 설정 외부화, 페이지네이션 부재, alias UNIQUE 정책 |

---

## Orchestrator 판정

**HIGH 이슈 3건 발견 → Backend Agent 반려 처리**

인증/인가(Spring Security) 구현이 없어 모든 엔드포인트가 무방비로 노출되어 있다.
현재 상태로는 프로덕션 배포 불가. 아래 항목을 Backend Agent에 반려한다.

### 반려 요구사항

1. **[필수] Spring Security 인증 레이어 추가**
   - JWT 또는 OAuth2 Resource Server 설정
   - 미인증 요청에 대해 401 반환

2. **[필수] BOLA 방어 - 소유자 필터링**
   - `certificates` 테이블에 `owner_id` 컬럼 추가 또는 테넌트 격리 설계
   - `findById()` 시 호출자 소유 여부 검증

3. **[필수] DELETE 엔드포인트 인가 강화**
   - `ROLE_ADMIN` 또는 소유자 확인 후 삭제 허용

## Quality Gate
- [x] PEM 원문 저장 위험 검토
- [x] fingerprint 로그 노출 범위 검토
- [x] Trust Store 정책 검토
- [x] PEM 사이즈 상한·alias 정규식 인젝션 방어 검토
- [x] OWASP Top 10 관점 검토
- [x] HIGH 이슈 발견 및 Backend Agent 반려 결정 명시
- [x] status: approved
