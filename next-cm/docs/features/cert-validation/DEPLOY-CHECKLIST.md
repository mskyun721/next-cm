---
agent: cicd
feature: cert-validation
status: approved
created: 2026-04-16
updated: 2026-04-16
links:
  prd: docs/features/cert-validation/PRD.md
  release: docs/features/cert-validation/RELEASE-NOTE.md
---

# DEPLOY-CHECKLIST: Certificate Manager (cert-validation)

배포 담당자가 따라야 할 체계적인 배포 절차입니다. 모든 항목을 순서대로 확인하고 승인 후 진행하세요.

---

## Phase 0: 사전 준비 (배포 1일 전)

### 0.1 환경 확인
- [ ] **JVM 버전**: `java -version` → OpenJDK 21+ 또는 Oracle JDK 21+
- [ ] **PostgreSQL 버전**: `psql --version` → 13+ 확인
  ```bash
  psql -h <prod-db-host> -U <user> -d <dbname> -c "SELECT version();"
  ```
- [ ] **Gradle**: `./gradlew --version` → 8.5+ 확인
- [ ] **메모리**: 프로덕션 JVM heap 최소 2GB 이상 할당 확인

### 0.2 DB 백업
- [ ] **Full 백업 실행**
  ```bash
  # PostgreSQL 전체 백업
  pg_dump -h <prod-db-host> -U <user> -d <dbname> > /backup/prod-$(date +%Y%m%d-%H%M%S).sql
  
  # 또는 자동화된 백업 확인
  ls -lh /backup/prod-* | tail -1
  ```
- [ ] **백업 무결성 확인**: 파일 크기 > 1MB (최소한의 데이터 보유 확인)
- [ ] **백업 위치 기록**: 운영 가이드에 저장

### 0.3 Monitoring & Alerting 준비
- [ ] **Prometheus 수집 대상 추가**: `scrapeDiskSize` 확인
- [ ] **Grafana 대시보드 구성**: 아래 4개 패널 설정
  - 시간별 인증서 등록 수
  - 검증 성공/실패율
  - API 응답 시간 분포 (P50/P95/P99)
  - DB 커넥션 풀 상태
- [ ] **Alertmanager 규칙 추가**: RELEASE-NOTE.md "알람 규칙" 참조
  ```yaml
  # prometheus.yml 에 추가
  - alert: CertificateValidationFailureRate
    expr: rate(certificate.validation.failure.count[5m]) > 0.1
    for: 5m
    annotations:
      summary: "인증서 검증 실패율 > 10%"
  ```
- [ ] **로그 수집 설정**: ELK / Datadog / CloudWatch
  ```
  로그 필터: certificate | validation | error
  보존 기간: 30일 (감사 목적)
  ```

### 0.4 운영팀 공지
- [ ] **배포 예정 공지**: Slack / 메일
  ```
  배포 대상: cert-validation feature
  배포 시간: 2026-04-16 14:00 UTC (약 30분 소요)
  예상 영향: 최소 (신규 기능, 기존 기능 변경 없음)
  롤백 계획: auth-layer 배포 전까지 보호 필요
  ```
- [ ] **점검 창(Maintenance Window) 예약**: (선택사항, 트래픽 낮은 시간)
- [ ] **on-call 엔지니어 대기**: 배포 중/후 즉시 대응 가능 상태

### 0.5 코드 품질 최종 검증
- [ ] **린트 확인**
  ```bash
  cd /Users/sk.mun/Project/next/next-cm/next-cm
  ./gradlew ktlintCheck detekt
  ```
  - ✅ PASS: 배포 진행
  - ❌ FAIL: 수정 후 재실행 (배포 연기)

- [ ] **테스트 실행**
  ```bash
  ./gradlew test
  ```
  - ✅ All tests PASS: 배포 진행
  - ⚠️ Known issues (REVIEW.md LOW-04 참조): 4/8 service tests 작동 예상, 허용
  - ❌ 예상 밖 실패: 조사 후 재시도

- [ ] **커버리지 확인**
  ```bash
  ./gradlew jacocoTestReport
  # build/reports/jacoco/test/html/index.html 확인
  # 목표: Domain 95%+, Service 60%+, 전체 65%+
  ```

---

## Phase 1: 배포 전 최종 점검 (배포 30분 전)

### 1.1 배포 아티팩트 준비
- [ ] **이미지 빌드**
  ```bash
  cd /Users/sk.mun/Project/next/next-cm/next-cm
  ./gradlew bootBuildImage --imageName=next-cm:v1.0.0
  ```
