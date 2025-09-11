## Docker Compose Conventions

이 프로젝트는 서비스별 Compose 프래그먼트를 분리하여 관리합니다. 루트 `docker-compose.yml`은 필요한 프래그먼트를 `extends`로 가져와 한 번에 기동합니다.

### Naming & Location
- 파일명: `docker/compose.<name>.yml`
  - 예: `docker/compose.mysql.yml`, `docker/compose.redis.yml`, `docker/compose.kafka.yml`, `docker/compose.kafka-ui.yml`
- 원칙: 가능한 한 프래그먼트당 1 서비스. 스택 단위가 필요하면 의미있는 `<name>` 사용(예: `compose.observability.yml`).

### Authoring Guidelines
- 포트/볼륨 충돌을 피하십시오. 데이터 볼륨은 `${COMPOSE_PROJECT_DIR}/data/<service>`로 통일합니다.
- 헬스체크를 권장합니다(의존 서비스의 `depends_on.condition`에 활용 가능).
- 민감정보는 환경변수/개발용 값만 포함하고, 비밀키는 커밋하지 않습니다.
- 태그는 명시적으로 고정합니다(예: `mysql:8.0`, `redis:7-alpine`, `bitnami/kafka:4.0.0`).

### Running
- 전체 서비스 기동(루트에서 실행):
```bash
docker compose up -d
```
- 상태/로그/정지:
```bash
docker compose ps
docker compose logs <service> | cat
docker compose down
```

### Selective Run (Examples)
- 특정 프래그먼트만 선택 실행:
```bash
docker compose -f docker-compose.yml -f docker/compose.mysql.yml -f docker/compose.redis.yml up -d
```
- PowerShell 세션에서 기본 프래그먼트 묶기:
```powershell
$env:COMPOSE_FILE="docker-compose.yml;docker/compose.mysql.yml;docker/compose.redis.yml"
docker compose up -d
```

### Adding a New Fragment
1) `docker/compose.<name>.yml` 파일 생성
```yaml
services:
  <service-name>:
    image: <vendor>/<image>:<tag>
    ports:
      - "<host>:<container>"
    environment:
      # 환경변수 필요 시
    volumes:
      - ${COMPOSE_PROJECT_DIR}/data/<service-name>:/data
    healthcheck:
      # 필요 시 정의
```
2) 루트 `docker-compose.yml`에 `extends` 추가:
```yaml
services:
  <service-name>:
    extends:
      file: docker/compose.<name>.yml
      service: <service-name>
```

### Notes
- 개발 편의를 위해 일부 프래그먼트는 자동 토픽 생성, PLAINTEXT 등 로컬 지향 설정을 포함할 수 있습니다. 운영 구성과 분리하여 사용하세요.

