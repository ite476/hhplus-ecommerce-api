# 🗄️ repository

## 폴더 구조

```text
repository/
├── domain/                # 도메인별 리포지토리
│   ├── user/             # 사용자 도메인
│   │   ├── UserRepository.kt        # 리포지토리 인터페이스
│   │   └── UserJpaRepository.kt     # JPA 구현체
│   ├── product/          # 상품 도메인
│   └── order/            # 주문 도메인
├── common/               # 공통 리포지토리
└── config/               # 리포지토리 설정
```

## 역할

**데이터 접근 추상화**: 영속성 기술 독립적인 CRUD 및 복잡한 쿼리 처리

