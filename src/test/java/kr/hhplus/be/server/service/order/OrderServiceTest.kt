package kr.hhplus.be.server.service.order

import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import kr.hhplus.be.server.service.ServiceTestBase
import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.entity.UserCouponStatus
import kr.hhplus.be.server.service.coupon.service.CouponService
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.entity.OrderItem
import kr.hhplus.be.server.service.order.port.DataPlatformPort
import kr.hhplus.be.server.service.order.port.OrderPort
import kr.hhplus.be.server.service.order.service.OrderService
import kr.hhplus.be.server.service.point.entity.PointChange
import kr.hhplus.be.server.service.point.entity.PointChangeType
import kr.hhplus.be.server.service.point.service.PointService
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.service.ProductService
import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OrderService 단위테스트")
class OrderServiceTest : ServiceTestBase() {

    @MockK
    private lateinit var userService: UserService
    
    @MockK
    private lateinit var pointService: PointService
    
    @MockK
    private lateinit var productService: ProductService
    
    @MockK
    private lateinit var couponService: CouponService
    
    @MockK
    private lateinit var orderPort: OrderPort
    
    @MockK
    private lateinit var dataPlatformPort: DataPlatformPort
    
    private lateinit var orderService: OrderService

    @BeforeEach
    fun setupOrderService() {
        super.setUp()
        orderService = OrderService(
            userService,
            pointService,
            productService,
            couponService,
            orderPort,
            dataPlatformPort,
            timeProvider
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
            val products = listOf(
                OrderService.CreateOrderInput.ProductWithQuantity(1L, 2),
                OrderService.CreateOrderInput.ProductWithQuantity(2L, 1)
            )
            val input = OrderService.CreateOrderInput(userId, products, null)

            val user = User(userId, "김철수", 20000L)
            val productEntities = listOf(
                Product(1L, "아메리카노", 4500L, 100, fixedTime),
                Product(2L, "라떼", 5000L, 50, fixedTime)
            )
            val pointChange = PointChange(1L, userId, 14000L, PointChangeType.Use, fixedTime)
            
            val orderItems = listOf(
                OrderItem(1L, 1L, "아메리카노", 4500L, 2),
                OrderItem(2L, 2L, "라떼", 5000L, 1)
            )
            val expectedOrder = Order(
                1L, userId, null, orderItems, 14000L, 0L, fixedTime
            )

            // Mock 설정
            every { userService.requireUserExists(userId) } just Runs
            every { productService.requireProductExists(any()) } just Runs
            every { couponService.requireUserCouponExists(any()) } just Runs
            every { userService.readSingleUser(userId) } returns user
            every { productService.readSingleProduct(1L) } returns productEntities[0]
            every { productService.readSingleProduct(2L) } returns productEntities[1]
            every { productService.reduceProductStock(1L, 2, fixedTime) } returns Unit
            every { productService.reduceProductStock(2L, 1, fixedTime) } returns Unit
            every { pointService.usePoint(userId, 14000L) } returns pointChange
            coEvery { orderPort.createOrder(any(), any(), any(), any()) } returns expectedOrder
            coEvery { dataPlatformPort.sendOrderData(any()) } returns Unit

            // when
            val result = orderService.createOrder(input)

            // then
            result shouldBe expectedOrder
            verify { userService.readSingleUser(userId) }
            verify { productService.readSingleProduct(1L) }
            verify { productService.readSingleProduct(2L) }
            verify { productService.reduceProductStock(1L, 2, fixedTime) }
            verify { productService.reduceProductStock(2L, 1, fixedTime) }
            verify { pointService.usePoint(userId, 14000L) }
            coVerify { orderPort.createOrder(any(), any(), any(), any()) }
            coVerify { dataPlatformPort.sendOrderData(any()) }
        }

        @Test
        @DisplayName("쿠폰을 사용하여 주문을 성공적으로 생성한다")
        fun createsOrderWithCoupon() = runTest {
            // given
            val userId = 1L
            val userCouponId = 1L
            val products = listOf(
                OrderService.CreateOrderInput.ProductWithQuantity(1L, 1)
            )
            val input = OrderService.CreateOrderInput(userId, products, userCouponId)

            val user = User(userId, "김철수", 15000L)
            val product = Product(1L, "아메리카노", 4500L, 100, fixedTime)
            val userCoupon = UserCoupon(
                1L, userId, 1L, "신규가입쿠폰", 2000L,
                UserCouponStatus.ACTIVE, fixedTime, null, fixedTime.plusDays(30)
            )
            val pointChange = PointChange(1L, userId, 2500L, PointChangeType.Use, fixedTime)
            
            val orderItems = listOf(
                OrderItem(1L, 1L, "아메리카노", 4500L, 1)
            )
            val expectedOrder = Order(
                1L, userId, userCouponId, orderItems, 4500L, 2000L, fixedTime
            )

            // Mock 설정
            every { userService.requireUserExists(userId) } just Runs
            every { productService.requireProductExists(any()) } just Runs
            every { couponService.requireUserCouponExists(any()) } just Runs
            every { userService.readSingleUser(userId) } returns user
            every { productService.readSingleProduct(1L) } returns product
            every { productService.reduceProductStock(1L, 1, fixedTime) } returns Unit
            every { couponService.readSingleUserCoupon(userId, userCouponId) } returns userCoupon
            every { couponService.useUserCoupon(userCoupon, fixedTime) } returns Unit
            every { pointService.usePoint(userId, 2500L) } returns pointChange
            coEvery { orderPort.createOrder(any(), any(), any(), any()) } returns expectedOrder
            coEvery { dataPlatformPort.sendOrderData(any()) } returns Unit

            // when
            val result = orderService.createOrder(input)

            // then
            result shouldBe expectedOrder
            verify { userService.readSingleUser(userId) }
            verify { productService.readSingleProduct(1L) }
            verify { productService.reduceProductStock(1L, 1, fixedTime) }
            verify { couponService.readSingleUserCoupon(userId, userCouponId) }
            verify { couponService.useUserCoupon(userCoupon, fixedTime) }
            verify { pointService.usePoint(userId, 2500L) }
            coVerify { orderPort.createOrder(any(), any(), any(), any()) }
            coVerify { dataPlatformPort.sendOrderData(any()) }
        }
    }

