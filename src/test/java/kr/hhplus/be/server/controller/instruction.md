# 🎯 Controller 테스트 가이드

## 기본 설정

### 필수 어노테이션
```kotlin
@WebMvcTest(PointController::class)  // 테스트할 컨트롤러 지정
@Import(GlobalExceptionHandler::class)  // ⚠️ 필수! 예외 처리를 위해
@DisplayName("PointController 테스트")
class PointControllerTest {
    // ...
}
```

### 의존성 Mock
```kotlin
@MockkBean
lateinit var pointService: PointService

@MockkBean  
lateinit var userService: UserService
```

### RestClient 설정
```kotlin
@Autowired
lateinit var mockMvc: MockMvc
lateinit var restClient: RestClient

@BeforeEach
fun setUp() {
    clearAllMocks()
    restClient = RestClient.builder()
        .requestFactory(MockMvcClientHttpRequestFactory(mockMvc))
        .build()
}
```

## API 테스트 패턴

### GET 요청 테스트
```kotlin
@Test
@DisplayName("정상적인 포인트 조회 요청 - 포인트가 성공적으로 반환된다")
fun getPoint_ValidRequest_ReturnsSuccessResponse() {
    // Given
    val userId = 1L
    val userPoint = 15000L
    val user = User(userId, "김철수", userPoint)

    every { userService.findUserById(userId) } returns user

    // When & Then
    val response = restClient.get()
        .uri(getPointEndpoint)
        .header("userId", userId.toString())
        .retrieve()
        .toEntity(GetPointResponse::class.java)

    response.statusCode shouldBe HttpStatus.OK
    response.body.let { body ->
        body shouldNotBe null
        body?.run {
            userId shouldBe userId
            point shouldBe userPoint
        }
    }
}
```

### POST 요청 테스트
```kotlin
@Test
@DisplayName("정상적인 주문 요청 - 주문이 성공적으로 생성되고 201 Created를 반환한다")
fun createOrder_ValidRequest_ReturnsSuccessResponse() {
    // Given
    val userId = 1L
    val requestBody = PostOrderRequestBody(
        orderItems = listOf(
            OrderItemRequest(productId = 1L, quantity = 2)
        ),
        userCouponId = null
    )
    val mockOrder = Order(/* ... */)

    coEvery { createOrderUsecase.createOrder(any()) } returns mockOrder

    // When & Then
    val response = restClient.post()
        .uri(postOrderEndpoint)
        .header("userId", userId.toString())
        .body(requestBody)
        .retrieve()
        .toEntity(PostOrderResponse::class.java)

    response.statusCode shouldBe HttpStatus.CREATED
    response.body shouldNotBe null
    
    coVerify(exactly = 1) {
        createOrderUsecase.createOrder(any())
    }
}
```

