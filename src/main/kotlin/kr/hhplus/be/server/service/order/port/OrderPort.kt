package kr.hhplus.be.server.service.order.port

import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.product.entity.ProductSale
import kr.hhplus.be.server.service.user.entity.User
import java.time.ZonedDateTime

interface OrderPort {
    fun cancelOrder(order: Order)
    fun createOrder(
        user: User,
        userCouponId: Long?,
        productsStamp: List<ProductSale>,
        now: ZonedDateTime
    ) : Order

}