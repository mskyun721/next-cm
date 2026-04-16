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
---

# RELEASE-NOTE: Certificate Manager (cert-validation)

## 버전 정보

| 항목 | 값 |
|-----|-----|
| 버전 | v1.0.0 |
| 출시일 | 2026-04-16 |
| 지원 브라우저 / 클라이언트 | REST API 호출 가능한 모든 환경 |
| 개발 기간 | 4주 (Phase 1) |

## 신규 기능

### 1. 인증서 관리 API (5개 엔드포인트)

#### 1.1 인증서 등록
```
POST /api/1.0/certificates
Content-Type: application/json

Request:
{
  "alias": "my-service-prod",
  "pemContent": "-----BEGIN CERTIFICATE-----\nMIID...\n-----END CERTIFICATE-----"
}

Response: 201 Created
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

**특징**
- PEM 형식 X.509 인증서 등록
- 자동 메타데이터 추출 (subject, issuer, 만료일 등)
- 자동 상태 판정 (VALID/EXPIRED/PENDING)
- 중복 등록 방지 (fingerprint UNIQUE)

**에러 처리**
| HTTP | Code | 조건 |
|------|------|------|
| 400 | `ECRT003` | PEM 파싱 실패 |
| 400 | `EKCP010` | 입력 검증 실패 (alias 정규식, PEM 크기) |
| 409 | `ECRT002` | 동일 fingerprint 존재 |

#### 1.2 인증서 목록 조회
```
GET /api/1.0/certificates

Response: 200 OK
[
  { ... CertificateResponse ... },
  { ... CertificateResponse ... }
]
```

#### 1.3 인증서 단건 조회
```
GET /api/1.0/certificates/{id}

Response: 200 OK
{ ... CertificateResponse ... }

Error: 404 Not Found (CERTIFICATE_NOT_FOUND)
```

#### 1.4 유효성 검증 (재검증)
```
POST /api/1.0/certificates/{id}/validate

Response: 200 OK
{
  "valid": true,
  "errors": [],
  "daysUntilExpiry": 60,
  "revocationChecked": false
}
```

**검증 항목**
- ✅ 만료일 검증 (notBefore ≤ now ≤ notAfter)
- ✅ CA 체인 검증 (JVM cacerts 기반)
- ✅ Key Usage 확인
- ⚠️ CRL/OCSP: 비활성화 (Phase 2에서 활성화)

**응답 필드**
- `valid`: 만료 + 체인 검증 모두 통과 여부
- `errors`: 검증 실패 사유 메시지 배열
- `daysUntilExpiry`: 현재 기준 만료까지 남은 일수 (음수면 만료됨)
- `revocationChecked`: CRL/OCSP 검증 여부 (Phase 1: false)

#### 1.5 인증서 삭제
```
DELETE /api/1.0/certificates/{id}

Response: 204 No Content

