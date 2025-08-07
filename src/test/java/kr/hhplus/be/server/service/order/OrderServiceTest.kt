package kr.hhplus.be.server.service.order

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kr.hhplus.be.server.service.ServiceTestBase
import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.entity.UserCouponStatus
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.entity.OrderItem
import kr.hhplus.be.server.service.order.port.DataPlatformPort
import kr.hhplus.be.server.service.order.port.OrderPort
import kr.hhplus.be.server.service.order.service.OrderService
import kr.hhplus.be.server.service.order.service.OrderServiceFacade
import kr.hhplus.be.server.service.point.entity.PointChange
import kr.hhplus.be.server.service.point.entity.PointChangeType
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.user.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OrderService 단위테스트")
class OrderServiceTest : ServiceTestBase() {
    @MockK
    private lateinit var orderServiceFacade: OrderServiceFacade

    @MockK
    private lateinit var orderPort: OrderPort
    
    @MockK
    private lateinit var dataPlatformPort: DataPlatformPort
    
    private lateinit var orderService: OrderService



    @BeforeEach
    fun setupOrderService() {
        super.setUp()
        orderService = OrderService(
            facade = orderServiceFacade,
            orderPort = orderPort,
            dataPlatformPort = dataPlatformPort,
            timeProvider = timeProvider
        )
    }

