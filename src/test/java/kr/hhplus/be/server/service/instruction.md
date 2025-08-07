# ⚙️ Service 테스트 가이드

## 기본 설정

### ServiceTestBase 상속
```kotlin
@DisplayName("PointService 단위테스트")
class PointServiceTest : ServiceTestBase() {
    
    @MockK
    private lateinit var userService: UserService
    
    @MockK
    private lateinit var pointPort: PointPort
    
    private lateinit var pointService: PointService

    @BeforeEach
    fun setupPointService() {
        super.setUp()  // ⚠️ 필수! 부모 setUp 호출
        pointService = PointService(
            userService = userService,
            pointPort = pointPort,
            timeProvider = timeProvider  // ServiceTestBase에서 제공
        )
    }
}
```

### MockK 어노테이션
```kotlin
// ✅ GOOD - @MockK 사용
@MockK
private lateinit var userService: UserService

@MockK  
private lateinit var pointPort: PointPort

// ❌ BAD - @MockkBean은 Controller 테스트에서만 사용
@MockkBean  // 서비스 테스트에서 사용 금지
```

## 테스트 구조화

### @Nested 클래스로 메서드 그룹화
```kotlin
@Nested
@DisplayName("chargePoint 메서드는")
inner class ChargePointTest {

    @Test
    @DisplayName("포인트를 성공적으로 충전한다")
    fun chargesPointSuccessfully() {
        // 테스트 내용
    }

    @Test
    @DisplayName("사용자 조회 후 포인트 충전을 처리한다")
    fun verifiesUserExistenceBeforeCharging() {
        // 테스트 내용
    }
}

@Nested
@DisplayName("usePoint 메서드는")
inner class UsePointTest {
    // usePoint 관련 테스트들
}
```

## 동기 서비스 테스트

### 기본 패턴
```kotlin
@Test
@DisplayName("포인트를 성공적으로 충전한다")
fun chargesPointSuccessfully() {
    // given
    val userId = 1L
    val chargeAmount = 10000L
    val expectedPointChange = PointChange(
        id = 1L,
        userId = userId,
        pointChange = chargeAmount,
        type = PointChangeType.Charge,
        happenedAt = fixedTime
    )
    
    every { userService.findUserById(userId) } returns User(userId, "김철수", 5000L)
    every { pointPort.chargePoint(userId, chargeAmount, fixedTime) } returns expectedPointChange

    // when
    val result = pointService.chargePoint(userId = userId, point = chargeAmount)

    // then
    result shouldBe expectedPointChange
    verify { userService.findUserById(userId) }
    verify { pointPort.chargePoint(userId, chargeAmount, fixedTime) }
}
```

### Unit 타입 Mock
```kotlin
// ✅ GOOD - Unit 반환 메서드 Mocking
every { requireUserIdExistsUsecase.requireUserIdExists(userId) } just Runs
every { orderServiceFacade.reduceProductStock(productId, quantity, fixedTime) } returns Unit

// 검증
verify { requireUserIdExistsUsecase.requireUserIdExists(userId) }
verify { orderServiceFacade.reduceProductStock(productId, quantity, fixedTime) }
```

## 코루틴 서비스 테스트

### runTest 블록 사용
```kotlin
@Test
@DisplayName("쿠폰 없이 주문을 성공적으로 생성한다")
fun createsOrderWithoutCoupon() = runTest {
    // given
    val userId = 1L
    val products = listOf(
        OrderService.CreateOrderInput.ProductWithQuantity(productId = 1L, quantity = 2)
    )
    val input = OrderService.CreateOrderInput(userId, products, userCouponId = null)

    // Mock 설정
    every { orderServiceFacade.findUserById(userId) } returns user
    coEvery { orderPort.createOrder(any(), any(), any(), any()) } returns expectedOrder
    coEvery { dataPlatformPort.sendOrderData(any()) } returns Unit

    // when
    val result = orderService.createOrder(input)

    // then
    result shouldBe expectedOrder
    verify { orderServiceFacade.findUserById(userId) }
    coVerify { orderPort.createOrder(any(), any(), any(), any()) }
    coVerify { dataPlatformPort.sendOrderData(any()) }
}
```

### coEvery와 coVerify
```kotlin
// ✅ GOOD - 코루틴 함수 Mocking
coEvery { orderPort.createOrder(any(), any(), any(), any()) } returns expectedOrder
coEvery { dataPlatformPort.sendOrderData(any()) } returns Unit

// 검증
coVerify { orderPort.createOrder(any(), any(), any(), any()) }
coVerify { dataPlatformPort.sendOrderData(any()) }

// ❌ BAD - 코루틴 함수에 일반 every/verify 사용
every { orderPort.createOrder(any(), any(), any(), any()) } returns expectedOrder  // 잘못됨
verify { dataPlatformPort.sendOrderData(any()) }  // 잘못됨
```

## 예외 테스트

### assertThrows 사용
```kotlin
@Test
@DisplayName("포인트 부족 시 예외가 발생한다")
fun usePoint_InsufficientPoint_ThrowsException() {
    // given
    val userId = 1L
    val useAmount = 20000L
    every { pointPort.usePoint(any(), any(), any()) } throws LackOfPointException()

    // when & then
    assertThrows<LackOfPointException> {
        pointService.usePoint(userId, useAmount)
    }
    
    verify { pointPort.usePoint(userId, useAmount, fixedTime) }
}
```

