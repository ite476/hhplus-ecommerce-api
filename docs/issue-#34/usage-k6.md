# ▶️ k6 실행 가이드

---
## 🎯 목적
- 로컬/원격 환경에서 동일 스크립트로 부하 테스트를 실행하기 위한 표준 절차를 제공합니다.

---
## ⚙️ 사전 준비
- 서버 실행: BASE_URL 기본값은 `http://localhost:8080`
- 데이터셋: `docs/issue-#18` 시드 적용(로컬은 `orders.bulk-insert.small.sql` 권장)
- Kafka 컨슈머: 활성화 상태 권장
- k6 설치: 로컬에 k6 설치 필요

---
## 🗂️ 스크립트 위치
- `tools/perf/k6/scenarios/*.js`
- 실행 스크립트: `tools/perf/k6/run.ps1`, `tools/perf/k6/run.sh`

---
## ▶️ 실행 예시
Windows PowerShell:
```powershell
./tools/perf/k6/run.ps1 -Scenario smoke -BaseUrl http://localhost:8080 -Vu 5 -Duration 2m
./tools/perf/k6/run.ps1 -Scenario baseline
./tools/perf/k6/run.ps1 -Scenario stress
./tools/perf/k6/run.ps1 -Scenario spike
./tools/perf/k6/run.ps1 -Scenario soak -Duration 30m
./tools/perf/k6/run.ps1 -Scenario contention -Duration 5m -Vu 30
./tools/perf/k6/run.ps1 -Scenario popular_pareto
```

Bash (Linux/macOS):
```bash
BASE_URL=http://localhost:8080 VU=5 DURATION=2m ./tools/perf/k6/run.sh smoke
./tools/perf/k6/run.sh baseline
./tools/perf/k6/run.sh stress
./tools/perf/k6/run.sh spike
DURATION=30m ./tools/perf/k6/run.sh soak
VU=30 DURATION=5m ./tools/perf/k6/run.sh contention
./tools/perf/k6/run.sh popular_pareto
```

---
## 🧾 결과 저장
- 요약 JSON: `tools/perf/k6/results/<scenario>-<timestamp>.json`
- 필요 시 파일명 접두사/출력 경로는 스크립트에서 조정 가능

---
## 📝 참고
- BASE_URL/DURATION/VU는 환경변수 또는 실행 인자로 주입 가능
- Pareto 시나리오는 인기 상품 편중 트래픽을 모사
- Contention 시나리오는 동일 자원 경합(상품/쿠폰/사용자 포인트)을 모사

