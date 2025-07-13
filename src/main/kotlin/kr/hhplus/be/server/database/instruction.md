# database/instruction.md

## ✅ 작성 규칙

### 🧱 구조
- 도메인 단위로 하위 디렉토리를 구성 (ex: `user/`, `product/`)
- 각 도메인에는 Repository 구현체 또는 Infra 연동 구현이 위치

### 📐 클래스 네이밍
- Repository 구현체는 `Pg~Repository.kt` 형식으로 명시 (`PgUserRepository`)
    - Pg = PostgreSQL, Mongo = MongoDB, Redis = Redis 등
- Spring Data JPA 사용 시 `@Repository` 명시 + `JpaRepository<,>` 구현

### 🔌 책임
- core 레이어에 정의된 `UserRepository` 인터페이스를 실제 DB에 맞게 구현
- JPA Query, QueryDSL, Native Query 등 사용 가능
- DB 외 시스템과의 연동 구현도 포함 가능 (ex. KafkaProducer, RedisAdapter)

### 🔐 트랜잭션 관리
- 트랜잭션 범위는 서비스(core) 레이어에서 제어
- DB 접근 코드 자체에는 `@Transactional` 지양

---

## 💬 커밋 템플릿 예시

```markdown
feat(database-user): UserRepository JPA 구현 추가
refactor(database-product): QueryDSL 기반 Product 조회 리팩토링
```