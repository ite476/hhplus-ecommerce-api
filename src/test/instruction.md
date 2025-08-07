# 📋 테스트 공통 컨벤션

## 기본 원칙

### ✅ DO
- **Given-When-Then** 패턴 일관 적용
- **한국어 `@DisplayName`** 사용으로 테스트 의도 명확화
- **Kotest Assertion** 사용 (`shouldBe`, `shouldNotBe`)
- **MockK** 프레임워크로 Mock 객체 관리
- **고정 시간** 사용으로 테스트 안정성 확보

### ❌ DON'T
- JUnit의 기본 assertion (`assertEquals`) 사용 금지
- Mockito 사용 금지 (MockK 사용)
- 실제 외부 시스템 연동 금지
- 테스트 간 의존성 생성 금지

## 공통 설정

### 🕒 시간 고정
```kotlin
private val fixedTime = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("Asia/Seoul"))
```

### 📄 페이징 기본값
```kotlin
private val pagingOptions = PagingOptions(0, 10)
```

## 네이밍 컨벤션

### 테스트 클래스
```kotlin
// ✅ GOOD
class PointControllerTest
class OrderServiceTest

// ❌ BAD  
class PointControllerTestCase
class OrderServiceTests
```

### 테스트 메서드
```kotlin
// ✅ GOOD - 한국어 DisplayName
@Test
@DisplayName("정상적인 포인트 조회 요청 - 포인트가 성공적으로 반환된다")
fun getPoint_ValidRequest_ReturnsSuccessResponse() { }

@Test  
@DisplayName("존재하지 않는 사용자의 포인트 조회 - 404 Not Found 오류를 반환한다")
fun getPoint_UserNotFound_Returns404Error() { }

// ❌ BAD - 영어 DisplayName이나 부정확한 메서드명
@Test
@DisplayName("Get point successfully")
fun testGetPoint() { }
```

### 메서드명 패턴
**`{기능}_{시나리오}_{예상결과}`**

```kotlin
// ✅ GOOD
fun createOrder_ValidRequest_ReturnsSuccessResponse()
fun createOrder_InsufficientStock_ThrowsException()
fun findUser_UserNotFound_ThrowsUserNotFoundException()

// ❌ BAD
fun testCreateOrder()
fun createOrderSuccess()
fun createOrderFail()
```

## 테스트 구조

### Given-When-Then 패턴
```kotlin
@Test
@DisplayName("정상적인 포인트 충전 요청 - 포인트가 성공적으로 충전되고 201 Created를 반환한다")
fun chargePoint_ValidRequest_ReturnsSuccessResponse() {
    // Given - 테스트 준비
    val userId = 1L
    val chargeAmount = 10000L
    val requestBody = PatchPointChargeRequestBody(chargeAmount)
    val pointChange = PointChange(1L, userId, chargeAmount, PointChangeType.Charge, fixedTime)

    every { pointService.chargePoint(userId, chargeAmount) } returns pointChange

    // When & Then - 실행 및 검증
    val response = restClient.patch()
        .uri(chargePointEndpoint)
        .header("userId", userId.toString())
        .body(requestBody)
        .retrieve()
        .toEntity(String::class.java)

    response.statusCode shouldBe HttpStatus.CREATED
    response.headers.location?.toString() shouldBe "/point"
}
```

## Assertion 가이드

### ✅ Kotest Assertion 사용
```kotlin
// ✅ GOOD
response.statusCode shouldBe HttpStatus.OK
body?.point shouldBe 15000L
body shouldNotBe null
coupons.isEmpty() shouldBe true

// ❌ BAD - JUnit assertion
assertEquals(HttpStatus.OK, response.statusCode)
assertNotNull(body)
assertTrue(coupons.isEmpty())
```

### 복합 객체 검증
```kotlin
// ✅ GOOD - 체이닝으로 명확한 검증
response.body.let { body ->
    body shouldNotBe null
    body?.run {
        orderId shouldBe mockOrder.id
        totalAmount shouldBe mockOrder.totalProductsPrice
        orderItems.size shouldBe mockOrder.orderItems.size
        orderItems[0].productName shouldBe "아메리카노"
    }
}
```

## Mock 사용 가이드

### MockK 패턴
```kotlin
// ✅ GOOD - MockK 사용
@MockkBean
lateinit var pointService: PointService

every { pointService.chargePoint(userId, chargeAmount) } returns pointChange
coEvery { orderService.createOrder(any()) } returns order  // 코루틴

verify { pointService.chargePoint(userId, chargeAmount) }
coVerify { orderService.createOrder(any()) }  // 코루틴
```

### Mock 초기화
```kotlin
@BeforeEach
fun setUp() {
    clearAllMocks()  // MockK 전용
    // 필요한 초기화 작업
}
```

## 예외 테스트 패턴

### 컨트롤러 예외 테스트
```kotlin
@Test
@DisplayName("존재하지 않는 사용자의 포인트 조회 - 404 Not Found 오류를 반환한다")
fun getPoint_UserNotFound_Returns404Error() {
    // Given
    val userId = 999L
    every { userService.findUserById(userId) } throws UserNotFoundException()

    // When & Then
    try {
        restClient.get()
            .uri(getPointEndpoint)
            .header("userId", userId.toString())
            .retrieve()
            .toEntity(GetPointResponse::class.java)
    } catch (e: RestClientResponseException) {
        e.statusCode shouldBe HttpStatus.NOT_FOUND
        e.responseBodyAsString.contains("회원이 존재하지 않습니다.") shouldBe true
    }
}
```

### 서비스 예외 테스트
```kotlin
@Test
@DisplayName("포인트 부족 시 예외가 발생한다")
fun usePoint_InsufficientPoint_ThrowsException() {
    // Given
    val userId = 1L
    val useAmount = 20000L
    every { pointPort.usePoint(any(), any(), any()) } throws LackOfPointException()

    // When & Then
    assertThrows<LackOfPointException> {
        pointService.usePoint(userId, useAmount)
    }
}
```

## 테스트 데이터 관리

### 명시적 테스트 데이터 생성
```kotlin
// ✅ GOOD - 의도가 명확한 테스트 데이터
val user = User(id = 1L, name = "김철수", point = 15000L)
val product = Product(
    id = 1L, 
    name = "아메리카노", 
    price = 4500L, 
    stock = 100, 
    createdAt = fixedTime
)

// ❌ BAD - 불명확한 테스트 데이터
val user = createTestUser()  // 내부 로직 감춰짐
val product = someProduct    // 상태 불명확
```

### 테스트별 독립적 데이터
```kotlin
// ✅ GOOD - 각 테스트마다 독립적 데이터
@Test
fun test1() {
    val userId = 1L  // 이 테스트만의 데이터
    // ...
}

@Test  
fun test2() {
    val userId = 2L  // 다른 테스트와 독립적
    // ...
}
```