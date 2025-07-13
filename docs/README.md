# 📦 e-커머스 상품 주문 서비스 (Backend API)

> 상품 주문, 쿠폰 관리 등의 기능을 제공하는 백엔드 API 프로젝트입니다.

---

## 🗂️ 프로젝트 구조

```text
src/main/kotlin/com/example/app/
├── ServiceApplication.kt
├── config/
├── controller/
├── core/
└── database/
```

📄 각 디렉토리의 책임 및 구현 방식은 하위 문서에서 상세히 확인할 수 있습니다.

| 폴더            | 역할 요약                             | 문서 링크                                                                                                                                                   |
|---------------|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `config/`     | 글로벌 설정 (JPA, Swagger, Security 등) | [README](../src/main/kotlin/kr/hhplus/be/server/config/README.md) / [instruction](../src/main/kotlin/kr/hhplus/be/server/config/instruction.md)         |
| `controller/` | API 엔트리포인트 및 DTO 처리               | [README](../src/main/kotlin/kr/hhplus/be/server/controller/README.md) / [instruction](../src/main/kotlin/kr/hhplus/be/server/controller/instruction.md) |
| `core/`       | 도메인 로직, 서비스, 인터페이스 정의             | [README](../src/main/kotlin/kr/hhplus/be/server/core/README.md) / [instruction](../src/main/kotlin/kr/hhplus/be/server/core/instruction.md)             |
| `database/`   | 실제 데이터 접근/저장 구현체 구성               | [README](../src/main/kotlin/kr/hhplus/be/server/database/README.md) / [instruction](../src/main/kotlin/kr/hhplus/be/server/database/instruction.md)     |

---

## 🧱 레이어드 아키텍처

### 1. Controller
- REST 요청/응답 진입점
- 요청 파라미터 처리, 유효성 검증 (서비스 호출 전)
- DTO → 도메인 변환, 응답 → JSON 직렬화
### 2. Core
- 실제 비즈니스 도메인 로직 처리
- `Service` : 도메인 연결 및 흐름 제어
- `Domain` : 상태 관리 및 행위 정의
- `Repository` : 데이터 접근을 위한 인터페이스 정의 (실제 저장 책임 없음)
### 3. Database
- `@Repository` 등 빈 등록 포함
- Repository 구현체 (JPA, QueryDSL, etc)
- 실제 데이터 저장/조회 책임 담당

---

## 📡 HTTP 응답 설계 정책

| 상태코드                       | 의미                              | 예시                                              |
|----------------------------|---------------------------------|-------------------------------------------------|
| `200 OK`                   | 정상 요청 처리                        | -                                               |
| `400 Bad Request`          | 요청이 잘못된 경우 (JSON 구조, 파라미터 오류 등) | - JSON 형식 에러                                    |
| `404 Not Found`            | URI에 명시된 리소스가 존재하지 않음           | - `/user/1/bank-account/3` 중 user 또는 account 없음 |
| `409 Conflict`             | 서버 상태와 충돌하여 요청 수행 불가            | - 쿠폰 만료, 재고 부족, 유저 미존재 등                        |
| `422 Unprocessable Entity` | 정책적으로 허용되지 않는 요청                | - 간식 상품 최대 5,000원 제한                            |

> ❗ 200/400/500으로만 퉁치지 않고, 도메인 상황에 맞는 응답 코드 선택을 기본 원칙으로 함.

---

## 🎯 설계 철학 요약
### Controller Layer
- 서비스 호출 전 유효성 검증 책임
- 요청/응답 모델 분리 (DTO)
### Core Layer
- `@Service`: 흐름 제어, 도메인 호출
- `Domain`: 상태 보유 및 행위 중심 (data class)
- `Repository`: interface only
### Database Layer
- 구현체만 정의 (JPA, QueryDSL, etc)
- 외부 infra 의존 포함 가능 (EntityMapper, @Repository)

---

## 📌 작업 중 정보는?
> 아직 결정되지 않았거나 정리 중인 WIP 내용은 📂 docs/drafts 폴더에 별도로 문서화하거나, GitHub Issue로 관리됩니다.
