---
agent: cicd
feature: cert-validation
status: approved
created: 2026-04-16
updated: 2026-04-16
links:
  prd: docs/features/cert-validation/PRD.md
  api: docs/features/cert-validation/API-SPEC.md
  decisions: docs/features/cert-validation/DECISIONS.md
  review: docs/features/cert-validation/REVIEW.md
  security: docs/features/cert-validation/SECURITY-AUDIT.md
  test: docs/features/cert-validation/TEST-PLAN.md
---

# PR: Certificate Manager - 인증서 등록·관리·유효성 검증

## 변경 요약

공인 CA(Public Certificate Authority) 발급 X.509 인증서의 중앙 집중식 관리 및 유효성 검증 기능을 추가합니다.
- **5개 REST API 엔드포인트** 구현 (등록/조회/검증/삭제)
- **Hexagonal Architecture** 기반 도메인 로직 순수성 유지
- **JVM cacerts 신뢰 앵커** 기반 CA 체인 검증
- **상태 기반 인증서 라이프사이클 관리** (VALID/EXPIRED/PENDING/REVOKED)

## What & Why

### 배경
- 조직 내 여러 서비스에서 사용하는 인증서를 분산 관리 중
- 만료 예측 불가, 서명 검증 기준 불일치로 운영 위험
- 중앙 집중식 등록·검증 플랫폼 필요

### 목표
- 인증서 메타데이터(subject, issuer, 만료일 등) 중앙 저장
- 자동 유효성 검증 (만료/CA 체인)
- 인증서 상태 추적 (VALID → EXPIRED, REVOKED)
- 만료 임박 시 조회 가능한 정보 제공 (`daysUntilExpiry`)

## 주요 구현 내용

### 1. API 엔드포인트 (5종)

| Method | Path | 설명 | 상태코드 |
|--------|------|------|--------|
| `POST` | `/api/1.0/certificates` | 인증서 등록 (PEM) | 201 Created / 400 / 409 |
| `GET` | `/api/1.0/certificates` | 인증서 목록 조회 | 200 OK |
| `GET` | `/api/1.0/certificates/{id}` | 단건 조회 | 200 OK / 404 |
| `POST` | `/api/1.0/certificates/{id}/validate` | 유효성 재검증 | 200 OK / 404 |
| `DELETE` | `/api/1.0/certificates/{id}` | 인증서 삭제 | 204 No Content / 404 |

### 2. 도메인 설계

**Certificate** (인증서)
```
id: UUID
alias: String (사용자 식별명, 1-128 문자)
subject: String (CN=...)
issuer: String (CA 정보)
serialNumber: String (16진수)
notBefore / notAfter: Instant
status: CertificateStatus (VALID | EXPIRED | PENDING | REVOKED)
pemContent: String (PEM 형식 원문)
fingerprint: String (SHA-256, UNIQUE 제약)
keyUsage: List<String>
subjectAltNames: List<String>
daysUntilExpiry: Int (계산 속성)
createdAt / updatedAt: Instant
```

**CertificateStatus** (상태 전이)
```
PENDING  → 등록 후 검증 전 (notBefore > now)
VALID    → 유효한 인증서 (notBefore ≤ now ≤ notAfter)
EXPIRED  → 만료된 인증서 (notAfter < now)
REVOKED  → 폐기됨 (단방향, 덮어쓰기 불가)
```

### 3. 검증 로직

| 항목 | 구현 | 결과 |
|-----|------|------|
| **만료일 검증** | `notBefore ≤ now ≤ notAfter` | VALID/EXPIRED 상태 결정 |
| **CA 체인 검증** | JVM cacerts 기반 PKIX | 신뢰된 경로 확인 |
| **Key Usage** | TLS Server Auth 확인 | 검증 결과에 포함 |
| **CRL/OCSP** | 비활성화 (Phase 2) | `revocationChecked=false` 명시 |

### 4. 아키텍처 레이어