Error: 404 Not Found (CERTIFICATE_NOT_FOUND)
```

### 2. 인증서 상태 관리

| 상태 | 전이 조건 | 설명 |
|------|---------|------|
| **PENDING** | 등록 시 `notBefore > now` | 아직 유효하지 않은 인증서 |
| **VALID** | 등록/검증 시 `notBefore ≤ now ≤ notAfter` | 현재 유효한 인증서 |
| **EXPIRED** | 등록/검증 시 `notAfter < now` | 만료된 인증서 |
| **REVOKED** | 수동 처리 (현재 API 미제공) | 폐기된 인증서 (단방향) |

**상태 전이 규칙**
- PENDING → VALID / EXPIRED (검증 시)
- VALID → EXPIRED (재검증 시 만료 발견)
- VALID → VALID (재검증 성공)
- VALID → VALID (재검증 실패 시 상태 유지)
- REVOKED: 모든 상태에서 REVOKED로 변경되지 않음 (단방향 보호)

### 3. 메타데이터 추출

등록 시 자동으로 추출되는 정보:
- **주체(Subject)**: CN, O, C 등
- **발급자(Issuer)**: CA 정보
- **일련번호(Serial Number)**: 16진수 표현
- **유효 기간**: notBefore / notAfter (ISO 8601 UTC)
- **지문(Fingerprint)**: SHA-256 해시 (식별용)
- **주요 용도(Key Usage)**: digitalSignature, keyEncipherment 등
- **대체 이름(Subject Alt Names)**: 도메인 목록

## 호환성

### 변경 사항 (Breaking Changes)
**없음** - 신규 기능 추가만 해당

### 이전 버전과의 호환성
**N/A** - 최초 출시 버전

## 알려진 제한사항

### Phase 1 미구현 항목

| 항목 | 사유 | 대상 버전 |
|-----|------|---------|
| **CRL/OCSP 폐기 실시간 조회** | 외부 네트워크 의존, 성능/타임아웃 관리 필요 | Phase 2 (Q3 2026) |
| **인증/인가 (Spring Security)** | 횡단 관심사, 별도 auth-layer feature로 분리 | auth-layer (Q2 2026) |
| **PEM 평문 저장 암호화** | 데이터베이스 암호화(TDE) 또는 AES-256 검토 필요 | Phase 2 (Q3 2026) |
| **자동 갱신** | 인증서 수명 관리 정책 미정 | Phase 3 (Q4 2026) |
| **HSM/Key Vault 연동** | 별도 인프라 요구 | Phase 3 (Q4 2026) |

### 폐기 검증 미지원
- **현재**: `revocationChecked = false` (검증 결과 항상 포함)
- **영향**: 이미 폐기된 인증서가 VALID로 판정될 수 있음
- **완화**: 폐기 검증이 필요한 환경은 별도 CRL/OCSP 서비스 도입 권장
- **개선**: Phase 2에서 활성화 시 타임아웃(기본값 5초) + 재시도 정책 추가

### 인증/인가 비활성화 (고의적 분리)
- **현재**: 모든 엔드포인트 미인증/미인가 상태
- **보안 위험**: 누구나 모든 인증서 조회/삭제 가능
- **의도**: 횡단 관심사(cross-cutting concern)로서 별도 feature 분리
- **분리 대상**: `auth-layer` feature (Spring Security + JWT/OAuth2)
- **배포 원칙**: auth-layer feature 배포 전까지 프로덕션 노출 금지
- **추적**: SECURITY-AUDIT.md 참고

## 마이그레이션 가이드

### Forward Migration (V1 적용)

#### 1단계: 데이터베이스 마이그레이션
```bash
# Flyway 자동 실행 (Spring Boot 시작 시)
# → V1__create_certificates.sql 적용

# 또는 수동 실행 (PostgreSQL)
psql -h <host> -U <user> -d <dbname> < src/main/resources/db/migration/V1__create_certificates.sql

# 결과: certificates 테이블 생성 (11 컬럼 + 2 인덱스)
```

#### 2단계: 애플리케이션 배포
```bash
# 이미지 빌드
./gradlew bootBuildImage

# 배포
kubectl apply -f deployment/next-cm.yaml -n production
```

#### 3단계: Canary 배포 (권장)
```bash
# 10% → 50% → 100% 단계적 롤아웃 (Helm / ArgoCD)
helm upgrade next-cm . --values values-prod.yaml --set canary.weight=10
# 1시간 모니터링 후
helm upgrade next-cm . --set canary.weight=50
# 1시간 모니터링 후
helm upgrade next-cm . --set canary.weight=100
```

#### 4단계: 스모크 테스트
```bash
# API 기본 동작 확인
curl -X POST https://api.example.com/api/1.0/certificates \
  -H "Content-Type: application/json" \
  -d '{
    "alias": "smoke-test",
    "pemContent": "..."
  }'
# Expected: 201 Created

# 목록 조회
curl https://api.example.com/api/1.0/certificates
# Expected: 200 OK, 배열 반환

# 단건 조회
curl https://api.example.com/api/1.0/certificates/{id}
# Expected: 200 OK

# 검증
curl -X POST https://api.example.com/api/1.0/certificates/{id}/validate
# Expected: 200 OK, valid=true/false
```

### Rollback Migration (이전 버전으로 복구)

#### 전제 조건
- P0 결함 발견 (인증서 파싱 오류, 상태 전이 버그 등)
- 신속한 복구 필요

#### 1단계: 애플리케이션 롤백
```bash
# 이전 버전 파드 재배포
kubectl set image deployment/next-cm \
  next-cm=<previous-image-sha> \
  -n production

# 또는 helm rollback
helm rollback next-cm 1 -n production
```

#### 2단계: 데이터베이스 롤백
```bash
# Flyway는 자동으로 이전 마이그레이션을 역추적하지 않음
# 수동 롤백 필요:

psql -h <host> -U <user> -d <dbname> << EOF
DROP TABLE IF EXISTS certificates;
EOF

