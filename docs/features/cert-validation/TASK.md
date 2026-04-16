---
agent: planner
feature: cert-validation
status: draft
created: 2026-04-16
updated: 2026-04-16
links:
  prd: docs/features/cert-validation/PRD.md
  task: docs/features/cert-validation/TASK.md
---

# TASK: cert-validation 구현 계획

## Phase 1 — Domain & Port 정의

- [ ] `Certificate` 도메인 엔티티 작성
- [ ] `CertificateStatus` enum 작성
- [ ] `CertificateRepository` Port (interface) 정의
- [ ] `CertificateValidationPort` Port (interface) 정의
- [ ] `CertificateService` 유스케이스 작성
  - `registerCertificate(pem: String, alias: String): Certificate`
  - `getCertificate(id: UUID): Certificate`
  - `listCertificates(): List<Certificate>`
  - `validateCertificate(id: UUID): ValidationResult`
  - `deleteCertificate(id: UUID)`

## Phase 2 — Inbound Adapter (WebFlux Router)

- [ ] `CertificateHandler` 작성 (coRouter)
- [ ] `POST /api/v1/certificates` — 등록
- [ ] `GET /api/v1/certificates` — 목록 조회
- [ ] `GET /api/v1/certificates/{id}` — 단건 조회
- [ ] `POST /api/v1/certificates/{id}/validate` — 유효성 검증
- [ ] `DELETE /api/v1/certificates/{id}` — 삭제
- [ ] Request/Response DTO 작성
- [ ] 입력 유효성 검증 (PEM 형식 체크)

## Phase 3 — Outbound Adapter (Persistence)

- [ ] jOOQ 테이블 스키마 정의 (`certificates` 테이블)
- [ ] Flyway 마이그레이션 스크립트 작성 (`V1__create_certificates.sql`)
- [ ] `CertificateRepositoryAdapter` 구현 (R2DBC + jOOQ)
- [ ] DB ↔ Domain 매핑 함수 작성

## Phase 4 — Validation Domain Service

- [ ] `X509CertificateValidator` 구현
  - PEM 파싱 (`CertificateFactory`)
  - 만료일 검증 (`notBefore`, `notAfter`)
  - CA 체인 검증 (JVM TrustStore `PKIX`)
  - Key Usage 검증
  - `daysUntilExpiry` 계산
- [ ] `ValidationResult` 값 객체 정의
  - `valid: Boolean`
  - `errors: List<String>`
  - `daysUntilExpiry: Long`

## Phase 5 — 테스트

- [ ] `CertificateService` 단위 테스트 (MockK)
- [ ] `X509CertificateValidator` 단위 테스트 (테스트 인증서 픽스처)
- [ ] `CertificateRepositoryAdapter` 통합 테스트 (Testcontainers PostgreSQL)
- [ ] API E2E 테스트 (WebTestClient)

## Phase 6 — 산출물 문서화

- [ ] `API-SPEC.md` 작성
- [ ] `DECISIONS.md` 작성
- [ ] Reviewer Agent 검토 요청
- [ ] Security Agent 검토 요청

---

## 구현 디렉터리 구조 (예상)

```
next-cm/src/main/kotlin/com/ktcloud/kcp/cm/
├── certificate/
│   ├── domain/
│   │   ├── Certificate.kt
│   │   ├── CertificateStatus.kt
│   │   └── ValidationResult.kt
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   ├── RegisterCertificateUseCase.kt
│   │   │   │   ├── GetCertificateUseCase.kt
│   │   │   │   ├── ValidateCertificateUseCase.kt
│   │   │   │   └── DeleteCertificateUseCase.kt
│   │   │   └── out/
│   │   │       ├── CertificateRepository.kt
│   │   │       └── CertificateValidationPort.kt
│   │   └── service/
│   │       └── CertificateService.kt
│   └── adapter/
│       ├── in/
│       │   └── web/
│       │       ├── CertificateHandler.kt
│       │       ├── CertificateRouter.kt
│       │       └── dto/
│       │           ├── RegisterCertificateRequest.kt
│       │           ├── CertificateResponse.kt
│       │           └── ValidationResultResponse.kt
│       └── out/
│           ├── persistence/
│           │   ├── CertificateRepositoryAdapter.kt
│           │   └── CertificateRecord.kt
│           └── validation/
│               └── X509CertificateValidator.kt
└── ...

resources/db/migration/
└── V1__create_certificates.sql
```