**Domain**
- `Certificate`: 순수 도메인 엔티티 (외부 의존 없음)
- `CertificateStatus`: enum (상태 기계)
- `ValidationResult`: 검증 결과 (valid, errors[], daysUntilExpiry)

**Application**
- `CertificateService`: 비즈니스 로직 (등록/조회/검증/삭제)
- `RegisterCertificateUseCase`, `GetCertificateUseCase` 등: Use Case 인터페이스
- Port/Adapter 계약 준수

**Adapter (In)**
- `CertificateRouter`: WebFlux coRouter (경로 정의)
- `CertificateHandler`: 요청 처리 (유효성 검사, 응답 매핑)

**Adapter (Out)**
- `CertificatePersistenceAdapter`: jOOQ 기반 Repository (JDBC + TransactionalPort)
- `X509CertificateValidator`: X.509 JCA 파싱 및 검증

### 5. 설계 결정 (ADR 5건)

1. **ADR-001**: JDBC + jOOQ 사용 (R2DBC 대신, 기존 스켈레톤 호환)
2. **ADR-002**: fingerprint SHA-256 UNIQUE 제약 (중복 등록 방지)
3. **ADR-003**: 검증 실패도 200 OK (4xx 아님) - 부분 검증 가능
4. **ADR-004**: JVM cacerts 신뢰 앵커 (공공 CA 기준)
5. **ADR-005**: 기존 `DefaultException` + 전역 핸들러 재사용

자세한 Trade-off 분석은 [DECISIONS.md](docs/features/cert-validation/DECISIONS.md) 참조.

## 리뷰 포인트

### Hexagonal 경계
✅ Domain이 외부 의존 없이 순수 Kotlin
✅ Port/Adapter 계약 명확 (CertificateRepository, CertificateValidationPort)
✅ Handler는 UseCase 인터페이스만 참조

### 상태 전이 로직
✅ REVOKED 단방향 보호 (덮어쓰기 불가)
✅ EXPIRED 우선 처리
⚠️ MEDIUM-02: 재검증 실패 시 VALID 유지 정책 → ADR/코드 주석으로 명시됨

### Coroutine ↔ Blocking jOOQ 브리지
✅ `transactionalPort.execute { }` 래핑으로 virtual-thread dispatcher 사용
✅ Spring WebFlux 이벤트 루프 블로킹 회피
⚠️ LOW-02: Phase 2 OCSP 활성화 시 `withContext(Dispatchers.IO)` 추가 필요

### 테스트 커버리지
✅ Domain 레이어: ~95% (경계값 포함)
✅ Service 레이어: ~60% (핵심 경로 + 상태 결정)
⚠️ Adapter 테스트: 별도 issue (REVIEW.md LOW-04 선행 필요)
- Domain: CertificateTest 11개 (isExpired, daysUntilExpiry 등)
- Service: CertificateServiceTest 4개 (register/validate 핵심 경로)
- Validator: X509CertificateValidatorTest 3개 (에러 경로)

## 테스트 결과 요약

### 통과 항목
- ✅ Domain 레이어 테스트 11개 모두 작동
  - isExpired / isNotYetValid 경계값 (notBefore/notAfter 정확도)
  - daysUntilExpiry 계산 (일수 truncate)
  - withStatus 불변성
- ✅ Service 레이어 상태 결정 로직
  - register: VALID/EXPIRED/PENDING 정확한 판정
  - validate: REVOKED 보호, 만료 우선 처리
- ✅ Validator 에러 경로
  - 잘못된 PEM 형식 → `InvalidPemFormatException` (400)

### 알려진 제약
- ⚠️ Adapter (Web/Persistence) 테스트: Phase 2 권장
  - WebTestClient 슬라이스 테스트 필요
  - Testcontainers 통합 테스트 필요
