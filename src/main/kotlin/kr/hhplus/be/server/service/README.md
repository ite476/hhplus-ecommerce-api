# ⚙️ service

## 폴더 구조

```text
service/
├── common/
│   ├── exception/                 # 공통 비즈니스 예외 상속 base
│   └── transaction/               # SAGA 패턴 트랜잭션 구현체 
├── user/
│   ├── entity/                    # User, UserProfile 등
│   ├── exception/                 # UserNotFoundException 등   
│   ├── port/                      # UserPort (interface, 필요 시 도입) 
│   ├── service/
│   │   └── UserService.kt         # 오케스트레이션 담당
│           └── CreateUserInput.kt # 각 기능 별 Input/Output을 NestedClass로 관리
...
```

## 역할

**비즈니스 로직 구현**: 도메인 규칙, 트랜잭션 관리, 데이터 변환

