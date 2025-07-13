# core/instruction.md

## ✅ 작성 규칙

### 🧱 구조
- 도메인 단위로 디렉토리를 구분 (`user/`, `product/` 등)
- 각 디렉토리는 다음 세 가지 구성요소를 포함 가능:
    - `Entity or Aggregate`: 핵심 도메인 객체 (`User`, `Product` 등)
    - `Service`: 유즈케이스 담당 (`UserService`)
    - `Repository`: 영속성 인터페이스 (`UserRepository`)

### 📐 네이밍
- Entity: `User`, `Product` 등 단수형 사용
- Service: `UserService`, `ProductService`
- Repository: `UserRepository`, `ProductRepository`

### 🧠 도메인 설계 가이드
- 도메인 상태와 행위는 Entity 내부에 구현
- 외부 의존성이 필요한 작업은 Service에서 처리
- 가능한 한 domain → 외부 의존성으로만 흐르게 할 것
- 단순 데이터 홀더가 아닌 "행위 중심 모델"을 지향

### ❗ Repository는 반드시 인터페이스로만 정의
- 구현체는 `database/` 레이어에 위치

---

## 💬 커밋 템플릿 예시

```markdown
feat(core-user): 사용자 생성 유스케이스 및 도메인 정의
refactor(core-product): 가격 정책 로직 도메인 모델로 이동
```