### PATCH 요청 테스트
```kotlin
@Test
@DisplayName("정상적인 포인트 충전 요청 - 포인트가 성공적으로 충전되고 201 Created를 반환한다")
fun chargePoint_ValidRequest_ReturnsSuccessResponse() {
    // Given
    val userId = 1L
    val chargeAmount = 10000L
    val requestBody = PatchPointChargeRequestBody(chargeAmount)
    val pointChange = PointChange(1L, userId, chargeAmount, PointChangeType.Charge, fixedTime)

    every { pointService.chargePoint(userId, chargeAmount) } returns pointChange

    // When & Then
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

## 쿼리 파라미터 테스트

### 페이징 파라미터
```kotlin
@Test
@DisplayName("정상적인 쿠폰 목록 조회 요청 - 쿠폰 목록이 성공적으로 반환된다")
fun getMyCoupons_ValidRequest_ReturnsSuccessResponse() {
    // Given
    val userId = 1L
    every { findPagedUserCouponsUsecase.findPagedUserCoupons(userId, any()) } returns PagedList(/* ... */)

    // When & Then
    val response = restClient.get()
        .uri {
            it.path(getMyCouponsEndpoint)
                .queryParam("page", pagingOptions.page)
                .queryParam("size", pagingOptions.size)
                .build()
        }
        .header("userId", userId.toString())
        .retrieve()
        .toEntity(GetMyCouponsResponse::class.java)

    response.statusCode shouldBe HttpStatus.OK
}
```

## 예외 처리 테스트

### 비즈니스 예외 테스트
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

### 요청 검증 실패 테스트
```kotlin
@Test
@DisplayName("userId 헤더가 누락된 포인트 조회 - 400 Bad Request를 반환한다")
fun getPoint_MissingUserIdHeader_Returns400Error() {
    // When & Then
    try {
        restClient.get()
            .uri(getPointEndpoint)
            .retrieve()
            .toEntity(GetPointResponse::class.java)
    } catch (e: RestClientResponseException) {
        e.statusCode shouldBe HttpStatus.BAD_REQUEST
    }
}
```

### 시스템 오류 테스트
```kotlin
@Test
@DisplayName("시스템 오류로 포인트 조회 실패 - 500 Internal Server Error를 반환한다")
fun getPoint_SystemError_Returns500Error() {
    // Given
    val userId = 1L
    every { userService.findUserById(userId) } throws RuntimeException("시스템 오류가 발생했습니다.")

    // When & Then
    try {
        restClient.get()
            .uri(getPointEndpoint)
            .header("userId", userId.toString())
            .retrieve()
            .toEntity(GetPointResponse::class.java)
    } catch (e: RestClientResponseException) {
        e.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
    }
}
```

## HTTP 상태 코드 가이드

| 상황 | 상태 코드 | 용도 |
|------|----------|------|
| **성공적인 조회** | `200 OK` | GET 요청 성공 |
| **성공적인 생성** | `201 Created` | POST, PATCH 요청 성공 |
| **잘못된 요청** | `400 Bad Request` | 필수 헤더 누락, 잘못된 파라미터 |
| **리소스 없음** | `404 Not Found` | 존재하지 않는 사용자/데이터 |
| **비즈니스 충돌** | `409 Conflict` | 중복 발급, 이미 사용된 쿠폰 |
| **비즈니스 규칙 위반** | `422 Unprocessable Entity` | 재고 부족, 포인트 부족 |
| **시스템 오류** | `500 Internal Server Error` | 예상치 못한 런타임 오류 |

## 응답 검증 패턴

### 성공 응답 검증
```kotlin
response.statusCode shouldBe HttpStatus.OK
response.body.let { body ->
    body shouldNotBe null
    body?.run {
        // 필수 필드 검증
        userId shouldBe expectedUserId
        point shouldBe expectedPoint
        
        // 컬렉션 검증
        coupons.size shouldBe expectedSize
        coupons[0].userCouponId shouldBe expectedId
        
        // 페이징 정보 검증
        totalCount shouldBe expectedTotalCount
    }
}
```

### 에러 응답 검증
```kotlin
try {
    // API 호출
} catch (e: RestClientResponseException) {
    e.statusCode shouldBe HttpStatus.NOT_FOUND
    e.responseBodyAsString.contains("예상 에러 메시지") shouldBe true
}
```

## Mock Verify 패턴

### 동기 서비스 호출 검증
```kotlin
verify(exactly = 1) {
    userService.findUserById(userId)
}

verify {
    pointService.chargePoint(userId, chargeAmount)
}
```

### 비동기 유스케이스 호출 검증
```kotlin
coVerify(exactly = 1) {
    createOrderUsecase.createOrder(
        withArg { input ->
            assert(input.userId == userId)
            assert(input.products.size == 2)
            assert(input.userCouponId == null)
        }
    )
}
```

## 테스트 커버리지 가이드

각 컨트롤러마다 다음 시나리오를 커버해야 합니다:

### ✅ 필수 테스트 케이스
1. **정상 케이스** - 각 HTTP 메서드별 성공 시나리오
2. **헤더 누락** - userId 헤더 없는 요청
3. **비즈니스 예외** - 도메인별 특수 예외 상황
4. **시스템 오류** - 예상치 못한 런타임 오류

### 📋 도메인별 추가 케이스
- **Point**: 0 이하 충전, 포인트 부족
- **Coupon**: 중복 발급, 재고 부족, 만료된 쿠폰
- **Order**: 재고 부족, 사용할 수 없는 쿠폰
- **Product**: 빈 목록 조회, 페이징 경계값