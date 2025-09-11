# 🧪 k6 - Load Testing

---
## 🎯 목적
- 코드 기반(k6)으로 부하 테스트 시나리오를 형상 관리하고, 환경변수로 손쉽게 로컬/원격을 전환할 수 있도록 합니다.

---
## 🗂️ 구조
- `scenarios/`: 시나리오 스크립트 (`smoke.js` 등)
  - `smoke.js`: 기본 동작 검증
  - `popular_pareto.js`: 인기 상품 편중(Pareto) 분포 시나리오
- `results/`: 실행 결과(JSON 요약)
- `run.ps1`, `run.sh`: 표준 실행 스크립트(Windows/Linux, macOS)

---
## ⚙️ 환경변수
- `BASE_URL` (기본: `http://localhost:8080`)
- `VU` (기본: `5`)
- `DURATION` (기본: `2m`)
- `INFLUX_URL` (선택: `http://localhost:8086/k6` → `--out influxdb=...`)

---
## ▶️ 실행 예시
PowerShell(Windows):
```powershell
./run.ps1 -Scenario smoke -BaseUrl http://localhost:8080 -Vu 5 -Duration 2m
./run.ps1 -Scenario baseline -BaseUrl http://localhost:8080
./run.ps1 -Scenario stress
./run.ps1 -Scenario spike
./run.ps1 -Scenario soak -Duration 30m
./run.ps1 -Scenario contention -Duration 5m -Vu 30
```

Bash(Linux/macOS):
```bash
BASE_URL=http://localhost:8080 VU=5 DURATION=2m ./run.sh smoke
INFLUX_URL=http://localhost:8086/k6 ./run.sh baseline
./run.sh baseline
./run.sh stress
./run.sh spike
DURATION=30m ./run.sh soak
VU=30 DURATION=5m ./run.sh contention
```

---
## 🧾 참고
- 데이터셋 가정: `docs/issue-#18` 시드 스크립트 사용(로컬은 `orders.bulk-insert.small.sql` 기준)
- 시나리오 확장 시 `scenarios/`에 추가하고, 공통 상수/함수는 재사용을 권장합니다.


