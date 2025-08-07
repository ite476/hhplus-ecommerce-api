package kr.hhplus.be.server.controller.v1.order.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty

@Schema(description = "주문 생성 요청")
data class PostOrderRequestBody(
    @field:Schema(description = "주문 상품 목록")
    @field:NotEmpty
    @field:Valid
    val orderItems: List<OrderItemRequest>,
    @field:Schema(description = "사용할 쿠폰 ID (선택적)", example = "1", required = false)
    val userCouponId: Long? = null
)

@Schema(description = "주문 상품 정보")
data class OrderItemRequest(
    @field:Schema(description = "상품 ID", example = "1")
    val productId: Long,
    @field:Schema(description = "주문 수량", example = "2")
    @field:Min(1L)
    val quantity: Long
) 