package kr.hhplus.be.server.controller.v1.order.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.service.order.entity.Order

@Schema(description = "주문 생성 응답")
data class PostOrderResponse(
    @field:Schema(description = "주문 ID", example = "12345")
    val orderId: Long,
    @field:Schema(description = "총 주문 금액 (원)", example = "15000")
    val totalAmount: Long,
    @field:Schema(description = "사용된 포인트 (원)", example = "15000")
    val usedPoint: Long,
    @field:Schema(description = "쿠폰 할인 금액 (원)", example = "2000")
    val couponDiscountAmount: Long,
    @field:Schema(description = "주문 상품 목록")
    val orderItems: List<OrderItemResponse>
) {
    companion object {
        fun fromEntity(order: Order): PostOrderResponse {
            // 엔티티 persist 보증
            val persistedOrderId: Long = requireNotNull(value = order.id) { "주문이 정상적으로 생성되지 않았습니다." }

            return PostOrderResponse(
                orderId = persistedOrderId,
                totalAmount = order.totalProductsPrice,
                usedPoint = order.purchasedPrice,
                couponDiscountAmount = order.discountedPrice,
                orderItems = order.orderItems.map {
                    OrderItemResponse(
                        productId = it.productId,
                        productName = it.productName,
                        unitPrice = it.unitPrice,
                        quantity = it.quantity,
                        totalPrice = it.totalPrice,
                    )
                }
            )
        }
    }
}

@Schema(description = "주문 상품 응답 정보")
data class OrderItemResponse(
    @field:Schema(description = "상품 ID", example = "1")
    val productId: Long,
    @field:Schema(description = "상품명", example = "아메리카노")
    val productName: String,
    @field:Schema(description = "상품 단가 (원)", example = "4500")
    val unitPrice: Long,
    @field:Schema(description = "주문 수량", example = "2")
    val quantity: Long,
    @field:Schema(description = "주문 상품 총 금액 (원)", example = "9000")
    val totalPrice: Long
) 