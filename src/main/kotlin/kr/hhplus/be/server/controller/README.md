# controller/

## 📌 역할
HTTP 요청을 처리하는 진입점입니다.  
요청을 도메인 서비스로 위임하고, 응답 DTO로 결과를 반환합니다.

---

## 🗂️ 구조 예시
```text
controller/
├── user/
│ ├── UserController.kt
│ └── dto/
│ ├── UserRequest.kt
│ └── UserResponse.kt
├── product/
│ ├── ProductController.kt
│ └── dto/
│ ├── ProductRequest.kt
│ └── ProductResponse.kt
└── ...
```

---

## 📎 책임
- HTTP API URI 설계
- Request → DTO 매핑 및 Validation
- Service 호출 및 Response DTO 변환
- Swagger 문서화 (필요 시)

---

## 🔗 연관 문서
- [instruction.md](instruction.md)


