package kr.hhplus.be.server.service.order.entity
import java.time.ZonedDateTime

class Order (
    val id: Long? = null,
    val userId: Long,
    val userCouponId: Long?,
    val orderItems: List<OrderItem>,
    val totalProductsPrice: Long,
    val discountedPrice: Long,
    val orderedAt: ZonedDateTime,
) {
    val purchasedPrice: Long
        get() = totalProductsPrice - discountedPrice
}