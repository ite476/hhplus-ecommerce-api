# 🗄️ repository

## 폴더 구조

```text
repository/
├── user/
│   └── UserRepository.kt    # 저장소 연동된 경우

├── order/
│   ├── UserAdapter.kt       # 서비스, 외부 API 등에서 가져오는 경우
...
```

> 상세한 지침은 추후 DB 레이어 실 구현 작업 시 설정합니다. <br/>

## 역할

**데이터 접근 추상화**: 영속성 기술 독립적인 CRUD 및 복잡한 쿼리 처리

