package kr.hhplus.be.server.controller.v1.order

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import kr.hhplus.be.server.controller.advise.GlobalExceptionHandler
import kr.hhplus.be.server.controller.v1.order.request.OrderItemRequest
import kr.hhplus.be.server.controller.v1.order.request.PostOrderRequestBody
import kr.hhplus.be.server.controller.v1.order.response.PostOrderResponse
import kr.hhplus.be.server.service.coupon.exception.UserCouponCantBeUsedException
import kr.hhplus.be.server.service.exception.BusinessUnacceptableException
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.entity.OrderItem
import kr.hhplus.be.server.service.order.usecase.CreateOrderUsecase
import kr.hhplus.be.server.service.product.exception.LackOfProductStockException
import kr.hhplus.be.server.service.user.exception.UserNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.ZoneId
import java.time.ZonedDateTime

@WebMvcTest(OrderController::class)
@Import(GlobalExceptionHandler::class)
@DisplayName("OrderController 테스트")
class OrderControllerTest {
    @MockkBean
    lateinit var createOrderUsecase: CreateOrderUsecase

    private val postOrderEndpoint = "/api/v1/orders"
    private val fixedTime = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("Asia/Seoul"))

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

    @Test
    @DisplayName("정상적인 주문 요청 (쿠폰 없음) - 주문이 성공적으로 생성되고 올바른 응답을 반환한다")
    fun createOrder_ValidRequestWithoutCoupon_ReturnsSuccessResponse() {
        // Given
        val userId = 1L
        val requestBody = PostOrderRequestBody(
            orderItems = listOf(
                OrderItemRequest(productId = 1L, quantity = 2),
                OrderItemRequest(productId = 2L, quantity = 1)
            ),
            userCouponId = null
        )

        val mockOrder = Order(
            id = 12345L,
            userId = userId,
            userCouponId = null,
            orderItems = listOf(
                OrderItem(
                    id = 1L,
                    productId = 1L,
                    productName = "아메리카노",
                    unitPrice = 4500L,
                    quantity = 2
                ),
                OrderItem(
                    id = 2L,
                    productId = 2L,
                    productName = "라떼",
                    unitPrice = 5000L,
                    quantity = 1
                )
            ),
            totalProductsPrice = 14000L,
            discountedPrice = 0L,
            orderedAt = fixedTime
        )

        coEvery { createOrderUsecase.createOrder(any()) } returns mockOrder

        // When & Then
        val response = restClient.post()
            .uri(postOrderEndpoint)
            .header("userId", userId.toString())
            .body(requestBody)
            .retrieve()
            .toEntity(PostOrderResponse::class.java)

        response.statusCode shouldBe HttpStatus.CREATED
        response.body.let { body ->
            body shouldNotBe null
            body?.run {
                orderId shouldBe mockOrder.id
                totalAmount shouldBe mockOrder.totalProductsPrice
                usedPoint shouldBe mockOrder.purchasedPrice
                couponDiscountAmount shouldBe mockOrder.discountedPrice
                orderItems.size shouldBe mockOrder.orderItems.size
                orderItems[0].productId shouldBe mockOrder.orderItems[0].productId
                orderItems[0].productName shouldBe mockOrder.orderItems[0].productName
                orderItems[0].unitPrice shouldBe mockOrder.orderItems[0].unitPrice
                orderItems[0].quantity shouldBe mockOrder.orderItems[0].quantity
            }
        }

        coVerify(exactly = 1) {
            createOrderUsecase.createOrder(
                withArg { input ->
                    assert(input.userId == userId)
                    assert(input.products.size == 2)
                    assert(input.products[0].productId == 1L)
                    assert(input.products[0].quantity == 2L)
                    assert(input.userCouponId == null)
                }
            )
        }
    }

    @Test
    @DisplayName("쿠폰을 포함한 주문 요청 - 쿠폰 할인이 적용된 주문이 성공적으로 생성된다")
    fun createOrder_ValidRequestWithCoupon_ReturnsSuccessResponseWithDiscount() {
        // Given
        val userId = 1L
        val couponId = 100L
        val orderItems = listOf(OrderItemRequest(1L, 1))
        val requestBody = PostOrderRequestBody(orderItems, couponId)

        val mockOrder = Order(
            id = 12346L,
            userId = userId,
            userCouponId = couponId,
            orderItems = listOf(
                OrderItem(
                    id = 1L,
                    productId = 1L,
                    productName = "아메리카노",
                    unitPrice = 4500L,
                    quantity = 1
                )
            ),
            totalProductsPrice = 4500L,
            discountedPrice = 2000L,
            orderedAt = ZonedDateTime.now()
        )

        coEvery { createOrderUsecase.createOrder(any()) } returns mockOrder

        // When & Then
        val response = restClient.post()
            .uri(postOrderEndpoint)
            .header("userId", userId.toString())
            .body(requestBody)
            .retrieve()
            .toEntity(PostOrderResponse::class.java)

        response.statusCode shouldBe HttpStatus.CREATED
        response.body.let { body ->
            body shouldNotBe null
            body?.run {
                orderId shouldBe 12346L
                totalAmount shouldBe 4500L
                usedPoint shouldBe 2500L  // purchasedPrice = 4500 - 2000
                couponDiscountAmount shouldBe 2000L  // discountedPrice
            }
        }

        coVerify(exactly = 1) {
            createOrderUsecase.createOrder(
                withArg { input ->
                    assert(input.userCouponId == couponId)
                }
            )
        }
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 주문 요청 - 404 Not Found 오류를 반환한다")
    fun createOrder_UserNotFound_Returns404Error() {
        // Given
        val userId = 999L
        val orderItems = listOf(OrderItemRequest(1L, 1))
        val requestBody = PostOrderRequestBody(orderItems, null)

        coEvery { createOrderUsecase.createOrder(any()) } throws UserNotFoundException()

        // When & Then
        try {
            restClient.post()
                .uri(postOrderEndpoint)
                .header("userId", userId.toString())
                .body(requestBody)
                .retrieve()
                .toEntity(PostOrderResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.NOT_FOUND
            e.responseBodyAsString.contains("회원이 존재하지 않습니다.") shouldBe true
        }
    }

    @Test
    @DisplayName("재고가 부족한 상품을 주문 - 422 Unprocessable Entity 오류를 반환한다")
    fun createOrder_InsufficientStock_Returns422Error() {
        // Given
        val userId = 1L
        val orderItems = listOf(OrderItemRequest(1L, 100))
        val requestBody = PostOrderRequestBody(orderItems, null)

        coEvery { createOrderUsecase.createOrder(any()) } throws LackOfProductStockException()

        // When & Then
        try {
            restClient.post()
                .uri(postOrderEndpoint)
                .header("userId", userId.toString())
                .body(requestBody)
                .retrieve()
                .toEntity(PostOrderResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.UNPROCESSABLE_ENTITY
            e.responseBodyAsString.contains("상품 재고가 부족합니다.") shouldBe true
        }
    }

    @Test
    @DisplayName("포인트가 부족한 사용자가 주문 - 422 Unprocessable Entity 오류를 반환한다")
    fun createOrder_InsufficientPoint_Returns422Error() {
        // Given
        val userId = 1L
        val orderItems = listOf(OrderItemRequest(1L, 1))
        val requestBody = PostOrderRequestBody(orderItems, null)

        // 포인트 부족은 일반적인 BusinessUnacceptableException으로 처리됨
        coEvery { createOrderUsecase.createOrder(any()) } throws BusinessUnacceptableException("포인트 잔액이 부족합니다.")

        // When & Then
        try {
            restClient.post()
                .uri(postOrderEndpoint)
                .header("userId", userId.toString())
                .body(requestBody)
                .retrieve()
                .toEntity(PostOrderResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.UNPROCESSABLE_ENTITY
            e.responseBodyAsString.contains("포인트 잔액이 부족합니다.") shouldBe true
        }
    }

    @Test
    @DisplayName("사용할 수 없는 쿠폰으로 주문 - 409 Conflict 오류를 반환한다")
    fun createOrder_InvalidCoupon_Returns409Error() {
        // Given
        val userId = 1L
        val orderItems = listOf(OrderItemRequest(1L, 1))
        val requestBody = PostOrderRequestBody(orderItems, 999L)

        coEvery { createOrderUsecase.createOrder(any()) } throws UserCouponCantBeUsedException()

        // When & Then
        try {
            restClient.post()
                .uri(postOrderEndpoint)
                .header("userId", userId.toString())
                .body(requestBody)
                .retrieve()
                .toEntity(PostOrderResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.CONFLICT  // UserCouponCantBeUsedException -> BusinessConflictException -> 409
            e.responseBodyAsString.contains("사용할 수 없는 쿠폰입니다.") shouldBe true
        }
    }

    @Test
    @DisplayName("빈 주문 상품 목록으로 요청 - 400 Bad Request를 반환한다")
    fun createOrder_EmptyOrderItems_Returns400Error() {
        // Given
        val userId = 1L
        val invalidRequestBody = PostOrderRequestBody(emptyList(), null)

        // Mock 설정하지 않음 - validation이 먼저 동작해야 함

        // When & Then
        try {
            restClient.post()
                .uri(postOrderEndpoint)
                .header("userId", userId.toString())
                .body(invalidRequestBody)
                .retrieve()
                .toEntity(PostOrderResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    @DisplayName("주문 수량이 0인 경우 - 400 Bad Request를 반환한다")
    fun createOrder_ZeroQuantity_Returns400Error() {
        // Given
        val userId = 1L
        val invalidRequestBody = PostOrderRequestBody(
            listOf(OrderItemRequest(1L, 0)), null
        )

        // Mock 설정하지 않음 - validation이 먼저 동작해야 함

        // When & Then
        try {
            restClient.post()
                .uri(postOrderEndpoint)
                .header("userId", userId.toString())
                .body(invalidRequestBody)
                .retrieve()
                .toEntity(PostOrderResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    @DisplayName("userId 헤더가 누락된 경우 - 400 Bad Request를 반환한다")
    fun createOrder_MissingUserIdHeader_Returns400Error() {
        // Given
        val orderItems = listOf(OrderItemRequest(1L, 1))
        val requestBody = PostOrderRequestBody(orderItems, null)

        // When & Then
        try {
            restClient.post()
                .uri(postOrderEndpoint)
                .body(requestBody)
                .retrieve()
                .toEntity(PostOrderResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    @DisplayName("시스템 오류가 발생한 경우 - 500 Internal Server Error를 반환한다")
    fun createOrder_SystemError_Returns500Error() {
        // Given
        val userId = 1L
        val orderItems = listOf(OrderItemRequest(1L, 1))
        val requestBody = PostOrderRequestBody(orderItems, null)

        coEvery { createOrderUsecase.createOrder(any()) } throws RuntimeException("시스템 오류가 발생했습니다.")

        // When & Then
        try {
            restClient.post()
                .uri(postOrderEndpoint)
                .header("userId", userId.toString())
                .body(requestBody)
                .retrieve()
                .toEntity(PostOrderResponse::class.java)
        } catch (e: RestClientResponseException) {
            e.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}
