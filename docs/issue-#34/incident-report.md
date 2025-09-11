# 🚨 Incident Response Report (Performance Testing)

## 🧭 Overview
- Incident ID: PT-2025-09-12-01
- Date/Time: 2025-09-12
- Impacted Components: `API`(orders, coupons), `DB`(orders/order_items), `Kafka`
- Severity: S2 (부분 가용성 저하 및 쓰기 실패율 증가)
- Status: Resolved(원인 가설 정리 및 조치 계획 수립)

## 🔎 Summary
> 부하 시나리오(stress/spike/contention/pareto)에서 `POST /orders`, `POST /coupons/{id}` 쓰기 요청 실패율 급증.
> `i/o timeout` 로그 다수 → 서버 응답 지연/연결 대기 초과 추정. 읽기 엔드포인트는 p95 < ~160ms로 상대적 안정.
> 감지: k6 ErrorRate, p95, 실행 로그.

## 🕒 Timeline
- T0: 감지(알람/대시보드/k6 실패 임계)
- T+N: 1차 분류(네트워크/애플리케이션/DB/Kafka)
- T+N: 원인 가설 수립 및 검증
- T+N: 완화/해결 조치 적용
- T+N: 모니터링으로 정상화 확인

## 📈 Evidence (k6 / Logs)
- baseline.short: RPS 5.82, ErrorRate 17.37%, p95 80.58ms
- stress.short: RPS 26.17, ErrorRate 40.40%, p95 153.52ms
- spike.short: RPS 27.97, ErrorRate 20.81%, p95 92.29ms
- soak.short: RPS 4.82, ErrorRate 29.13%, p95 84.77ms
- contention.short: RPS 21.57, ErrorRate 100.00%, p95 100.19ms
- popular_pareto.short: RPS 9.67, ErrorRate 56.71%, p95 70.22ms
- k6 로그: 다수의 `dial: i/o timeout` 메시지 (host.docker.internal:8080 대상)

## 🧩 Root Cause Analysis (RCA)
- Technical Root Cause: 동시 쓰기 경합(주문/쿠폰)에서의 자원 잠금·트랜잭션 경합, 연결 대기 초과로 인한 요청 타임아웃.
- Contributing Factors: 제한된 커넥션 풀, DB 인덱스/락 경합, Docker 네트워크 오버헤드(host.docker.internal), 소비자(Kafka) 처리 지연 가능성.
- Why-Why(5 Whys): 쓰기 실패율↑ → 응답 지연/타임아웃 → DB 잠금·풀 고갈/큐 적체 → 동시성 제어/인덱스 부족 → 설계/설정 미조정.

## 🛠 Mitigation & Fix
- Immediate Mitigation:
  - API 타임아웃·커넥션 풀 상향(소폭), 재시도 정책(일부 5xx/타임아웃), k6 쓰기 비율 단계적 증가로 점검.
- Permanent Fix(안):
  - 주문/쿠폰 경합 구간 낙관적 잠금·버전 컬럼 사용, 트랜잭션 범위 단축 및 인덱스 점검.
  - 인기상품 조회 캐시/HOT 키 완화(파레토 트래픽 흡수), 큐 기반 비동기 처리 검토.
- Config/Infra 변경사항:
  - DB 커넥션 풀/타임아웃/스레드풀 파라미터 튜닝, Docker→로컬直 호출 또는 네트워크 최적화.

## ✅ Verification
- 재현 테스트: baseline.short(60s, VU10), stress.short(60s 단계), contention.short(60s)
- 기대: p95 200ms 이내 유지, ErrorRate < 5% (쓰기 경합 완화 후)
- Before/After 비교: p95/에러율/성공율 표로 첨부(k6 JSON 기반)

## 🔁 Follow-ups
- 모니터링 룰/알람 추가: ErrorRate>5%, p95>400ms 5분 지속 시 알람.
- 성능 회귀 테스트 자동화: CI에서 smoke/baseline.short + threshold 게이트.
- 문서/Runbook 업데이트: k6 실행 스크립트/대응 절차(롤백·스케일업) 문서화.

## 📎 Appendix
- 관련 PR/Commit:
- k6 Summary JSON 링크:
- 참고 로그/대시보드:
