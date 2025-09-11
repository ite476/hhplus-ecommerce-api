# 🚀 부하 테스트 시나리오 계획서

## 🎯 1. 목적과 범위
- **목적**: 핵심 API의 처리량과 지연시간 목표를 수립하고, 병목 지점을 식별·개선하기 위한 근거 데이터를 획득한다.
- **범위**: 아래 엔드포인트 기반의 Read/Write 혼합 트래픽 및 경합 상황을 포함한다.
  - Product
    - GET `/api/v1/products`
    - GET `/api/v1/products/popular`
  - Order
    - POST `/api/v1/orders` (헤더 `userId` 필요)
  - Point
    - PATCH `/api/v1/point/charge` (헤더 `userId` 필요)
    - PATCH `/api/v1/point/use` (헤더 `userId` 필요)
    - GET `/api/v1/point` (헤더 `userId` 필요)
  - Coupon
    - GET `/api/v1/mycoupons` (헤더 `userId` 필요)
    - POST `/api/v1/coupons/{couponId}` (헤더 `userId` 필요)

---
## 🧰 2. 도구 스택 및 선정 기준
- **Load Generator: k6**
  - 선정 이유: 코드 기반(JS) 시나리오 구성, 파라미터화와 모듈화 용이, 낮은 러닝 커브, CI 연동과 요약 리포트/Threshold 내장.
  - 기대효과: 재현 가능한 테스트, 시나리오별 독립 실행과 결과 비교 용이, 유지보수성 향상.
- **Observability: Loki + Grafana (+ Promtail)**
  - 선정 이유: 애플리케이션/인프라 로그의 중앙 수집·검색·시각화. LogQL로 에러/지연 패턴 탐색.
  - 구성: 애플리케이션 로그 → Promtail → Loki, Grafana로 대시보드/Explore.
- (선택) **Metrics 확장**
  - 필요 시, Spring Boot Actuator + Micrometer + Prometheus를 추가해 JVM/HTTP/DB 풀 지표를 계량화한다. 본 계획서는 로그 중심으로 진행하되, 병목 식별 정량성이 부족할 경우 지표 수집을 확장한다.

---
## ⚙️ 3. 테스트 환경 및 변수
- **BASE_URL**: 실행 환경에 따라 환경변수로 주입한다.
  - 예시: 로컬 `http://localhost:8080`
  - 원격 배포 시나리오 대비를 위해 `BASE_URL`만 교체하여 동일 스크립트를 재사용.
- **k6 실행 공통 옵션(예시)**
  - `k6 run -e BASE_URL=http://localhost:8080 path/to/scenario.js`
  - 결과 저장: `--summary-export=./results/<scenario>-<timestamp>.json`
- **로그 파이프라인**: Promtail가 애플리케이션 로그를 Loki로 ship 중이라는 전제(미구성 시 추가 구성 필요).

---
## 📈 4. KPI / SLO (초안, 시나리오별로 조정 가능)
- **지연시간**: p50, p90, p95, p99
- **에러율**: HTTP 5xx/4xx(의도치 않은 실패) 비율
- **처리량**: RPS, VU(가상 사용자) 기준
- **안정성/포화 신호**: 재시도 증가, 타임아웃, (선택) 풀 고갈/GC 급증 등 로그 패턴
- **SLO 가이드**
  - Read(목록/조회): p95 < 200 ms, 에러율 < 0.1%
  - Write(주문/포인트/쿠폰): p95 < 500 ms, 에러율 < 0.5%

---
## 🔀 5. 트래픽 믹스와 데이터 전략
- **트래픽 믹스(기본안)**
  - Product: 50–60% (목록/인기, 페이징 랜덤)
  - Order: 15–25% (아이템 수 1–3 랜덤, 쿠폰 적용 10–20% 확률)
  - Point: 10–15% (`GET` 잔액 확인, `charge`/`use` 1:1 비율)
  - Coupon: 5–10% (`GET`/`POST` 혼합)
- **데이터 전략**
  - `userId`: 합리적 분포(예: 1..10,000)에서 균등 혹은 가중 랜덤
  - `productId`/`couponId`: 샘플 풀을 준비하고 랜덤 선택
  - 페이징: `page`, `size`를 합리 범위에서 랜덤
  - 실패가 비정상인지/정상 경쟁 실패인지(예: 중복 발급/재고 부족)를 구분 라벨링
 - **데이터셋 가정**
   - 시드 데이터는 `docs/issue-#18`의 스크립트를 사용한다.
     - 사용자: `users.bulk-insert.sql` (1..10,000)
     - 상품: `products.bulk-insert.sql` (1..100,000)
     - 쿠폰: `coupon.insert.sql` (10종)
     - 주문/아이템: 경량판 `orders.bulk-insert.small.sql` 기준으로 로컬 구성
   - 대용량 `orders.bulk-insert.sql`은 로컬 환경에서는 사용하지 않으며, 필요 시 별도 실행한다.