# 또는 (데이터 보존 필요 시)
TRUNCATE TABLE certificates;
```

#### 3단계: 헬스 체크
```bash
curl https://api.example.com/health
# Expected: 200 OK
```

#### 4단계: 데이터 정제 (필요 시)
- certificates 테이블이 생성되었으나 이전 버전에서는 미인식
- 데이터가 존재하면 운영팀과 협의 후 삭제

### 롤백 주의사항

| 시나리오 | 조치 |
|--------|------|
| DB 마이그레이션 후 앱 배포 전 롤백 | DROP TABLE 안전 (데이터 없음) |
| 배포 후 1일 이내 롤백 | 데이터 백업 후 DROP TABLE |
| 배포 후 1주 이상 운영 중 롤백 | 데이터 마이그레이션 계획 필요 (별도 협의) |

## 배포 전 필수 확인사항

### 환경 요구사항
- JVM 21+ (Spring Boot 3.3+)
- PostgreSQL 13+ (또는 H2 테스트용)
- Gradle 8.5+

### 프로덕션 체크리스트
- [ ] DB 마이그레이션 백업 완료
- [ ] 롤백 계획 검증 (운영팀 승인)
- [ ] 모니터링 대시보드 설정 (아래 참조)
- [ ] 알람 임계치 설정 (아래 참조)
- [ ] auth-layer feature 로드맵 확인 (인증/인가 구현 일정)
- [ ] 보안 공지: API 미인증 상태 문서화 (내부 시스템만 허용)

### 모니터링 및 알람

#### 메트릭 수집 (Micrometer)
```
certificate.registration.count        # 등록 수
certificate.validation.count          # 검증 수
certificate.validation.failure.count  # 검증 실패 수
certificate.expiry.days               # 만료까지 남은 일수
certificate.pem.parse.error.count     # PEM 파싱 오류 수
```

#### 대시보드 (Prometheus / Grafana)
- **패널 1**: 시간별 인증서 등록 수 (과도한 등록 감지)
- **패널 2**: 검증 실패율 (체인 오류 추적)
- **패널 3**: 만료 임박 인증서 (30일, 7일, 1일 기준)
- **패널 4**: API 응답 시간 분포 (P50/P95/P99)

#### 알람 규칙 (예시)
| 조건 | 심각도 | 조치 |
|-----|--------|------|
| 검증 실패율 > 10% for 5m | WARNING | 로그 분석, CA 신뢰 앵커 확인 |
| 등록 수 > 1000/hour | WARNING | 대량 등록 원인 조사 |
| API 응답 시간 p99 > 1s | WARNING | 쿼리 성능 검토 |
| DB 커넥션 풀 고갈 > 80% | CRITICAL | 즉시 스케일 업 |

## 지원 및 피드백

### 버그 리포트
- 인증서 등록 오류: ECRT003 (PEM 파싱) 또는 ECRT002 (중복)
- 검증 오류: 상태 전이 버그, CA 체인 검증 실패
- API 오류: 404, 409, 400 에러 코드 + 메시지

### 기능 요청
- CRL/OCSP 폐기 검증 (Phase 2)
- 인증/인가 통합 (auth-layer)
- PEM 암호화 저장 (Phase 2)
- 자동 갱신 (Phase 3)

## 배포 후 모니터링

### 첫 24시간 (Critical)
- [ ] API 엔드포인트 가용성 (99.9% 이상)
- [ ] 에러율 (< 0.1%)
- [ ] 응답 시간 (p99 < 500ms)
- [ ] DB 커넥션 상태 (정상 범위)

### 첫 1주 (High)
- [ ] 누적 등록 인증서 수 (예상치 대비)
- [ ] 검증 성공/실패율
- [ ] 만료 임박 인증서 추적
- [ ] 보안 로그 (OWASP Top 10 공격 감지)

## 버전 히스토리

| 버전 | 출시일 | 주요 변경사항 |
|-----|--------|------------|
| v1.0.0 | 2026-04-16 | 초기 출시 (5개 API, 상태 관리, CA 검증) |

---

## 체크리스트

- [x] 신규 기능 5개 문서화
- [x] Breaking Changes 없음 확인
- [x] 알려진 제한사항 명시
- [x] Forward + Rollback 마이그레이션 가이드
- [x] 배포 전 체크리스트
- [x] 모니터링 및 알람 설정안
- [x] 지원 및 피드백 채널
