---
agent: backend
feature: cert-validation
status: approved
created: 2026-04-16
updated: 2026-04-16
---

# DECISIONS: Certificate Manager (cert-validation)

## ADR-001: Persistence 스택은 R2DBC 대신 JDBC + jOOQ 사용
- **Date**: 2026-04-16
- **Status**: Accepted
- **Context**
  - PRD Assumption A4는 "RDB (jOOQ + R2DBC)"를 제시하였다.
  - 그러나 본 스켈레톤 `next-cm` 프로젝트는 이미 **JDBC + jOOQ + Hikari + TransactionTemplate** 로 구성되어 있고, 전용 `TransactionalExecutorAdapter`(virtual-thread dispatcher 기반)로 코루틴에서 동기 jOOQ 호출을 안전하게 브리지한다.
  - R2DBC로 전환하려면 Hikari/TransactionTemplate 기반 인프라를 전부 교체해야 하며, 이는 본 feature 범위를 크게 벗어난다.
- **Decision**
  - 기존 `TransactionalPort` / jOOQ DSLContext 를 그대로 사용하고, coroutine ↔ blocking 브리지는 `transactionalPort.execute { … }` 로 감싼다.
- **Alternatives Considered**
  | 대안 | 장점 | 단점 | 기각 사유 |
  |---|---|---|---|
  | jOOQ + JDBC (채택) | 기존 스켈레톤 재사용, 즉시 가용 | 진짜 non-blocking 아님 | — |
  | jOOQ + R2DBC | 완전 reactive 스택 | 인프라 전면 재구성 필요 | 범위 초과 |
  | Spring Data R2DBC | 구현 빠름 | jOOQ 매핑 패턴과 일관성 붕괴 | 기존 sample 패턴 분기 |
- **Consequences**
  - (+) sample 도메인과 완전한 일관성. 기존 트랜잭션/읽기 라우팅 기능(`readTransactionTemplate`) 혜택 그대로 사용.
  - (−) PRD 기술 제시와 약간의 불일치. API 응답 시간 목표(p99 < 500ms)에는 영향 없음.
  - 재검토 조건: 장기적으로 DB 연결 수 압박이 있을 때 R2DBC로 이관.

---

## ADR-002: 등록 중복 방지는 fingerprint(SHA-256) UNIQUE 제약으로 구현
- **Date**: 2026-04-16
- **Status**: Accepted
- **Context**
  - 동일 인증서를 서로 다른 alias 로 중복 등록할 경우, 운영자 혼선과 검증 결과 불일치가 발생할 수 있다.
- **Decision**
  - `certificates.fingerprint` 컬럼에 UNIQUE 제약을 두고, `register()` 트랜잭션 내에서 `findByFingerprint()` 선행 조회 후 존재 시 `CertificateAlreadyRegisteredException` 을 던진다 (409).
  - fingerprint 는 DER 인코딩 인증서의 SHA-256 해시이다 (RFC 5280의 일반적 식별 규약).
- **Alternatives Considered**
  | 대안 | 장점 | 단점 | 기각 사유 |
  |---|---|---|---|
  | fingerprint UNIQUE (채택) | 강한 중복 차단, 표준 식별자 | — | — |
  | (subject+issuer+serial) 복합 유니크 | RFC 식별 기준 | CA 내에서만 유일하며 cross-CA 충돌 시 판별 불가 | 실무 혼선 |
  | alias UNIQUE만 | 구현 단순 | 같은 물리 인증서를 alias만 다르게 등록 가능 | 요구 미충족 |
- **Consequences**
  - (+) 동일 물리 인증서에 대해 단 하나의 레코드만 존재.
  - (−) 선조회 + INSERT 경합 시 race 발생 가능 → DB UNIQUE 위반이 최종 방어선으로 작동 (`SQLIntegrityConstraintViolationException`). 현재는 409 매핑 전역 처리되지 않으므로 후속 개선 대상.
  - 재검토 조건: 고동시성 등록 환경에서 409 포맷 누락이 관찰되면 `@RestControllerAdvice` 에 DB 예외 매핑 추가.

---

## ADR-003: 유효성 검증 실패를 요청 실패(4xx)로 보지 않고 200 OK 응답으로 반환
- **Date**: 2026-04-16
- **Status**: Accepted
- **Context**
  - PRD Assumption A3 "검증 실패 처리: 검증 결과를 응답으로 반환 (요청 차단 아님)".
