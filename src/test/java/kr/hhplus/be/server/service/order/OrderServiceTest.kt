package kr.hhplus.be.server.service.order

import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.test.runTest
import kr.hhplus.be.server.service.ServiceTestBase
import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.entity.UserCouponStatus
import kr.hhplus.be.server.service.coupon.usecase.FindUserCouponByIdUsecase
import kr.hhplus.be.server.service.coupon.usecase.UseUserCouponUsecase
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.entity.OrderItem
import kr.hhplus.be.server.service.order.port.DataPlatformPort
import kr.hhplus.be.server.service.order.port.OrderPort
import kr.hhplus.be.server.service.order.service.OrderService
import kr.hhplus.be.server.service.point.entity.PointChange
import kr.hhplus.be.server.service.point.entity.PointChangeType
import kr.hhplus.be.server.service.point.usecase.ChargePointUsecase
import kr.hhplus.be.server.service.point.usecase.UsePointUsecase
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.usecase.AddProductStockUsecase
import kr.hhplus.be.server.service.product.usecase.FindProductByIdUsecase
import kr.hhplus.be.server.service.product.usecase.ReduceProductStockUsecase
import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.usecase.FindUserByIdUsecase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OrderService 단위테스트")
class OrderServiceTest : ServiceTestBase() {
    @MockK
    private lateinit var findUserByIdUsecase: FindUserByIdUsecase

    @MockK
    private lateinit var findProductByIdUsercase: FindProductByIdUsecase

    @MockK
    private lateinit var reduceProductStockUsecase: ReduceProductStockUsecase

    @MockK
    private lateinit var addProduceStockUsecase: AddProductStockUsecase

    @MockK
    private lateinit var usePointUsecase: UsePointUsecase

    @MockK
    private lateinit var chargePointUsecase: ChargePointUsecase

    @MockK
    private lateinit var findUserCouponByIdUsecase: FindUserCouponByIdUsecase

    @MockK
    private lateinit var useUserCouponUsecase: UseUserCouponUsecase



    @MockK
    private lateinit var orderPort: OrderPort
    
    @MockK
    private lateinit var dataPlatformPort: DataPlatformPort
    
    private lateinit var orderService: OrderService



    @BeforeEach
    fun setupOrderService() {
        super.setUp()
        orderService = OrderService(
            findUserByIdUsecase = findUserByIdUsecase,
            findProductByIdUsercase = findProductByIdUsercase,
            reduceProductStockUsecase = reduceProductStockUsecase,
            addProduceStockUsecase = addProduceStockUsecase,
            usePointUsecase = usePointUsecase,
            chargePointUsecase = chargePointUsecase,
            findUserCouponByIdUsecase = findUserCouponByIdUsecase,
            useUserCouponUsecase = useUserCouponUsecase,
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
            every { findUserByIdUsecase.findUserById(userId) } returns user
            every { findProductByIdUsercase.findProductById(productId = 1L) } returns productEntities[0]
            every { findProductByIdUsercase.findProductById(productId = 2L) } returns productEntities[1]
            every { reduceProductStockUsecase.reduceProductStock(productId = 1L, quantity = 2, now = fixedTime) } returns Unit
            every { reduceProductStockUsecase.reduceProductStock(productId = 2L, quantity = 1, now = fixedTime) } returns Unit
            every { usePointUsecase.usePoint(userId = userId, point = 14000L) } returns pointChange
            coEvery { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) } returns expectedOrder
            coEvery { dataPlatformPort.sendOrderData(order = any()) } returns Unit

            // when
            val result = orderService.createOrder(input)

            // then
            result shouldBe expectedOrder
            verify { findUserByIdUsecase.findUserById(userId) }
            verify { findProductByIdUsercase.findProductById(productId = 1L) }
            verify { findProductByIdUsercase.findProductById(productId = 2L) }
            verify { reduceProductStockUsecase.reduceProductStock(productId = 1L, quantity = 2, now = fixedTime) }
            verify { reduceProductStockUsecase.reduceProductStock(productId = 2L, quantity = 1, now = fixedTime) }
            verify { usePointUsecase.usePoint(userId = userId, point = 14000L) }
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
            every { findUserByIdUsecase.findUserById(userId) } returns user
            every { findProductByIdUsercase.findProductById(productId = 1L) } returns product
            every { reduceProductStockUsecase.reduceProductStock(productId = 1L, quantity = 1, now = fixedTime) } returns Unit
            every { findUserCouponByIdUsecase.findUserCouponById(userId = userId, userCouponId = userCouponId) } returns userCoupon
            every { useUserCouponUsecase.useUserCoupon(userCoupon = userCoupon, now = fixedTime) } returns Unit
            every { usePointUsecase.usePoint(userId, point = 2500L) } returns pointChange
            coEvery { orderPort.createOrder(user = any(), userCouponId = any(), productsStamp = any(), now = any()) } returns expectedOrder
            coEvery { dataPlatformPort.sendOrderData(order = any()) } returns Unit

            // when
            val result: Order = orderService.createOrder(input)

            // then
            result shouldBe expectedOrder
            verify { findUserByIdUsecase.findUserById(userId) }
            verify { findProductByIdUsercase.findProductById(productId = 1L) }
            verify { reduceProductStockUsecase.reduceProductStock(productId = 1L, quantity = 1, now = fixedTime) }
            verify { findUserCouponByIdUsecase.findUserCouponById(userId = userId, userCouponId = userCouponId) }
            verify { useUserCouponUsecase.useUserCoupon(userCoupon = userCoupon, now = fixedTime) }
            verify { usePointUsecase.usePoint(userId = userId, point = 2500L) }
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
} 