- [ ] **이미지 Tag 확인**
  ```bash
  docker images | grep next-cm
  # next-cm  v1.0.0  <image-id>  <created-time>
  ```
- [ ] **이미지 레지스트리 Push** (ECR / Docker Hub)
  ```bash
  docker push <registry>/next-cm:v1.0.0
  ```
- [ ] **이미지 검증**
  ```bash
  docker run --rm <registry>/next-cm:v1.0.0 --version
  ```

### 1.2 배포 설정 파일 준비
- [ ] **Kubernetes 매니페스트 확인**
  ```yaml
  # deployment/next-cm-prod.yaml
  spec:
    replicas: 3  # 최소 3개 (높은 가용성)
    strategy:
      type: RollingUpdate
      rollingUpdate:
        maxSurge: 1
        maxUnavailable: 0
    template:
      spec:
        containers:
        - name: next-cm
          image: <registry>/next-cm:v1.0.0
          env:
          - name: SPRING_DATASOURCE_URL
            value: jdbc:postgresql://<prod-db-host>:5432/<dbname>
          - name: SPRING_JPA_HIBERNATE_DDL_AUTO
            value: validate  # 마이그레이션은 Flyway로
          - name: SERVER_PORT
            value: "8080"
          - name: JAVA_OPTS
            value: "-Xms2g -Xmx4g"  # Heap 설정
  ```
- [ ] **Helm values 확인** (선택사항)
  ```bash
  helm template next-cm . -f values-prod.yaml | kubectl apply --dry-run=client -f -
  # 문법 오류 없음 확인
  ```

### 1.3 DB 마이그레이션 검증
- [ ] **마이그레이션 스크립트 존재 확인**
  ```bash
  ls -la /Users/sk.mun/Project/next/next-cm/next-cm/src/main/resources/db/migration/
  # V1__create_certificates.sql 존재 확인
  ```
- [ ] **마이그레이션 내용 리뷰**
  ```sql
  -- 확인 항목:
  -- 1. CREATE TABLE certificates (필수)
  -- 2. 11개 컬럼 포함 (id, alias, subject, issuer, ..., updated_at)
  -- 3. UNIQUE 제약 (fingerprint)
  -- 4. 2개 인덱스 (status, not_after)
  ```
- [ ] **Flyway 자동 실행 확인**
  - Spring Boot 시작 시 자동 실행됨
  - 또는 수동 실행: `./gradlew flywayMigrate`

### 1.4 롤백 계획 최종 확인
- [ ] **롤백 절차 테스트** (선택사항, 미리 한 번 실행)
  ```bash
  # 현재 배포 버전 기록
  kubectl get deployment next-cm -o jsonpath='{.spec.template.spec.containers[0].image}' -n production
  
  # rollback 명령 미리 준비
  kubectl rollout undo deployment/next-cm -n production
  ```
- [ ] **DB 롤백 스크립트 준비** (만약의 경우)
  ```sql
  -- 재해 복구용 (필요시에만 실행)
  DROP TABLE IF EXISTS certificates;
  -- 이전 버전에서 미인식이므로 애플리케이션은 자동 작동
  ```

### 1.5 통신 확인
- [ ] **운영팀 최종 공지**: "지금부터 배포 시작"
- [ ] **on-call 엔지니어 대기**: 메시지 확인
- [ ] **배포 시작 시간 기록**: 로그 / 문서

---

## Phase 2: 배포 실행 (배포 당일)

### 2.1 Canary 배포 (10% 트래픽)
- [ ] **Canary 파드 배포**
  ```bash
  # Option 1: Helm (권장)
  helm upgrade next-cm . \
    --values values-prod.yaml \
    --set canary.enabled=true \
    --set canary.weight=10 \
    -n production
  
  # Option 2: kubectl
  kubectl set image deployment/next-cm \
    next-cm=<registry>/next-cm:v1.0.0 \
    -n production
  kubectl scale deployment next-cm --replicas=1 -n production
  ```
- [ ] **파드 상태 확인**
  ```bash
  kubectl get pods -n production | grep next-cm
  # STATUS: Running, READY: 1/1
  ```
- [ ] **헬스체크 (Canary)**
  ```bash
  kubectl exec -it <canary-pod-name> -n production -- \
    curl -s http://localhost:8080/actuator/health | jq .
  # Expected: { "status": "UP" }
  ```

