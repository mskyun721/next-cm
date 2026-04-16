---
agent: backend
feature: cert-validation
status: approved
created: 2026-04-16
updated: 2026-04-16
links:
  prd: docs/features/cert-validation/PRD.md
---

# API-SPEC: Certificate Manager (cert-validation)

> REST API 상세 명세. `standards/api-contract.md` 규약 준수.
> 본 문서는 팀 공통 리뷰용 요약이며, 실제 OpenAPI YAML은 Phase 2 작업 범위로 유보한다.

## 공통
- Base URL (dev): `http://localhost:18080`
- Path prefix: `/api/{version}/...` (현재 `version=1.0`이 `api.version` predicate에 매핑됨)
  - 예: `POST /api/1.0/certificates`
  - 논리 버전 표기는 `v1`이지만 WebFlux `RequestPredicates.version("1.0")` 규약 때문에 URL 세그먼트는 `1.0` 이다.
- 인증: Phase 2에서 `Authorization: Bearer <JWT>` 추가 예정 (현재 skeleton 미적용)
- Content-Type: `application/json; charset=UTF-8`
- 공통 에러 포맷: `ApiErrorResponse` (프로젝트 표준)

---

## Endpoint 1: 인증서 등록

### 기본 정보
| 항목 | 값 |
|---|---|
| Method | `POST` |
| Path | `/api/1.0/certificates` |
| 권한 | Phase 2: 인증 사용자 |
| 멱등성 | fingerprint 단위로 중복 방지 (`409 CERTIFICATE_ALREADY_REGISTERED`) |

### Request
#### Body
```json
{
  "alias": "my-service-prod",
  "pemContent": "-----BEGIN CERTIFICATE-----\nMIID...\n-----END CERTIFICATE-----"
}
```

| Field | Type | Required | 제약 | 설명 |
|---|---|---|---|---|
| alias | string | Y | 1-128, `[A-Za-z0-9._-]+` | 사용자 식별명 |
| pemContent | string | Y | ≤32 KB | PEM 인코딩 X.509 인증서 |

### Response
#### 201 Created
```json
{
  "id": "b6d9e2e1-8f5a-4c10-9a1d-5e2f0e9b1c77",
  "alias": "my-service-prod",
  "subject": "CN=my-service.example.com",
  "issuer": "CN=Let's Encrypt R3, O=Let's Encrypt, C=US",
  "serialNumber": "03f1...",
  "notBefore": "2026-01-01T00:00:00Z",
  "notAfter": "2026-04-01T00:00:00Z",
  "status": "VALID",
  "fingerprint": "AB:CD:...",
  "keyUsage": ["digitalSignature", "keyEncipherment"],
  "subjectAltNames": ["my-service.example.com"],
  "daysUntilExpiry": 60,
  "createdAt": "2026-04-16T10:00:00Z",
  "updatedAt": "2026-04-16T10:00:00Z"
}
```

#### 에러
| HTTP | code | 조건 |
|---|---|---|
| 400 | `ECRT003` (INVALID_PEM_FORMAT) | PEM 파싱 실패 |
| 400 | `EKCP010` (VALIDATION_FAIL) | alias/pemContent 검증 실패 |
| 409 | `ECRT002` (CERTIFICATE_ALREADY_REGISTERED) | 동일 fingerprint 존재 |

---

## Endpoint 2: 인증서 목록 조회

| 항목 | 값 |
|---|---|
| Method | `GET` |
| Path | `/api/1.0/certificates` |

### Response
#### 200 OK
`CertificateResponse[]` 형태. 각 항목은 Endpoint 1 응답과 동일 구조.

---

## Endpoint 3: 인증서 단건 조회

| 항목 | 값 |
|---|---|
| Method | `GET` |
| Path | `/api/1.0/certificates/{id}` |

### Path Parameters
| Name | Type | 제약 |
|---|---|---|
| id | UUID | RFC 4122 |

### Response
- 200 OK: `CertificateResponse`
- 404: `ECRT001 CERTIFICATE_NOT_FOUND`
- 400: `EKCP012 INVALID_PARAMETER` (id가 UUID 형식 아님)

---

## Endpoint 4: 인증서 유효성 검증 실행

| 항목 | 값 |
|---|---|
| Method | `POST` |
| Path | `/api/1.0/certificates/{id}/validate` |

### Response
#### 200 OK
```json
{
  "valid": true,
  "errors": [],
  "daysUntilExpiry": 60
}
```

| Field | Type | 설명 |
|---|---|---|
| valid | boolean | 만료+체인 검증 모두 통과 여부 |
| errors | string[] | 실패 사유(메시지) 목록. 성공 시 빈 배열 |
| daysUntilExpiry | integer | 현재 시점 기준 만료까지 남은 일수 (음수면 이미 만료) |

#### 동작
- 실패해도 `200 OK`로 검증 결과를 반환한다 (PRD A3). `status`가 필요시 `EXPIRED`로 자동 갱신.
- `REVOKED` 상태는 자동 변경되지 않는다.

#### 에러
- 404: `ECRT001 CERTIFICATE_NOT_FOUND`

---

## Endpoint 5: 인증서 삭제

| 항목 | 값 |
|---|---|
| Method | `DELETE` |
| Path | `/api/1.0/certificates/{id}` |

### Response
- 204 No Content
- 404: `ECRT001 CERTIFICATE_NOT_FOUND`

---

## 상태 전이 (CertificateStatus)

```
PENDING ──(register: notBefore > now)──► PENDING
PENDING ──(register: within window)──► VALID
PENDING ──(register: notAfter < now)──► EXPIRED
VALID   ──(validate: expired)─────────► EXPIRED
VALID   ──(validate: still valid)─────► VALID
any     ──(manual)─────────────────────► REVOKED  (현재 API에서는 직접 전환 경로 없음; DB 레벨 또는 향후 API)
```

## Backward Compatibility
- 모든 필드 추가는 non-breaking.
- `status` enum 추가는 consumer 대응 확인 필요.

## Quality Gate
- [x] 모든 엔드포인트 Request/Response 예시 포함
- [x] 모든 에러 케이스 나열
- [x] 이벤트 스펙 없음 (Out-of-scope)
- [ ] OpenAPI YAML 파일과 일치 (Phase 2)
- [x] status: `approved`