- ⚠️ Mock Setup Issue (runTest + coEvery 혼용)
  - REVIEW.md LOW-04에서 인식됨
  - getCertificate, delete, register throws 테스트 3개 유보

### 커버리지 목표 (Phase 1)
| 레이어 | 달성 | 목표 |
|--------|------|------|
| Domain | ~95% | 90%+ |
| Service | ~60% | 85%+ |
| Adapter | 0% | 70%+ (Phase 2) |
| **전체** | **~65%** | **80%+ (Phase 2 후)** |

## 보안 고려사항

### 인증/인가 (High Priority - auth-layer feature로 분리)
⚠️ **현재 미구현 (의도적 defer)**
- Spring Security 레이어 추가 필요 (JWT / OAuth2)
- BOLA 방어: `owner_id` 컬럼 추가 및 소유자 필터링
- DELETE 엔드포인트: `ROLE_ADMIN` 또는 소유자 확인

→ **분리 근거**: 인증/인가는 횡단 관심사(cross-cutting concern)로, cert-validation 도메인과 독립적
→ **추적**: SECURITY-AUDIT.md에 명시, auth-layer feature에서 통합 처리 예정

### 기타 보안 항목 (Phase 2)
- ⚠️ PEM 원문 평문 저장 (MEDIUM) → 데이터베이스 암호화(TDE) 또는 AES-256 검토
- ⚠️ CRL/OCSP 미검증 (MEDIUM) → Phase 2에서 활성화, 타임아웃 설정 필수
- ✅ Alias 정규식 (SQL Injection 방어)
- ✅ PEM 사이즈 상한 32KB

### 보안 감사 완료
- ✅ OWASP Top 10 (일반) A03 Injection: SQL Injection 방어됨 (jOOQ parameterized query)
- ✅ A02 Cryptographic Failures: SHA-256 fingerprint, MessageDigest thread-safe 패턴
- ⚠️ A01 BOLA, API2 Broken Authentication, API5 권한 부여: auth-layer 분리 → SECURITY-AUDIT 참조

자세한 내용은 [SECURITY-AUDIT.md](docs/features/cert-validation/SECURITY-AUDIT.md) 참조.

## Out of Scope (Phase 2 이후)

| 항목 | 사유 |
|-----|------|
| CRL / OCSP 실시간 폐기 조회 | 외부 네트워크 의존, 타임아웃 관리 필요 |
| 인증서 자동 갱신 | 인증서 수명 관리 정책 미정 |
| HSM / Key Vault 연동 | 별도 인프라 요구 |
| mTLS 클라이언트 인증 | 별도 feature 범위 |
| **인증/인가 (Spring Security)** | **auth-layer feature로 분리** |

## 배포 체크리스트

- [x] PRD / API-SPEC / DECISIONS 완료
- [x] Reviewer 코드 리뷰 통과 (HIGH 없음, MEDIUM/LOW 인식)
- [x] Security 감사 통과 (HIGH 이슈 auth-layer로 분리)
- [x] QA 테스트 계획 완료 (조건부 승인)
- [x] DB 마이그레이션 스크립트 (V1__create_certificates.sql)
- [ ] 프로덕션 배포 (auth-layer feature 배포 전까지 보호)

## 변경 사항 요약

