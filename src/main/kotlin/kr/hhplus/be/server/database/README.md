# database/

## 📌 역할
core 레이어에 정의된 Repository 인터페이스의 실제 구현을 담당합니다.  
또한 외부 시스템과의 연동(DB, 메시징, 외부 API 등)도 포함될 수 있습니다.

---

## 🗂️ 구조 예시

```text
database/
├── user/
│ ├── PgUserRepository.kt
│ └── ...
├── product/
│ ├── PgProductRepository.kt
│ └── ...
└── ...
```

---

## 🔗 연관 문서
- [instruction.md](instruction.md)
