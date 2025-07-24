package kr.hhplus.be.server.repository.order

import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.port.OrderPort
import kr.hhplus.be.server.service.product.entity.ProductSale
import kr.hhplus.be.server.service.user.entity.User
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OrderTemporaryAdapter : OrderPort {
    override fun cancelOrder(order: Order) {
        TODO("Not yet implemented")
    }

    override fun createOrder(
        user: User,
        userCouponId: Long?,
        productsStamp: List<ProductSale>,
        now: ZonedDateTime
    ): Order {
        TODO("Not yet implemented")
    }
}