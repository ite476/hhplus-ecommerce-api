# core/

## 📌 역할
도메인 로직의 핵심을 구성하는 레이어입니다.  
비즈니스 규칙, 도메인 모델, 서비스, 인터페이스(repository 등)를 포함합니다.
---
## 🗂️ 구조 예시
```text
core/
├── user/
│ ├── User.kt
│ ├── UserService.kt
│ ├── UserRepository.kt
│ └── ...
├── product/
│ ├── Product.kt
│ ├── ProductService.kt
│ ├── ProductRepository.kt
│ └── ...
└── ...
```

---

## 🧭 책임
- 도메인 모델의 상태 및 행위 정의
- 유스케이스를 수행하는 서비스 구현
- 영속성 인터페이스 정의 (`Repository`)
- 도메인 내부 정책, 규칙, 이벤트 정의

---

## 🔗 연관 문서
- [instruction.md](instruction.md)