---
## 📚 6. 시나리오 정의
각 시나리오는 “독립 실행”을 원칙으로 하여 결과 비교·추적을 용이하게 한다. 필요 시 혼합(Mix) 시나리오를 별도로 추가해 총합 관점의 성능/안정성을 확인한다.

1) Smoke
- 목적: 엔드포인트/데이터/인증 헤더 등 기본 동작 검증
- 부하: 1–5 RPS, 2–3분
- 판정: 에러율 0%, p95 기준 목표 내

2) Baseline
- 목적: 기준 성능 확보, p95/처리량 기준선
- 부하: 점증 Ramp(예: 5→50 RPS), 10–15분, 위 트래픽 믹스 적용
- 판정: SLO 충족, 에러율 안정

3) Stress
- 목적: 임계치 탐색 및 붕괴 지점 식별
- 부하: 50→300 RPS 이상의 단계 증가, 각 단계 3–5분 유지
- 판정: 지연/에러 급증 지점 기록, 로그로 실패 원인 유형 파악

4) Spike
- 목적: 단기 급증 트래픽 대응력
- 부하: 즉시 3–5배 트래픽 2–5분, 이후 정상 복귀 5분
- 판정: 에러율/지연 급증 후 빠른 회복 여부

5) Soak
- 목적: 장시간 안정성(리소스 누수/지연 악화) 확인
- 부하: 현실적 평균 부하로 1–2시간 유지
- 판정: 시간 경과에 따른 에러율/지연/자원 사용 악화 없음

6) Contention (경합 시나리오)
- 목적: 동시성/일관성/락 경합 검증
- 케이스: 동일 상품 다중 주문(재고/락), 동일 쿠폰 대량 발급, 동일 사용자 포인트 `charge/use` 경쟁
- 판정: 중복/경합 실패가 정상 범주인지 구분, 비정상 실패(무결성 위반, 예외 로그) 최소화

---
## ▶️ 7. 실행 및 결과 수집
- k6 실행: 시나리오별 스크립트를 개별 실행하고 요약을 JSON으로 저장한다.
  - 예시: `k6 run -e BASE_URL=http://localhost:8080 tools/perf/k6/scenarios/baseline.js --summary-export=./results/baseline-<timestamp>.json`
  - Pareto 시나리오 예시: `k6 run -e BASE_URL=http://localhost:8080 tools/perf/k6/scenarios/popular_pareto.js --summary-export=./results/pareto-<timestamp>.json`
- Loki: 에러 로그, 느린 처리(경고/에러 레벨, 지연 키워드) 탐지용 쿼리 예시를 운영 노트에 기록한다.
- Grafana: Explore로 시나리오 기간을 지정해 에러/응답시간 관련 로그 패턴을 캡처한다(스크린샷 첨부).

---
## 🔁 8. 결과 판정 및 개선 루프
- SLO 충족 여부와 여유치(Headroom)를 기록한다.
- 실패/지연 급증 구간에 대해 로그 상관관계를 분석하고, 개선 Hypothesis → 개선 → 재측정 사이클을 반복한다.
- 개선안 예시: 데이터베이스 인덱스/쿼리 튜닝, 동시성 제어(락 범위/시간 단축), 캐시/큐 도입, 스레드/풀 사이즈 조정 등.

---
## 🗂️ 9. 산출물 및 저장 위치
- 본 문서: `docs/issue-#34/load-test-scenarios.md`
- 테스트 스크립트: `tools/perf/k6/scenarios/*.js` (별도 커밋 예정)
- 실행 결과: `tools/perf/k6/results/*.json` (시나리오별 타임스탬프 포함)
- 로그/대시보드 캡처: `docs/issue-#34/artifacts/` 하위에 이미지/쿼리 스니펫 정리

---
## ⚠️ 10. 리스크와 주의사항
- 외부 시스템 의존 시나리오가 포함될 경우, 실제 비용/쿼터 영향 주의.
- 데이터 정합성: 경합 테스트 후 데이터 롤백/정리 절차 필요.
- 재사용성: 모든 시나리오는 `BASE_URL`만 교체해 재사용 가능하도록 설계한다.