### 2.2 Canary 검증 (5-10분)
- [ ] **API 테스트 (Canary 파드만)**
  ```bash
  # 인증서 등록
  curl -X POST http://<canary-service>/api/1.0/certificates \
    -H "Content-Type: application/json" \
    -d '{
      "alias": "canary-test",
      "pemContent": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----"
    }'
  # Expected: 201 Created, id 포함
  
  # 목록 조회
  curl http://<canary-service>/api/1.0/certificates
  # Expected: 200 OK, 배열
  
  # 삭제
  curl -X DELETE http://<canary-service>/api/1.0/certificates/{id}
  # Expected: 204 No Content
  ```
- [ ] **로그 확인**
  ```bash
  kubectl logs -f <canary-pod-name> -n production | grep -E "certificate|error|warn"
  # 이상 로그 없음 확인
  ```
- [ ] **메트릭 수집 확인** (Prometheus)
  ```
  certificate.registration.count (> 0 expected)
  certificate.validation.count (> 0 expected)
  http_requests_total{endpoint="/api/1.0/certificates"}
  ```
- [ ] **에러율 확인**
  ```
  에러율 < 1% (정상)
  응답 시간 P99 < 500ms (정상)
  ```
- [ ] **DB 커넥션 풀 상태**
  ```bash
  # Hikari 메트릭 확인
  hikaricp.connections.active (< 10 expected)
  hikaricp.connections.idle (> 0 expected)
  ```

### 2.3 Canary → 50% (1시간 모니터링 후)
- [ ] **모니터링 결과 평가**
  - ✅ 에러 없음, 응답 정상: 다음 단계 진행
  - ⚠️ 미미한 에러 (< 0.1%): 계속 모니터링, 30분 후 재평가
  - ❌ 심각한 에러 (> 1%): 즉시 롤백 (섹션 3 참조)

- [ ] **50% 배포 실행**
  ```bash
  helm upgrade next-cm . \
    --values values-prod.yaml \
    --set canary.weight=50 \
    -n production
  # 또는 replicas 증가: kubectl scale deployment next-cm --replicas=2
  ```

- [ ] **50% 검증** (5-10분)
  - 위 2.2 항목과 동일 (API 테스트, 로그, 메트릭)

### 2.4 50% → 100% (1시간 모니터링 후)
- [ ] **모니터링 결과 평가**
  - 위 2.3과 동일 기준

- [ ] **100% 배포 실행**
  ```bash
  helm upgrade next-cm . \
    --values values-prod.yaml \
    --set canary.weight=100 \
    -n production
  # 또는 full rollout: kubectl rollout status deployment/next-cm -n production
  ```

- [ ] **전체 파드 상태 확인**
  ```bash
  kubectl get pods -n production | grep next-cm
  # 3개 파드 모두 Running, Ready 1/1
  ```

### 2.5 배포 후 최종 검증 (5-10분)
- [ ] **프로덕션 엔드포인트 테스트**
  ```bash
  # 현재 프로덕션 주소로 테스트 (위 2.2 API 테스트 반복)
  curl https://api.example.com/api/1.0/certificates
  # Expected: 200 OK
  ```

- [ ] **전체 메트릭 확인**
  ```
  http_requests_total{status=~"2.."}  # 성공
  http_requests_total{status=~"4.."}  # 클라이언트 오류 (정상)
  http_requests_total{status=~"5.."}  # 서버 오류 (0 이어야 함)
  ```

- [ ] **DB 커넥션 풀 상태** (안정화)
  ```
  hikaricp.connections.active < 20
  hikaricp.connections.idle > 0
  ```

- [ ] **배포 기록 작성**
  ```
  배포 완료 시간: 2026-04-16 14:30 UTC
  배포 버전: v1.0.0
  배포 방식: Canary (10% → 50% → 100%)
  문제 사항: 없음
  ```

---

## Phase 3: 배포 후 모니터링 (배포 후 24시간)

### 3.1 첫 1시간 (Critical)
- [ ] **API 가용성**: 99.9% 이상
- [ ] **에러율**: < 0.1%
- [ ] **응답 시간**: P99 < 500ms
- [ ] **DB 커넥션**: 안정 범위 내
- [ ] **로그**: 심각한 오류 없음

### 3.2 첫 24시간 (High Priority)
- [ ] **누적 인증서 등록 수**: 예상치와 비교 (과도한 등록 감지)
- [ ] **검증 성공/실패율**: 일반적 비율 범위 (>80% 성공 예상)
- [ ] **API 응답 분포**: P50/P95/P99 추이 (급격한 악화 감시)
- [ ] **만료 임박 인증서**: 모니터링 대시보드 활성화
- [ ] **보안 로그**: OWASP 공격 패턴 감시 (auth-layer 배포 전까지)

