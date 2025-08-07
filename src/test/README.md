# 📁 test

## 폴더 구조

```text
test/
├── README.md                    # 이 문서 - 테스트 패키지 전체 가이드
├── instruction.md               # 공통 테스트 컨벤션
└── java/kr/hhplus/be/server/
    ├── controller/              # 컨트롤러 레이어 테스트
    │   ├── instruction.md       # 컨트롤러 테스트 전용 가이드
    │   └── v1/                  # API 버전별 테스트
    │       ├── coupon/          # 쿠폰 API 테스트
    │       ├── order/           # 주문 API 테스트
    │       ├── point/           # 포인트 API 테스트
    │       └── product/         # 상품 API 테스트
    ├── service/                 # 서비스 레이어 테스트
    │   ├── instruction.md       # 서비스 테스트 전용 가이드
    │   ├── ServiceTestBase.kt   # 서비스 테스트 공통 베이스
    │   ├── coupon/              # 쿠폰 서비스 테스트
    │   ├── order/               # 주문 서비스 테스트
    │   ├── point/               # 포인트 서비스 테스트
    │   ├── product/             # 상품 서비스 테스트
    │   └── user/                # 사용자 서비스 테스트
    ├── ServerApplicationTests.kt # 통합 테스트
    └── TestcontainersConfiguration.kt # 테스트 컨테이너 설정
```

## 테스트 레이어별 특징

### 🎯 Controller Layer Tests
- **목적**: REST API 엔드포인트의 HTTP 요청/응답 검증
- **기술**: `@WebMvcTest` + `RestClient`
- **검증 범위**: HTTP 상태 코드, 응답 구조, 요청 파라미터 유효성
- **Mock 대상**: Service 레이어 (비즈니스 로직)

### ⚙️ Service Layer Tests  
- **목적**: 비즈니스 로직과 도메인 규칙 검증
- **기술**: `@MockK` + `ServiceTestBase`
- **검증 범위**: 도메인 엔티티, 비즈니스 규칙, 의존성 호출
- **Mock 대상**: Port 인터페이스 (데이터 접근)

## 참고 가이드

| 테스트 유형 | 상세 가이드 |
|------------|------------|
| **공통 컨벤션** | [📋 instruction.md](./instruction.md) |
| **Controller 테스트** | [🎯 controller/instruction.md](./java/kr/hhplus/be/server/controller/instruction.md) |
| **Service 테스트** | [⚙️ service/instruction.md](./java/kr/hhplus/be/server/service/instruction.md) |

## 원본 패키지 구조 참고

테스트 패키지는 원본 패키지(`src/main/kotlin/kr/hhplus/be/server/`)의 구조를 따릅니다:

- **폴더 구조**: 원본과 동일한 패키지 경로 유지
- **네이밍**: `{클래스명}Test.kt` 패턴
- **배치**: 테스트하는 클래스와 동일한 패키지 위치

```text
src/main/kotlin/kr/hhplus/be/server/controller/v1/point/PointController.kt
    ↓ 대응
src/test/java/kr/hhplus/be/server/controller/v1/point/PointControllerTest.kt
```

## 테스트 환경 설정

- **Test Profile**: `application-test.yml`
- **Database**: Testcontainers (PostgreSQL)
- **시간 고정**: `2024-01-15 10:30:00 Asia/Seoul`
- **페이징 기본값**: `page=0, size=10`