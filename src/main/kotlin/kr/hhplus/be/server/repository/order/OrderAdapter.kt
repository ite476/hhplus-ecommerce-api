package kr.hhplus.be.server.repository.order

import kr.hhplus.be.server.repository.jpa.entity.order.OrderEntity
import kr.hhplus.be.server.repository.jpa.entity.order.OrderItemEntity
import kr.hhplus.be.server.repository.jpa.repository.coupon.UserCouponJpaRepository
import kr.hhplus.be.server.repository.jpa.repository.order.OrderJpaRepository
import kr.hhplus.be.server.repository.jpa.repository.product.ProductJpaRepository
import kr.hhplus.be.server.repository.jpa.repository.user.UserJpaRepository
import kr.hhplus.be.server.service.coupon.exception.UserCouponNotFoundException
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.port.OrderPort
import kr.hhplus.be.server.service.product.entity.ProductSale
import kr.hhplus.be.server.service.product.exception.ProductNotFoundException
import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.exception.UserNotFoundException
import kr.hhplus.be.server.util.KoreanTimeProvider
import kr.hhplus.be.server.util.unwrapOrThrow
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OrderAdapter(
    private val orderRepository: OrderJpaRepository,
    private val userRepository: UserJpaRepository,
    private val userCouponRepository: UserCouponJpaRepository,
    private val productRepository: ProductJpaRepository,
    val timeProvider: KoreanTimeProvider
) : OrderPort {
    override fun cancelOrder(order: Order) {
        val orderEntity = orderRepository.findById(requireNotNull(order.id) {
            "Order.id is null. 주문 ID가 필요합니다"
        }).unwrapOrThrow {
            error("Order ${order.id} not found. 주문 데이터 확인 필요")
        }

        // Soft Delete 적용
        val now = timeProvider.now()
        orderEntity.delete(now.toInstant())
        orderRepository.save(orderEntity)
    }

    override fun createOrder(
        user: User, 
        userCouponId: Long?, 
        productsStamp: List<ProductSale>, 
        now: ZonedDateTime
    ): Order {
        val userEntity = userRepository.findById(user.id!!)
            .unwrapOrThrow { UserNotFoundException() }

        val userCouponEntity = userCouponId?.let { id ->
            userCouponRepository.findById(id)
                .unwrapOrThrow { UserCouponNotFoundException() }
        }

        val orderItems = productsStamp.map { sale ->
            val productEntity = productRepository.findById(sale.productId)
                .unwrapOrThrow { ProductNotFoundException() }

            OrderItemEntity(
                product = productEntity,
                unitPrice = sale.unitPrice,
                // 팔린 수
                quantity = sale.soldCount.toLong()
            )
        }

        val orderEntity = OrderEntity(
            user = userEntity,
            userCoupon = userCouponEntity,
            orderedAt = now.toInstant()
        )

        orderItems.forEach(orderEntity::addOrderItem)

        return orderRepository.save(orderEntity).toDomain(timeProvider)
    }
}