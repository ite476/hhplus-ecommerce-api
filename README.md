## 프로젝트 개요

> `e-커머스 상품 주문 서비스`의 백엔드 API 프로젝트입니다.
>
> 상품 주문, 쿠폰 관리 등의 기능을 제공합니다.

---

## Getting Started

### Prerequisites

#### Running Docker Containers

`local` profile 로 실행하기 위하여 인프라가 설정되어 있는 Docker 컨테이너를 실행해주셔야 합니다.

```bash
docker-compose up -d
```

---

## 🚀 핵심 기능

- 사용자 잔액 충전 / 조회
- 상품 목록 조회
- 주문 및 결제 처리
- 선착순 쿠폰 발급 / 조회 / 주문 시 사용
- 최근 3일간 인기 상품 통계 조회

> 요구사항 전체는 [📂 docs/requirements](docs/requirements) 폴더에서 확인할 수 있습니다.

---

## 🧱 아키텍처 개요

### 📁 패키지 구조

```text
src/main/kotlin/com/example/app/
├── ServiceApplication.kt
├── config/
├── controller/
├── core/
└── database/
```

- `config/`: 전역 설정 (JPA, Swagger, Security 등)
- `controller/`: API 진입점 및 DTO 처리
- `core/`: 도메인 로직, 서비스, 인터페이스 정의
- `database/`: 영속성 구현체 (`@Repository`, JPA 등)

폴더별 세부 규칙은 아래 문서를 참고하세요:

| 폴더         | 문서                                                                                                                                                 |
|------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| config     | [README](src/main/kotlin/kr/hhplus/be/server/config/README.md), [instruction](./src/main/kotlin/kr/hhplus/be/server/config/instruction.md)         |
| controller | [README](src/main/kotlin/kr/hhplus/be/server/controller/README.md), [instruction](./src/main/kotlin/kr/hhplus/be/server/controller/instruction.md) |
| core       | [README](src/main/kotlin/kr/hhplus/be/server/core/README.md), [instruction](./src/main/kotlin/kr/hhplus/be/server/core/instruction.md)             |
| database   | [README](src/main/kotlin/kr/hhplus/be/server/database/README.md), [instruction](./src/main/kotlin/kr/hhplus/be/server/database/instruction.md)     |

---

## 🧩 도메인 구조

- `User`: 잔액 충전/조회
- `Product`: 상품 조회, 인기 상품 통계
- `Order`: 주문, 결제, 외부 데이터 연동
- `Coupon`: 쿠폰 발급, 사용, 정책 제약

→ 도메인별 요구사항 명세: [docs/requirements/overview.md](./docs/requirements/overview.md)

---

## ⚙️ 설계 철학 요약

### Controller Layer
- REST API 진입점
- 파라미터 유효성 검사
- DTO ↔ 도메인 객체 변환
- 적절한 HTTP status code로 응답

### Core Layer
- `@Service`: 유스케이스 흐름 제어
- `Domain`: 상태 및 행위 중심의 `data class`
- `Repository`: 영속성 추상화 (interface-only)

### Database Layer
- `@Repository` 구현
- JPA, QueryDSL 등 영속성 기술에 의존
- 외부 infra 의존성 분리 책임

---

## 📡 응답 코드 설계 기준

| 상태코드                       | 의미                           |
|----------------------------|------------------------------|
| `200 OK`                   | 요청 성공                        |
| `400 Bad Request`          | 요청 파라미터 오류                   |
| `404 Not Found`            | URI에 명시된 리소스 없음              |
| `409 Conflict`             | 상태 충돌 (쿠폰 소진, 잔액 부족 등)       |
| `422 Unprocessable Entity` | 정책 위반 (예: 쿠폰 만료, 상품 조건 위반 등) |

---

## 🧪 테스트 및 운영 전략

- 모든 API는 최소 1개 이상의 단위 테스트 작성
- 결제/재고 관련 기능은 트랜잭션 & 동시성 고려 필수
- 다중 인스턴스 환경에서도 동작 보장
- 외부 데이터 전송은 **Mock** 또는 **Fake Module**로 대체 가능

---

## 📁 기타 문서

- [📖 요구사항 정리](./docs/requirements/overview.md)
- [📎 drafts/](./docs/drafts/) – 논의 중인 정책 또는 결정 대기 항목 (WIP)
- [📋 GitHub Issues](https://github.com/ite476/hhplus-ecommerce-api/issues) – 기능 요청 / 버그 추적 / 정책 변경