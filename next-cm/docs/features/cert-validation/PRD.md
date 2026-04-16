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

# PRD: 인증서 등록·관리·유효성 검증

## 1. 배경 및 목적

`next-cm`은 공인 CA(Public Certificate Authority)에서 발급한 X.509 인증서를 중앙에서 등록·관리·검증하는 Certificate Manager 플랫폼이다. 본 기능은 인증서의 전체 라이프사이클(등록 → 저장 → 유효성 검증 → 만료 알림)을 지원한다.

## 2. 목표

- 공인 CA 발급 X.509 인증서를 시스템에 등록하고 메타데이터 저장
- 등록된 인증서의 유효성(만료·서명·체인)을 검증하는 API 제공
- 인증서 만료 임박 시 조회 가능한 상태 정보 제공
- Hexagonal Architecture 기반으로 도메인 순수성 유지

## 3. 범위 (In-Scope)

| 항목 | 설명 |
|---|---|
| 인증서 등록 | PEM 형식 인증서를 API로 제출하여 시스템에 저장 |
| 인증서 조회 | 등록된 인증서 목록 및 단건 조회 |
| 유효성 검증 | 만료일, CA 체인 서명, Key Usage 검증 |
| 상태 관리 | VALID / EXPIRED / REVOKED / PENDING 상태 관리 |
| 인증서 삭제 | 등록된 인증서 삭제 |

## 4. 범위 외 (Out-of-Scope)

- CRL / OCSP 실시간 폐기 조회 (Phase 2)
- 인증서 자동 갱신 (Phase 2)
- HSM / Key Vault 연동 (Phase 2)
- mTLS 클라이언트 인증 (별도 기능)

## 5. 주요 도메인 개념

### 5.1 Certificate (인증서)

```
Certificate
  ├── id: UUID
  ├── alias: String (사용자 지정 식별명)
  ├── subject: String (CN=...)
  ├── issuer: String (CA 정보)
  ├── serialNumber: String
  ├── notBefore: Instant
  ├── notAfter: Instant
  ├── status: CertificateStatus (VALID | EXPIRED | REVOKED | PENDING)
  ├── pemContent: String (PEM 원문)
  ├── fingerprint: String (SHA-256)
  ├── keyUsage: List<String>
  ├── subjectAltNames: List<String>
  ├── createdAt: Instant
  └── updatedAt: Instant
```

### 5.2 CertificateStatus

| 값 | 설명 |
|---|---|
| `VALID` | 유효한 인증서 |
| `EXPIRED` | 만료된 인증서 |
| `REVOKED` | 폐기된 인증서 (수동 처리) |
| `PENDING` | 등록 후 검증 전 상태 |

## 6. API 설계 (초안)

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/v1/certificates` | 인증서 등록 |
| `GET` | `/api/v1/certificates` | 인증서 목록 조회 |
| `GET` | `/api/v1/certificates/{id}` | 인증서 단건 조회 |
| `POST` | `/api/v1/certificates/{id}/validate` | 인증서 유효성 검증 실행 |
| `DELETE` | `/api/v1/certificates/{id}` | 인증서 삭제 |

## 7. 유효성 검증 항목

| 검증 항목 | 설명 | Assumption |
|---|---|---|
| 만료일 검증 | `notBefore ≤ now ≤ notAfter` | 필수 |
| CA 체인 서명 검증 | JVM 기본 TrustStore(공공 CA) 기준 | 필수 |
| Key Usage | TLS Server Auth 용도 확인 | 선택 (설정 가능) |
| CN/SAN 일치 | 도메인 일치 여부 확인 | 선택 (요청 파라미터) |
| Certificate Pinning | 특정 공개키 해시 고정 | Out-of-Scope |

## 8. Assumptions

| # | 항목 | Assumption |
|---|---|---|
| A1 | 인증서 소스 | API Body에 PEM 형식으로 직접 제출 |
| A2 | 신뢰 앵커 | JVM 기본 TrustStore (공공 CA 신뢰) |
| A3 | 검증 실패 처리 | 검증 결과를 응답으로 반환 (요청 차단 아님) |
| A4 | 인증서 저장소 | RDB (jOOQ + R2DBC) |
| A5 | 검증 트리거 | `/validate` API 명시적 호출 + 등록 시 자동 검증 |
| A6 | 만료 알림 | 조회 API 응답에 `daysUntilExpiry` 포함으로 대체 |

## 9. 비기능 요구사항

- 응답 시간: 단건 검증 API p99 < 500ms
- 보안: PEM 원문은 저장 시 암호화 고려 (Phase 2), 조회 API는 인증 필요
- 테스트: 도메인 단위 테스트 100%, 통합 테스트 핵심 경로 커버

## 10. 완료 조건 (Definition of Done)

- [ ] 인증서 등록·조회·삭제 API 동작
- [ ] 유효성 검증 API 동작 (만료·CA체인 검증)
- [ ] 단위 테스트 작성 (MockK)
- [ ] 통합 테스트 작성 (Testcontainers)
- [ ] Reviewer / Security Agent 검토 통과
- [ ] API-SPEC.md 작성 완료