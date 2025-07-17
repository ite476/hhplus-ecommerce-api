# 📁 kr.hhplus.be.server

## 폴더 구조

```text
kr.hhplus.be.server/
├── common/         # 공통 설정, Configuration
├── controller/     # REST API 엔드포인트
├── core/          # 비즈니스 로직, 도메인 모델
├── repository/    # 데이터 접근 계층
├── util/          # 공통 유틸리티
└── ServerApplication.kt
```

## 아키텍처

**Clean Architecture** 기반 레이어드 아키텍처  
**Spring Boot 3.x + Kotlin** + **DDD** 적용

## 구성요소

| 폴더                               | 역할                          |
|----------------------------------|-----------------------------|
| 🗂️ [common/](./common/)         | Spring Configuration, 공통 설정 |
| 🎯 [controller/](./controller/)  | REST API 컨트롤러               |
| ⚙️ [core/](./core/)              | 비즈니스 로직, 도메인 서비스            |
| 🗄️ [repository/](./repository/) | 데이터 접근 리포지토리                |
| 🛠️ [util/](./util/)             | 공통 유틸리티 함수                  |