### 코루틴 예외 테스트
```kotlin
@Test
@DisplayName("주문 생성 중 예외 발생 시 적절히 처리한다")
fun createOrder_ExceptionOccurs_HandlesGracefully() = runTest {
    // given
    val input = OrderService.CreateOrderInput(/* ... */)
    coEvery { orderPort.createOrder(any(), any(), any(), any()) } throws RuntimeException("DB 오류")

    // when & then
    assertThrows<RuntimeException> {
        orderService.createOrder(input)
    }
    
    coVerify { orderPort.createOrder(any(), any(), any(), any()) }
}
```

## Entity 검증 테스트

### Entity 생성 및 계산 로직 테스트
```kotlin
@Nested
@DisplayName("Order Entity 검증")
inner class OrderEntityTest {

    @Test
    @DisplayName("Order가 올바르게 생성된다")
    fun createOrderCorrectly() {
        // given & when
        val orderItems = listOf(
            OrderItem(id = 1L, productId = 1L, productName = "아메리카노", unitPrice = 4500L, quantity = 2)
        )
        val order = Order(
            id = 1L,
            userId = 1L,
            userCouponId = null,
            orderItems = orderItems,
            totalProductsPrice = 9000L,
            discountedPrice = 1000L,
            orderedAt = fixedTime
        )

        // then
        order.id shouldBe 1L
        order.userId shouldBe 1L
        order.userCouponId shouldBe null
        order.orderItems shouldBe orderItems
        order.totalProductsPrice shouldBe 9000L
        order.discountedPrice shouldBe 1000L
        order.purchasedPrice shouldBe 8000L // 계산 로직 검증
        order.orderedAt shouldBe fixedTime
    }
}
```

### Input/Output 객체 검증
```kotlin
@Nested
@DisplayName("CreateOrderInput 검증")
inner class CreateOrderInputTest {

    @Test
    @DisplayName("CreateOrderInput이 올바르게 생성된다")
    fun createInputCorrectly() {
        // given & when
        val products = listOf(
            OrderService.CreateOrderInput.ProductWithQuantity(productId = 1L, quantity = 2)
        )
        val input = OrderService.CreateOrderInput(userId = 1L, products = products, userCouponId = 1L)

        // then
        input.userId shouldBe 1L
        input.products.size shouldBe 1
        input.userCouponId shouldBe 1L
        input.products[0].productId shouldBe 1L
        input.products[0].quantity shouldBe 2
    }
}
```

## Mock 검증 패턴

### 정확한 호출 횟수 검증
```kotlin
// ✅ GOOD - 명시적 횟수 검증
verify(exactly = 1) { userService.findUserById(userId) }
verify(exactly = 2) { orderServiceFacade.findProductById(any()) }

coVerify(exactly = 1) { orderPort.createOrder(any(), any(), any(), any()) }

// ❌ BAD - 횟수 명시 없음 (기본값 사용)
verify { userService.findUserById(userId) }  // exactly = 1과 동일하지만 명시적이지 않음
```

### 파라미터 검증
```kotlin
// ✅ GOOD - 구체적 파라미터 검증
verify { pointPort.chargePoint(userId = userId, pointChange = chargeAmount, `when` = fixedTime) }

// ✅ GOOD - any() 사용 (파라미터가 중요하지 않은 경우)
coVerify { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) }

// ✅ GOOD - withArg를 이용한 복합 검증
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

## 테스트 데이터 패턴

### 명시적 테스트 데이터
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
val userCoupon = UserCoupon(
    id = 1L,
    userId = 1L,
    couponId = 1L,
    couponName = "신규가입쿠폰",
    discount = 2000L,
    status = UserCouponStatus.ACTIVE,
    issuedAt = fixedTime,
    usedAt = null,
    validUntil = fixedTime.plusDays(30)
)
```

### 고정 시간 사용
```kotlin
// ✅ GOOD - ServiceTestBase의 fixedTime 사용
val pointChange = PointChange(
    id = 1L,
    userId = userId,
    pointChange = chargeAmount,
    type = PointChangeType.Charge,
    happenedAt = fixedTime  // 고정 시간 사용
)

every { timeProvider.now() } returns fixedTime  // ServiceTestBase에서 자동 설정
```

## 테스트 커버리지 가이드

각 서비스마다 다음을 커버해야 합니다:

### ✅ 필수 테스트 케이스
1. **정상 케이스** - 각 퍼블릭 메서드의 성공 시나리오
2. **예외 케이스** - 비즈니스 예외 및 시스템 예외
3. **의존성 호출 검증** - Mock 객체 상호작용 확인
4. **Entity 검증** - 도메인 객체의 생성 및 계산 로직

### 📋 도메인별 추가 케이스
- **Point**: 충전/사용 로직, 포인트 부족 예외
- **Coupon**: 발급/사용 로직, 중복 발급 예외, 만료 검증
- **Order**: 주문 생성, 재고 차감, 포인트 사용, 쿠폰 적용
- **Product**: 재고 관리, 인기 상품 조회, 재고 부족 예외

### 🔍 Entity 테스트 중점사항
- **불변성**: Entity의 상태 변경 로직
- **계산 로직**: 가격 계산, 할인 적용 등
- **비즈니스 규칙**: 도메인 제약 조건 검증