### 신규 파일 (17개)
```
src/main/kotlin/com/ktcloud/kcp/cm/certificate/
├── domain/
│   ├── Certificate.kt
│   ├── CertificateStatus.kt
│   └── ValidationResult.kt
├── application/
│   ├── port/in/
│   │   ├── RegisterCertificateUseCase.kt
│   │   ├── GetCertificateUseCase.kt
│   │   ├── ValidateCertificateUseCase.kt
│   │   └── DeleteCertificateUseCase.kt
│   ├── port/out/
│   │   ├── CertificateRepository.kt
│   │   └── CertificateValidationPort.kt
│   └── service/
│       └── CertificateService.kt
├── adapter/
│   ├── in/web/
│   │   ├── CertificateRouter.kt
│   │   ├── CertificateHandler.kt
│   │   └── dto/
│   │       ├── RegisterCertificateRequest.kt
│   │       ├── CertificateResponse.kt
│   │       └── ValidationResultResponse.kt
│   └── out/
│       ├── persistence/
│       │   └── CertificatePersistenceAdapter.kt
│       └── validation/
│           └── X509CertificateValidator.kt
└── exception/
    ├── CertificateNotFoundException.kt
    ├── CertificateAlreadyRegisteredException.kt
    └── InvalidPemFormatException.kt

src/main/resources/
├── db/migration/
│   └── V1__create_certificates.sql
└── enums/
    └── CertificateErrorCode.kt

src/test/kotlin/com/ktcloud/kcp/cm/certificate/
├── domain/
│   └── CertificateTest.kt
├── application/service/
│   └── CertificateServiceTest.kt
└── adapter/out/validation/
    └── X509CertificateValidatorTest.kt
```

### 주요 변경 파일
- `CertificateRouter.kt`: WebFlux coRouter 정의 (경로 + 버전 설정)
- `CertificateHandler.kt`: 요청/응답 매핑 (Content negotiation)
- `CertificateService.kt`: 상태 결정 로직 + 트랜잭션 관리
- `CertificatePersistenceAdapter.kt`: jOOQ DSLContext 쿼리 구현
- `X509CertificateValidator.kt`: X.509 파싱 + PKIX 검증

### 에러 코드 (CertificateErrorCode enum)
```
ECRT001: CERTIFICATE_NOT_FOUND (404)
ECRT002: CERTIFICATE_ALREADY_REGISTERED (409)
ECRT003: INVALID_PEM_FORMAT (400)
```

## 참고 자료

- **PRD**: [PRD.md](docs/features/cert-validation/PRD.md) - 배경·목표·범위
- **API-SPEC**: [API-SPEC.md](docs/features/cert-validation/API-SPEC.md) - 엔드포인트 상세명세
- **설계 결정**: [DECISIONS.md](docs/features/cert-validation/DECISIONS.md) - ADR 5건
- **코드 리뷰**: [REVIEW.md](docs/features/cert-validation/REVIEW.md) - Hexagonal 경계·테스트
- **보안 감사**: [SECURITY-AUDIT.md](docs/features/cert-validation/SECURITY-AUDIT.md) - OWASP + auth-layer 분리
- **테스트 계획**: [TEST-PLAN.md](docs/features/cert-validation/TEST-PLAN.md) - 42개 테스트 케이스

## 롤백 계획

### 롤백 전제
프로덕션 배포 후 P0 결함 발견 시:

```bash
# 1. DB 롤백 (Flyway)
# (이전 버전으로 git checkout 후 배포)
# → V1__create_certificates.sql 미실행, certificates 테이블 미생성

# 2. 애플리케이션 롤백 (파드 재시작)
kubectl set image deployment/next-cm \
  next-cm=<previous-image-sha> \
  -n production

# 3. 헬스 체크
curl https://api.example.com/health
```

### 주의사항
- certificates 테이블에 데이터가 존재하면 이전 버전(테이블 미인식)에서 오류 가능
  → 롤백 후 데이터 정제 필요 (운영팀과 조율)
- auth-layer feature 배포 전까지는 프로덕션 노출 금지 (보안 위험)

## 승인 체크리스트

- [x] 모든 Feature 산출물 (PRD/API-SPEC/DECISIONS/REVIEW/SECURITY-AUDIT/TEST-PLAN) 완료
- [x] Reviewer 승인 (HIGH 없음)
- [x] Security 승인 (HIGH 이슈 auth-layer로 분리 인식)
- [x] QA 승인 (조건부, Domain 테스트 작동)
- [x] DB 마이그레이션 스크립트 준비
- [x] 설계 결정 문서화 (ADR 5건)
- [x] 롤백 계획 구체화