    @Nested
    @DisplayName("createOrder 메서드는 (코루틴)")
    inner class CreateOrderTest {

        @Test
        @DisplayName("쿠폰 없이 주문을 성공적으로 생성한다")
        fun createsOrderWithoutCoupon() = runTest {
            // given
            val userId = 1L
            val products: List<OrderService.CreateOrderInput.ProductWithQuantity> = listOf(
                OrderService.CreateOrderInput.ProductWithQuantity(productId = 1L, quantity = 2),
                OrderService.CreateOrderInput.ProductWithQuantity(productId = 2L, quantity = 1)
            )
            val input = OrderService.CreateOrderInput(userId = userId, products = products, userCouponId = null)

            val user = User(id = userId, name = "김철수", point = 20000L)
            val productEntities: List<Product> = listOf(
                Product(id = 1L, name = "아메리카노", price = 4500L, stock = 100, createdAt = fixedTime),
                Product(id = 2L, name = "라떼", price = 5000L, stock = 50, createdAt = fixedTime)
            )
            val pointChange = PointChange(
                id = 1L,
                userId = userId,
                pointChange = 14000L,
                type = PointChangeType.Use,
                happenedAt = fixedTime
            )
            
            val orderItems: List<OrderItem> = listOf(
                OrderItem(id = 1L, productId = 1L, productName = "아메리카노", unitPrice = 4500L, quantity = 2),
                OrderItem(id = 2L, productId = 2L, productName = "라떼", unitPrice = 5000L, quantity = 1)
            )
            val expectedOrder = Order(
                id = 1L,
                userId = userId,
                userCouponId = null,
                orderItems = orderItems,
                totalProductsPrice = 14000L,
                discountedPrice = 0L,
                orderedAt = fixedTime
            )

            // Mock 설정
            every { orderServiceFacade.findUserById(userId) } returns user
            every { orderServiceFacade.findProductById(1L) } returns productEntities[0]
            every { orderServiceFacade.findProductById(2L) } returns productEntities[1]
            every { orderServiceFacade.reduceProductStock(1L, 2, fixedTime) } returns Unit
            every { orderServiceFacade.reduceProductStock(2L, 1, fixedTime) } returns Unit
            every { orderServiceFacade.usePoint(userId, 14000L) } returns pointChange
            coEvery { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) } returns expectedOrder
            coEvery { dataPlatformPort.sendOrderData(order = any()) } returns Unit

            // when
            val result = orderService.createOrder(input)

            // then
            result shouldBe expectedOrder
            verify { orderServiceFacade.findUserById(userId) }
            verify { orderServiceFacade.findProductById(1L) }
            verify { orderServiceFacade.findProductById(2L) }
            verify { orderServiceFacade.reduceProductStock(1L, 2, fixedTime) }
            verify { orderServiceFacade.reduceProductStock(2L, 1, fixedTime) }
            verify { orderServiceFacade.usePoint(userId, 14000L) }
            coVerify { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) }
            coVerify { dataPlatformPort.sendOrderData(order = any()) }
        }

        @Test
        @DisplayName("쿠폰을 사용하여 주문을 성공적으로 생성한다")
        fun createsOrderWithCoupon() = runTest {
            // given
            val userId = 1L
            val userCouponId = 1L
            val products: List<OrderService.CreateOrderInput.ProductWithQuantity> = listOf(
                OrderService.CreateOrderInput.ProductWithQuantity(productId = 1L, quantity = 1)
            )
            val input = OrderService.CreateOrderInput(userId = userId, products = products, userCouponId = userCouponId)

            val user = User(id = userId, name = "김철수", point = 15000L)
            val product = Product(id = 1L, name = "아메리카노", price = 4500L, stock = 100, createdAt = fixedTime)
            val userCoupon = UserCoupon(
                id = 1L, userId = userId, couponId = 1L, couponName = "신규가입쿠폰", discount = 2000L,
                status = UserCouponStatus.ACTIVE, issuedAt = fixedTime, usedAt = null, validUntil = fixedTime.plusDays(30)
            )
            val pointChange = PointChange(
                id = 1L,
                userId = userId,
                pointChange = 2500L,
                type = PointChangeType.Use,
                happenedAt = fixedTime
            )
            
            val orderItems: List<OrderItem> = listOf(
                OrderItem(id = 1L, productId = 1L, productName = "아메리카노", unitPrice = 4500L, quantity = 1)
            )
            val expectedOrder = Order(
                id = 1L,
                userId = userId,
                userCouponId = userCouponId,
                orderItems = orderItems,
                totalProductsPrice = 4500L,
                discountedPrice = 2000L,
                orderedAt = fixedTime
            )

            // Mock 설정
            every { orderServiceFacade.findUserById(userId) } returns user
            every { orderServiceFacade.findProductById(1L) } returns product
            every { orderServiceFacade.reduceProductStock(1L, 1, fixedTime) } returns Unit
            every { orderServiceFacade.findUserCouponById(userId, userCouponId) } returns userCoupon
            every { orderServiceFacade.useUserCoupon(userCoupon, fixedTime) } returns Unit
            every { orderServiceFacade.usePoint(userId, 2500L) } returns pointChange
            coEvery { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) } returns expectedOrder
            coEvery { dataPlatformPort.sendOrderData(order = any()) } returns Unit

            // when
            val result: Order = orderService.createOrder(input)

            // then
            result shouldBe expectedOrder
            verify { orderServiceFacade.findUserById(userId) }
            verify { orderServiceFacade.findProductById(1L) }
            verify { orderServiceFacade.reduceProductStock(1L, 1, fixedTime) }
            verify { orderServiceFacade.findUserCouponById(userId, userCouponId) }
            verify { orderServiceFacade.useUserCoupon(userCoupon, fixedTime) }
            verify { orderServiceFacade.usePoint(userId, 2500L) }
            coVerify { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) }
            coVerify { dataPlatformPort.sendOrderData(order = any()) }
        }
    }

    @Nested
    @DisplayName("Order Entity 검증")
    inner class OrderEntityTest {

        @Test
        @DisplayName("Order가 올바르게 생성된다")
        fun createOrderCorrectly() {
            // given & when
            val orderItems: List<OrderItem> = listOf(
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
            order.purchasedPrice shouldBe 8000L // 9000 - 1000
            order.orderedAt shouldBe fixedTime
        }

        @Test
        @DisplayName("OrderItem이 올바르게 생성된다")
        fun createOrderItemCorrectly() {
            // given & when
            val orderItem = OrderItem(
                id = 1L, productId = 1L, productName = "아메리카노", unitPrice = 4500L, quantity = 2
            )

            // then
            orderItem.id shouldBe 1L
            orderItem.productId shouldBe 1L
            orderItem.productName shouldBe "아메리카노"
            orderItem.unitPrice shouldBe 4500L
            orderItem.quantity shouldBe 2
            orderItem.totalPrice shouldBe 9000L // 4500 * 2
        }

        @Test
        @DisplayName("여러 OrderItem으로 구성된 Order가 올바르게 생성된다")
        fun createOrderWithMultipleItems() {
            // given & when
            val orderItems: List<OrderItem> = listOf(
                OrderItem(id = 1L, productId = 1L, productName = "아메리카노", unitPrice = 4500L, quantity = 2),
                OrderItem(id = 2L, productId = 2L, productName = "라떼", unitPrice = 5000L, quantity = 1)
            )
            val order = Order(
                id = 1L,
                userId = 1L,
                userCouponId = 1L,
                orderItems = orderItems,
                totalProductsPrice = 14000L,
                discountedPrice = 2000L,
                orderedAt = fixedTime
            )

            // then
            order.orderItems.size shouldBe 2
            order.totalProductsPrice shouldBe 14000L
            order.discountedPrice shouldBe 2000L
            order.purchasedPrice shouldBe 12000L // 14000 - 2000
            order.orderItems[0].productName shouldBe "아메리카노"
            order.orderItems[1].productName shouldBe "라떼"
        }
    }

    @Nested
    @DisplayName("CreateOrderInput 검증")
    inner class CreateOrderInputTest {

        @Test
        @DisplayName("CreateOrderInput이 올바르게 생성된다")
        fun createInputCorrectly() {
            // given & when
            val products: List<OrderService.CreateOrderInput.ProductWithQuantity> = listOf(
                OrderService.CreateOrderInput.ProductWithQuantity(productId = 1L, quantity = 2),
                OrderService.CreateOrderInput.ProductWithQuantity(productId = 2L, quantity = 1)
            )
            val input = OrderService.CreateOrderInput(userId = 1L, products = products, userCouponId = 1L)

            // then
            input.userId shouldBe 1L
            input.products.size shouldBe 2
            input.userCouponId shouldBe 1L
            input.products[0].productId shouldBe 1L
            input.products[0].quantity shouldBe 2
            input.products[1].productId shouldBe 2L
            input.products[1].quantity shouldBe 1
        }

        @Test
        @DisplayName("쿠폰 없는 CreateOrderInput이 올바르게 생성된다")
        fun createInputWithoutCoupon() {
            // given & when
            val products: List<OrderService.CreateOrderInput.ProductWithQuantity> = listOf(
                OrderService.CreateOrderInput.ProductWithQuantity(productId = 1L, quantity = 1)
            )
            val input = OrderService.CreateOrderInput(userId = 1L, products = products, userCouponId = null)

            // then
            input.userId shouldBe 1L
            input.products.size shouldBe 1
            input.userCouponId shouldBe null
            input.products[0].productId shouldBe 1L
            input.products[0].quantity shouldBe 1
        }
    }

    @Nested
    @DisplayName("CompensationScope 롤백 테스트")
    inner class RollbackTest {

        @Test
        @DisplayName("데이터 플랫폼 전송 실패 시 전체 롤백이 수행된다 (쿠폰 없는 경우)")
        fun rollsBackAllWhenDataPlatformFailsWithoutCoupon() = runTest {
            // given
            val userId = 1L
            val products: List<OrderService.CreateOrderInput.ProductWithQuantity> = listOf(
                OrderService.CreateOrderInput.ProductWithQuantity(productId = 1L, quantity = 2)
            )
            val input = OrderService.CreateOrderInput(userId = userId, products = products, userCouponId = null)

            val user = User(id = userId, name = "김철수", point = 20000L)
            val product = Product(id = 1L, name = "아메리카노", price = 4500L, stock = 100, createdAt = fixedTime)
            val pointChange = PointChange(
                id = 1L,
                userId = userId,
                pointChange = 9000L,
                type = PointChangeType.Use,
                happenedAt = fixedTime
            )
            
            val orderItems: List<OrderItem> = listOf(
                OrderItem(id = 1L, productId = 1L, productName = "아메리카노", unitPrice = 4500L, quantity = 2)
            )
            val createdOrder = Order(
                id = 1L,
                userId = userId,
                userCouponId = null,
                orderItems = orderItems,
                totalProductsPrice = 9000L,
                discountedPrice = 0L,
                orderedAt = fixedTime
            )

            // Mock 설정 - 정상 플로우
            every { orderServiceFacade.findUserById(userId) } returns user
            every { orderServiceFacade.findProductById(1L) } returns product
            every { orderServiceFacade.reduceProductStock(1L, 2, fixedTime) } returns Unit
            every { orderServiceFacade.usePoint(userId, 9000L) } returns pointChange
            coEvery { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) } returns createdOrder
            
            // 데이터 플랫폼 전송에서 실패
            val dataException = RuntimeException("데이터 플랫폼 전송 실패")
            coEvery { dataPlatformPort.sendOrderData(order = any()) } throws dataException
            
            // 롤백 메서드들 모킹
            every { orderServiceFacade.addProductStock(1L, 2, fixedTime) } returns Unit
            val rollbackPointChange1 = PointChange(
                id = 2L,
                userId = userId,
                pointChange = 9000L,
                type = PointChangeType.Charge,
                happenedAt = fixedTime
            )
            every { orderServiceFacade.chargePoint(userId, 9000L) } returns rollbackPointChange1
            every { orderPort.cancelOrder(order = any()) } returns Unit

            // when & then
            shouldThrow<RuntimeException> {
                orderService.createOrder(input)
            }

            // 정상 작업들이 실행되었는지 확인
            verify { orderServiceFacade.findUserById(userId) }
            verify { orderServiceFacade.findProductById(1L) }
            verify { orderServiceFacade.reduceProductStock(1L, 2, fixedTime) }
            verify { orderServiceFacade.usePoint(userId, 9000L) }
            coVerify { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) }
            coVerify { dataPlatformPort.sendOrderData(order = any()) }

            // 롤백 작업들이 실행되었는지 확인
            verify { orderServiceFacade.addProductStock(1L, 2, fixedTime) }
            verify { orderServiceFacade.chargePoint(userId, 9000L) }
            verify { orderPort.cancelOrder(order = any()) }
        }

        @Test
        @DisplayName("데이터 플랫폼 전송 실패 시 전체 롤백이 수행된다 (쿠폰 있는 경우)")
        fun rollsBackAllWhenDataPlatformFailsWithCoupon() = runTest {
            // given
            val userId = 1L
            val userCouponId = 1L
            val products: List<OrderService.CreateOrderInput.ProductWithQuantity> = listOf(
                OrderService.CreateOrderInput.ProductWithQuantity(productId = 1L, quantity = 1)
            )
            val input = OrderService.CreateOrderInput(userId = userId, products = products, userCouponId = userCouponId)

            val user = User(id = userId, name = "김철수", point = 15000L)
            val product = Product(id = 1L, name = "아메리카노", price = 4500L, stock = 100, createdAt = fixedTime)
            val userCoupon = UserCoupon(
                id = userCouponId, userId = userId, couponId = 1L, couponName = "신규가입쿠폰", discount = 2000L,
                status = UserCouponStatus.ACTIVE, issuedAt = fixedTime, usedAt = null, validUntil = fixedTime.plusDays(30)
            )
            val pointChange = PointChange(
                id = 1L,
                userId = userId,
                pointChange = 2500L, // 4500 - 2000 = 2500
                type = PointChangeType.Use,
                happenedAt = fixedTime
            )
            
            val orderItems: List<OrderItem> = listOf(
                OrderItem(id = 1L, productId = 1L, productName = "아메리카노", unitPrice = 4500L, quantity = 1)
            )
            val createdOrder = Order(
                id = 1L,
                userId = userId,
                userCouponId = userCouponId,
                orderItems = orderItems,
                totalProductsPrice = 4500L,
                discountedPrice = 2000L,
                orderedAt = fixedTime
            )

            // Mock 설정 - 정상 플로우
            every { orderServiceFacade.findUserById(userId) } returns user
            every { orderServiceFacade.findProductById(1L) } returns product
            every { orderServiceFacade.findUserCouponById(userId, userCouponId) } returns userCoupon
            every { orderServiceFacade.reduceProductStock(1L, 1, fixedTime) } returns Unit
            every { orderServiceFacade.useUserCoupon(userCoupon, fixedTime) } returns Unit
            every { orderServiceFacade.usePoint(userId, 2500L) } returns pointChange
            coEvery { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) } returns createdOrder
            
            // 데이터 플랫폼 전송에서 실패
            val dataException = RuntimeException("데이터 플랫폼 전송 실패")
            coEvery { dataPlatformPort.sendOrderData(order = any()) } throws dataException
            
            // 롤백 메서드들 모킹
            every { orderServiceFacade.addProductStock(1L, 1, fixedTime) } returns Unit
            every { orderServiceFacade.rollbackUserCouponUsage(userCoupon, fixedTime) } returns Unit
            val rollbackPointChange2 = PointChange(
                id = 3L,
                userId = userId,
                pointChange = 2500L,
                type = PointChangeType.Charge,
                happenedAt = fixedTime
            )
            every { orderServiceFacade.chargePoint(userId, 2500L) } returns rollbackPointChange2
            every { orderPort.cancelOrder(order = any()) } returns Unit

            // when & then
            shouldThrow<RuntimeException> {
                orderService.createOrder(input)
            }

            // 정상 작업들이 실행되었는지 확인
            verify { orderServiceFacade.findUserById(userId) }
            verify { orderServiceFacade.findProductById(1L) }
            verify { orderServiceFacade.findUserCouponById(userId, userCouponId) }
            verify { orderServiceFacade.reduceProductStock(1L, 1, fixedTime) }
            verify { orderServiceFacade.usePoint(userId, 2500L) }
            coVerify { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) }
            coVerify { dataPlatformPort.sendOrderData(order = any()) }

            // 롤백 작업들이 실행되었는지 확인
            verify { orderServiceFacade.addProductStock(1L, 1, fixedTime) }
            verify { orderServiceFacade.rollbackUserCouponUsage(userCoupon, fixedTime) }
            verify { orderServiceFacade.chargePoint(userId, 2500L) }
            verify { orderPort.cancelOrder(order = any()) }
        }

        @Test
        @DisplayName("주문 생성 실패 시 부분 롤백이 수행된다")
        fun rollsBackPartiallyWhenOrderCreationFails() = runTest {
            // given
            val userId = 1L
            val products: List<OrderService.CreateOrderInput.ProductWithQuantity> = listOf(
                OrderService.CreateOrderInput.ProductWithQuantity(productId = 1L, quantity = 1)
            )
            val input = OrderService.CreateOrderInput(userId = userId, products = products, userCouponId = null)

            val user = User(id = userId, name = "김철수", point = 15000L)
            val product = Product(id = 1L, name = "아메리카노", price = 4500L, stock = 100, createdAt = fixedTime)
            val pointChange = PointChange(
                id = 1L,
                userId = userId,
                pointChange = 4500L,
                type = PointChangeType.Use,
                happenedAt = fixedTime
            )

            // Mock 설정 - 주문 생성까지는 정상, 주문 생성에서 실패
            every { orderServiceFacade.findUserById(userId) } returns user
            every { orderServiceFacade.findProductById(1L) } returns product
            every { orderServiceFacade.reduceProductStock(1L, 1, fixedTime) } returns Unit
            every { orderServiceFacade.usePoint(userId, 4500L) } returns pointChange
            
            // 주문 생성에서 실패
            val orderException = RuntimeException("주문 생성 실패")
            coEvery { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) } throws orderException
            
            // 롤백 메서드들 모킹
            every { orderServiceFacade.addProductStock(1L, 1, fixedTime) } returns Unit
            val rollbackPointChange3 = PointChange(
                id = 4L,
                userId = userId,
                pointChange = 4500L,
                type = PointChangeType.Charge,
                happenedAt = fixedTime
            )
            every { orderServiceFacade.chargePoint(userId, 4500L) } returns rollbackPointChange3

            // when & then
            shouldThrow<RuntimeException> {
                orderService.createOrder(input)
            }

            // 정상 작업들이 실행되었는지 확인
            verify { orderServiceFacade.findUserById(userId) }
            verify { orderServiceFacade.findProductById(1L) }
            verify { orderServiceFacade.reduceProductStock(1L, 1, fixedTime) }
            verify { orderServiceFacade.usePoint(userId, 4500L) }
            coVerify { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) }

            // 롤백 작업들이 실행되었는지 확인 (주문 취소는 실행되지 않음 - 주문이 생성되지 않았으므로)
            verify { orderServiceFacade.addProductStock(1L, 1, fixedTime) }
            verify { orderServiceFacade.chargePoint(userId, 4500L) }
            verify(exactly = 0) { orderPort.cancelOrder(order = any()) }
            
            // 데이터 플랫폼 전송은 시도되지 않음
            coVerify(exactly = 0) { dataPlatformPort.sendOrderData(order = any()) }
        }

        @Test
        @DisplayName("포인트 사용 실패 시 부분 롤백이 수행된다")
        fun rollsBackPartiallyWhenPointUsageFails() = runTest {
            // given
            val userId = 1L
            val products: List<OrderService.CreateOrderInput.ProductWithQuantity> = listOf(
                OrderService.CreateOrderInput.ProductWithQuantity(productId = 1L, quantity = 1)
            )
            val input = OrderService.CreateOrderInput(userId = userId, products = products, userCouponId = null)

            val user = User(id = userId, name = "김철수", point = 15000L)
            val product = Product(id = 1L, name = "아메리카노", price = 4500L, stock = 100, createdAt = fixedTime)

            // Mock 설정 - 포인트 사용까지는 정상, 포인트 사용에서 실패
            every { orderServiceFacade.findUserById(userId) } returns user
            every { orderServiceFacade.findProductById(1L) } returns product
            every { orderServiceFacade.reduceProductStock(1L, 1, fixedTime) } returns Unit
            
            // 포인트 사용에서 실패
            val pointException = RuntimeException("포인트 사용 실패")
            every { orderServiceFacade.usePoint(userId, 4500L) } throws pointException
            
            // 롤백 메서드들 모킹
            every { orderServiceFacade.addProductStock(1L, 1, fixedTime) } returns Unit

            // when & then
            shouldThrow<RuntimeException> {
                orderService.createOrder(input)
            }

            // 정상 작업들이 실행되었는지 확인
            verify { orderServiceFacade.findUserById(userId) }
            verify { orderServiceFacade.findProductById(1L) }
            verify { orderServiceFacade.reduceProductStock(1L, 1, fixedTime) }
            verify { orderServiceFacade.usePoint(userId, 4500L) }

            // 롤백 작업들이 실행되었는지 확인 (재고만 롤백됨)
            verify { orderServiceFacade.addProductStock(1L, 1, fixedTime) }
            
            // 포인트 사용이 실패했으므로 포인트 롤백은 실행되지 않음
            verify(exactly = 0) { orderServiceFacade.chargePoint(userId, any()) }
            
            // 후속 작업들은 실행되지 않음
            coVerify(exactly = 0) { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) }
            verify(exactly = 0) { orderPort.cancelOrder(order = any()) }
            coVerify(exactly = 0) { dataPlatformPort.sendOrderData(order = any()) }
        }
    }
} 