# ⚙️ core

## 폴더 구조

```text
core/
├── domain/                 # 도메인별 비즈니스 로직
│   ├── user/              # 사용자 도메인
│   │   ├── UserService.kt         # 비즈니스 로직
│   │   ├── User.kt                # 도메인 Entity
│   │   ├── UserCommand.kt         # Command DTO
│   │   └── UserPort.kt            # Repository 인터페이스
│   ├── product/           # 상품 도메인
│   └── order/             # 주문 도메인
├── common/                # 공통 서비스
└── external/              # 외부 시스템 연동
```

## 역할

**비즈니스 로직 구현**: 도메인 규칙, 트랜잭션 관리, 데이터 변환

