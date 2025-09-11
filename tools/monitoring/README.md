# 📊 Monitoring (Loki + Grafana)

---
## 🎯 목적
- 로그를 중앙 수집(Loki)하고 Grafana로 빠르게 조회/시각화합니다.

---
## ▶️ 실행
```bash
cd tools/monitoring
docker compose up -d
```
- Grafana: `http://localhost:3000` (Anonymous Viewer 허용)
- Loki: `http://localhost:3100`

---
## 🔌 데이터 소스
- Grafana가 자동으로 Loki 데이터소스(`http://loki:3100`)를 추가합니다.

---
## 📝 참고
- 애플리케이션 로그를 Loki로 보내려면 Promtail 또는 로그 드라이버 설정이 필요합니다.
- 현재 구성은 Loki/Grafana만 제공합니다.