    @Nested
    @DisplayName("Order Entity 검증")
    inner class OrderEntityTest {

        @Test
        @DisplayName("Order가 올바르게 생성된다")
        fun createOrderCorrectly() {
            // given & when
            val orderItems = listOf(
                OrderItem(1L, 1L, "아메리카노", 4500L, 2)
            )
            val order = Order(
                1L, 1L, null, orderItems, 9000L, 1000L, fixedTime
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
                1L, 1L, "아메리카노", 4500L, 2
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
            val orderItems = listOf(
                OrderItem(1L, 1L, "아메리카노", 4500L, 2),
                OrderItem(2L, 2L, "라떼", 5000L, 1)
            )
            val order = Order(
                1L, 1L, 1L, orderItems, 14000L, 2000L, fixedTime
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
            val products = listOf(
                OrderService.CreateOrderInput.ProductWithQuantity(1L, 2),
                OrderService.CreateOrderInput.ProductWithQuantity(2L, 1)
            )
            val input = OrderService.CreateOrderInput(1L, products, 1L)

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
            val products = listOf(
                OrderService.CreateOrderInput.ProductWithQuantity(1L, 1)
            )
            val input = OrderService.CreateOrderInput(1L, products, null)

            // then
            input.userId shouldBe 1L
            input.products.size shouldBe 1
            input.userCouponId shouldBe null
            input.products[0].productId shouldBe 1L
            input.products[0].quantity shouldBe 1
        }
    }
} 