### 3.3 첫 1주 (Ongoing)
- [ ] **누적 성능 데이터 수집**
  - Percentile 분석 (P50/P95/P99)
  - 피크 시간대 부하 분석
  - 캐시 히트율 (향후 최적화용)

- [ ] **비즈니스 메트릭**
  - 일일 등록 건수
  - 검증 요청 건수
  - 에러 패턴 분석

- [ ] **운영팀 피드백**
  - 신규 기능 만족도
  - 개선 건의사항

---

## Phase 4: 문제 발생 시 대응 (Troubleshooting)

### 4.1 배포 중 문제 감지

#### 증상: API 응답 5xx 에러 급증
```bash
# 1. 로그 확인
kubectl logs -f <pod-name> -n production | grep -i error

# 2. 파드 상태 확인
kubectl describe pod <pod-name> -n production
# Events 섹션에서 문제 확인

# 3. DB 연결 확인
# 위 포드에서:
curl -s http://localhost:8080/actuator/health/db | jq .

# 4. 롤백 실행 (4.2 참조)
```

#### 증상: DB 마이그레이션 실패
```
Error: Flyway migration failed: V1__create_certificates.sql

원인: 테이블이 이미 존재하거나 스키마 충돌

해결:
1. Flyway metadata 확인
   SELECT * FROM flyway_schema_history;
   
2. 기존 certificates 테이블 확인
   \dt certificates;
   
3. 필요시 정리 (운영팀 승인 필수)
   DROP TABLE IF EXISTS certificates;
   TRUNCATE TABLE flyway_schema_history;
```

#### 증상: 높은 CPU/메모리 사용률
```bash
# 1. 파드 리소스 확인
kubectl top pod <pod-name> -n production

# 2. 메모리 누수 의심
kubectl exec <pod-name> -n production -- jmap -heap <pid>

# 3. 필요시 수동 GC 트리거
kubectl exec <pod-name> -n production -- \
  jcmd <pid> GC.run
```

### 4.2 롤백 절차 (비상 상황)

#### 조건: P0 결함 발견
- 인증서 파싱 오류로 API 503 (> 10% 에러율)
- 상태 전이 버그로 데이터 손상
- DB 마이그레이션 실패로 서비스 불가능

#### 롤백 실행
```bash
# Step 1: 애플리케이션 롤백
kubectl rollout undo deployment/next-cm -n production
# 또는 직접:
kubectl set image deployment/next-cm \
  next-cm=<previous-image-sha> \
  -n production

# Step 2: 상태 확인
kubectl rollout status deployment/next-cm -n production

# Step 3: 헬스 체크
curl https://api.example.com/health

# Step 4: DB 롤백 (필요시)
psql -h <prod-db-host> -U <user> -d <dbname> << EOF
DROP TABLE IF EXISTS certificates;
EOF

# Step 5: 재배포 시작
```

#### 롤백 후 조치
- [ ] 문제 원인 분석 (포스트모템)
- [ ] 테스트 케이스 추가
- [ ] 리뷰 + 보안 감사
- [ ] 재배포 일정 수립

---

## Phase 5: 배포 후 관리 (지속)

### 5.1 인증/인가 (고의적 미구현)

#### 현재 상태
- ⚠️ 모든 엔드포인트 미인증/미인가
- ⚠️ 누구나 모든 인증서 조회/삭제 가능

#### 보안 원칙
```
⚠️ auth-layer feature 배포 전까지 프로덕션 노출 금지

배포 가능한 환경:
- 내부 시스템만 접근 가능한 VPC
- API Gateway 인증 레이어 통과 필수
- 네트워크 격리 (Private Subnet)

배포 불가능한 환경:
- 공개 인터넷 노출
- 게스트 사용자 접근 가능
```

#### 완화 조치 (auth-layer 배포 전까지)
1. **네트워크 격리**
   ```yaml
   # Kubernetes NetworkPolicy
   apiVersion: networking.k8s.io/v1
   kind: NetworkPolicy
   metadata:
     name: next-cm-deny-external
     namespace: production
   spec:
     podSelector:
       matchLabels:
         app: next-cm
     policyTypes:
     - Ingress
     ingress:
     - from:
       - namespaceSelector:
           matchLabels:
             name: trusted-services
   ```

2. **API Gateway 인증**
   ```
   모든 요청이 API Gateway를 경유하도록 설정
   API Gateway에서 JWT/OAuth2 검증
   인증된 요청만 next-cm 서비스로 포워드
   ```