- **Decision**
  - `/validate` API 는 검증 실패 여부와 무관하게 `ValidationResult` 를 200 으로 직렬화하여 반환. 실패 세부는 `errors: []` 에 포함.
  - 단, 인증서가 존재하지 않으면 404 (NOT_FOUND). PEM 자체 파싱 실패는 400.
- **Alternatives Considered**
  | 대안 | 장점 | 단점 | 기각 사유 |
  |---|---|---|---|
  | 200 + 결과 본문 (채택) | 클라이언트가 부분 실패(만료만/체인만)를 구체적으로 분류 가능 | HTTP 코드만 보는 모니터링은 구분 어려움 | — |
  | 422 Unprocessable Entity | 상태 코드로 실패 명시 | PRD 원칙과 어긋남 | — |
- **Consequences**
  - (+) 클라이언트 UX 단순화, `daysUntilExpiry` 같은 부가 정보 제공.
  - (−) APM이 HTTP 코드만 집계할 경우 실패 추적 누락 가능 → Micrometer `certificate.validation.failure` 커스텀 카운터를 Phase 2에 추가 예정.

---

## ADR-004: Trust Anchor 는 JVM default cacerts 재사용
- **Date**: 2026-04-16
- **Status**: Accepted
- **Context**
  - PRD Assumption A2: 공공 CA 기반 공인 인증서.
  - 자체 Trust Store를 코드에 번들하면 운영 갱신(루트 CA 교체) 시 재배포가 필요하다.
- **Decision**
  - `TrustManagerFactory.getInstance(default).init(null)` 로 JVM cacerts 를 로드하여 `PKIXParameters` 의 TrustAnchor 집합을 구성. `CertPathValidator("PKIX")` 로 체인 검증.
  - `isRevocationEnabled = false` (CRL/OCSP 는 Phase 2).
- **Alternatives Considered**
  | 대안 | 장점 | 단점 | 기각 사유 |
  |---|---|---|---|
  | JVM cacerts (채택) | JDK 업데이트와 함께 갱신, 운영 단순 | JVM 버전 의존 | — |
  | 자체 PEM 묶음 | 명시적 통제 | 갱신 프로세스 필요 | 비용 대비 효익 낮음 |
- **Consequences**
  - (+) JDK 패치와 함께 신뢰 앵커 자동 갱신.
  - (−) 사내 전용 CA를 Trust 해야 하는 케이스는 Phase 2에 "추가 trust store 경로 설정" 옵션으로 확장.

---

## ADR-005: 예외 처리는 기존 `DefaultException` + 전역 핸들러 패턴 재사용
- **Date**: 2026-04-16
- **Status**: Accepted
- **Context**
  - 스켈레톤에 `CommonExceptions`, `SampleNotFoundException`, `GlobalExceptionHandler` 패턴이 이미 존재.
- **Decision**
  - `CertificateErrorCode` enum 을 추가하고, `CertificateNotFoundException`, `CertificateAlreadyRegisteredException`, `InvalidPemFormatException` 을 `DefaultException` 상속으로 구현.
  - fingerprint 는 로그/메시지에 **12자리까지만 노출** (PII/운영 위생).
- **Consequences**
  - (+) 오류 응답 포맷 일관성. 추가 전역 핸들러 수정 불필요.
  - (−) 메시지 포맷 변경은 i18n properties 의존.

---

## Quality Gate
- [x] Trade-off 가 있었던 결정 5건 기록
- [x] 대안 비교표 포함
- [x] Consequences의 부정적 영향 명시
- [x] status: `approved`

## 자체 빌드 검증 노트
- `./gradlew jooqCodegen` 은 성공. `CERTIFICATES` 테이블 컬럼이 예상대로 생성됨 (`ID/ALIAS/NOT_BEFORE/…`).
- `./gradlew compileKotlin` 은 **기존 스켈레톤의 duplicate-package 이슈** (`com.example.skeleton`/`com.ktcloud.kcp.cm` 동시 존재, `MessageConverter`/`Tuple4-8` 등 중복 선언) 로 실패한다. 본 feature 변경과 무관한 사전 이슈이며, sample 도메인 자체도 같은 에러로 컴파일되지 않는다.
- 따라서 `ktlintCheck detekt test` 전체 Quality Gate 는 해당 사전 이슈 해결 뒤 재실행이 필요하다 (Handoff 시 Reviewer/Ops 공유).
