package kr.hhplus.be.server.controller.v1.order

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kr.hhplus.be.server.controller.advise.GlobalExceptionHandler
import kr.hhplus.be.server.controller.v1.order.request.OrderItemRequest
import kr.hhplus.be.server.controller.v1.order.request.PostOrderRequestBody
import kr.hhplus.be.server.service.exception.BusinessUnacceptableException
import kr.hhplus.be.server.service.coupon.exception.UserCouponCantBeUsedException
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.entity.OrderItem
import kr.hhplus.be.server.service.order.service.OrderService
import kr.hhplus.be.server.service.product.exception.LackOfProductStockException
import kr.hhplus.be.server.service.user.exception.UserNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.ZonedDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("OrderController 테스트")
class OrderControllerTest {

    private val orderService = mockk<OrderService>()
    private val objectMapper = ObjectMapper()
    private lateinit var mockMvc: MockMvc
    private val endpoint = "/api/v1/orders"

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        val controller = OrderController(orderService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    @DisplayName("정상적인 주문 요청 (쿠폰 없음) - 주문이 성공적으로 생성되고 올바른 응답을 반환한다")
    fun createOrder_ValidRequestWithoutCoupon_ReturnsSuccessResponse() {
        // Given
        val userId = 1L
        val orderItems = listOf(
            OrderItemRequest(1L, 2),
            OrderItemRequest(2L, 1)
        )
        val requestBody = PostOrderRequestBody(orderItems, null)

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
            orderedAt = ZonedDateTime.now()
        )

        coEvery { orderService.createOrder(any()) } returns mockOrder

        // When & Then
        mockMvc.perform(
            post(endpoint)
                .header("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.orderId").value(12345L))
            .andExpect(jsonPath("$.totalAmount").value(14000L))
            .andExpect(jsonPath("$.usedPoint").value(14000L))  // purchasedPrice
            .andExpect(jsonPath("$.couponDiscountAmount").value(0L))  // discountedPrice
            .andExpect(jsonPath("$.orderItems").isArray)
            .andExpect(jsonPath("$.orderItems.length()").value(2))
            .andExpect(jsonPath("$.orderItems[0].productId").value(1L))
            .andExpect(jsonPath("$.orderItems[0].productName").value("아메리카노"))
            .andExpect(jsonPath("$.orderItems[0].unitPrice").value(4500L))
            .andExpect(jsonPath("$.orderItems[0].quantity").value(2))
            .andExpect(jsonPath("$.orderItems[0].totalPrice").value(9000L))

        coVerify(exactly = 1) {
            orderService.createOrder(
                withArg { input ->
                    assert(input.userId == userId)
                    assert(input.products.size == 2)
                    assert(input.products[0].productId == 1L)
                    assert(input.products[0].quantity == 2)
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

        coEvery { orderService.createOrder(any()) } returns mockOrder

        // When & Then
        mockMvc.perform(
            post(endpoint)
                .header("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderId").value(12346L))
            .andExpect(jsonPath("$.totalAmount").value(4500L))
            .andExpect(jsonPath("$.usedPoint").value(2500L))  // purchasedPrice = 4500 - 2000
            .andExpect(jsonPath("$.couponDiscountAmount").value(2000L))  // discountedPrice

        coVerify(exactly = 1) {
            orderService.createOrder(
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

        coEvery { orderService.createOrder(any()) } throws UserNotFoundException()

        // When & Then
        mockMvc.perform(
            post(endpoint)
                .header("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("회원이 존재하지 않습니다."))
    }

    @Test
    @DisplayName("재고가 부족한 상품을 주문 - 422 Unprocessable Entity 오류를 반환한다")
    fun createOrder_InsufficientStock_Returns422Error() {
        // Given
        val userId = 1L
        val orderItems = listOf(OrderItemRequest(1L, 100))
        val requestBody = PostOrderRequestBody(orderItems, null)

        coEvery { orderService.createOrder(any()) } throws LackOfProductStockException()

        // When & Then
        mockMvc.perform(
            post(endpoint)
                .header("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.message").value("상품 재고가 부족합니다."))
    }

    @Test
    @DisplayName("포인트가 부족한 사용자가 주문 - 422 Unprocessable Entity 오류를 반환한다")
    fun createOrder_InsufficientPoint_Returns422Error() {
        // Given
        val userId = 1L
        val orderItems = listOf(OrderItemRequest(1L, 1))
        val requestBody = PostOrderRequestBody(orderItems, null)

        // 포인트 부족은 일반적인 BusinessUnacceptableException으로 처리됨
        coEvery { orderService.createOrder(any()) } throws BusinessUnacceptableException("포인트 잔액이 부족합니다.")

        // When & Then
        mockMvc.perform(
            post(endpoint)
                .header("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.message").value("포인트 잔액이 부족합니다."))
    }

    @Test
    @DisplayName("사용할 수 없는 쿠폰으로 주문 - 409 Conflict 오류를 반환한다")
    fun createOrder_InvalidCoupon_Returns409Error() {
        // Given
        val userId = 1L
        val orderItems = listOf(OrderItemRequest(1L, 1))
        val requestBody = PostOrderRequestBody(orderItems, 999L)

        coEvery { orderService.createOrder(any()) } throws UserCouponCantBeUsedException()

        // When & Then
        mockMvc.perform(
            post(endpoint)
                .header("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isConflict)  // UserCouponCantBeUsedException -> BusinessConflictException -> 409
            .andExpect(jsonPath("$.message").value("사용할 수 없는 쿠폰입니다."))
    }

    @Test
    @DisplayName("빈 주문 상품 목록으로 요청 - 400 Bad Request를 반환한다")
    fun createOrder_EmptyOrderItems_Returns400Error() {
        // Given
        val userId = 1L
        val invalidRequestBody = PostOrderRequestBody(emptyList(), null)

        // When & Then
        mockMvc.perform(
            post(endpoint)
                .header("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequestBody))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("주문 수량이 0인 경우 - 400 Bad Request를 반환한다")
    fun createOrder_ZeroQuantity_Returns400Error() {
        // Given
        val userId = 1L
        val invalidRequestBody = PostOrderRequestBody(
            listOf(OrderItemRequest(1L, 0)), null
        )

        // When & Then
        mockMvc.perform(
            post(endpoint)
                .header("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequestBody))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("userId 헤더가 누락된 경우 - 400 Bad Request를 반환한다")
    fun createOrder_MissingUserIdHeader_Returns400Error() {
        // Given
        val orderItems = listOf(OrderItemRequest(1L, 1))
        val requestBody = PostOrderRequestBody(orderItems, null)

        // When & Then
        mockMvc.perform(
            post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("시스템 오류가 발생한 경우 - 500 Internal Server Error를 반환한다")
    fun createOrder_SystemError_Returns500Error() {
        // Given
        val userId = 1L
        val orderItems = listOf(OrderItemRequest(1L, 1))
        val requestBody = PostOrderRequestBody(orderItems, null)

        coEvery { orderService.createOrder(any()) } throws RuntimeException("시스템 오류가 발생했습니다.")

        // When & Then
        mockMvc.perform(
            post(endpoint)
                .header("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isInternalServerError)
    }
}