3. **감시**
   ```
   로그 분석:
   - 비정상적인 대량 요청 감시
   - 대량 삭제 요청 감시
   - 의도하지 않은 IP에서의 접근 로깅
   ```

#### 마일스톤
- [ ] auth-layer feature 개발 시작: 2026-Q2
- [ ] auth-layer feature 배포: 2026-Q2 말
- [ ] cert-validation 인증/인가 통합: 배포 후

### 5.2 Performance Tuning (Phase 2)

#### 목표
- API 응답 시간 P99 < 200ms (현재 500ms)
- 등록 처리량 > 10,000/sec

#### 대상 최적화
| 항목 | 최적화 방안 | 타겟 버전 |
|-----|----------|--------|
| PEM 파싱 | JCA 캐싱 또는 네이티브 컴파일 | v1.1 |
| CA 체인 검증 | 신뢰 앵커 캐싱 | v1.1 |
| DB 쿼리 | 인덱스 추가 (alias, status 복합) | v1.1 |
| 캐싱 | Redis (fingerprint → id 매핑) | v1.2 |

### 5.3 CRL/OCSP 준비 (Phase 2)

#### 현재 상태
```
revocationChecked = false  # 폐기 검증 미실행
```

#### Phase 2 계획
1. **CRL 검증 추가**
   - CDP (CRL Distribution Point) 자동 다운로드
   - 타임아웃 5초, 재시도 2회

2. **OCSP 검증 추가**
   - OCSP responder 호출
   - 타임아웃 3초, 캐싱 24시간

3. **성능 영향 분석**
   - 검증 시간 증가 (예상 100-500ms)
   - 네트워크 I/O 증가
   - 캐싱 전략 필수

---

## Phase 6: 배포 문서화

### 6.1 배포 후 리포트 작성

```markdown
## 배포 리포트 (cert-validation v1.0.0)

### 배포 기본 정보
- 배포 날짜: 2026-04-16
- 배포 버전: v1.0.0
- 배포 담당자: [이름]
- 배포 시간: 14:00 - 14:45 UTC (45분)

### 배포 결과
- 배포 상태: SUCCESS
- 에러율: 0%
- 응답 시간 P99: 125ms
- DB 마이그레이션: 성공 (V1 적용)

### 모니터링 결과
- 첫 1시간 가용성: 99.99%
- 첫 24시간 에러율: 0.02% (정상)
- 누적 인증서 등록: 2,345개

### 문제 및 해결
- 문제 없음

### 다음 단계
- auth-layer feature 개발 진행
- 1주일 후 성능 최적화 검토
```

### 6.2 Runbook 업데이트
- [ ] cert-validation API 엔드포인트 문서화
- [ ] 일반적인 오류 및 해결책 추가
- [ ] 모니터링 대시보드 URL 기록
- [ ] on-call 가이드 업데이트

---

## 최종 체크리스트

### 배포 전
- [ ] 환경 확인 (JVM, DB, Gradle)
- [ ] DB 백업 완료
- [ ] 모니터링 준비
- [ ] 운영팀 공지
- [ ] 코드 품질 검증 (lint, test, coverage)

### 배포 중
- [ ] Canary 배포 (10%)
- [ ] 5-10분 검증
- [ ] 50% 배포
- [ ] 1시간 모니터링
- [ ] 100% 배포
- [ ] 최종 검증

### 배포 후
- [ ] 24시간 모니터링
- [ ] 문제 없음 확인
- [ ] 배포 리포트 작성
- [ ] Runbook 업데이트

### 특별 주의사항
- ⚠️ **auth-layer 배포 전까지 프로덕션 노출 금지**
  - 네트워크 격리 또는 API Gateway 인증 필수
  - 운영팀에 공지
  - 보안 경고 문서화

- ⚠️ **rolback 계획 항상 준비**
  - 자동 롤백 스크립트 테스트
  - 팀원에 절차 공유

---

## 지원 연락처

| 역할 | 담당자 | 연락처 |
|-----|--------|--------|
| 배포 담당 | [담당자] | [연락처] |
| 운영팀 | [담당자] | [연락처] |
| on-call 엔지니어 | [담당자] | [연락처] |
| Security | [담당자] | [연락처] |

---

## 문서 히스토리

| 버전 | 작성자 | 작성일 | 변경사항 |
|-----|--------|--------|--------|
| v1.0 | CICD Agent | 2026-04-16 | 초기 배포 체크리스트 |
