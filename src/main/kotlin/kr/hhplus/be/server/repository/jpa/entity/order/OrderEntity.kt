package kr.hhplus.be.server.repository.jpa.entity.order

import jakarta.persistence.*
import kr.hhplus.be.server.repository.jpa.entity.BaseEntity
import kr.hhplus.be.server.repository.jpa.entity.coupon.UserCouponEntity
import kr.hhplus.be.server.repository.jpa.entity.user.UserEntity
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.entity.OrderItem
import kr.hhplus.be.server.util.TimeProvider
import org.hibernate.annotations.SQLRestriction
import java.time.Instant

/**
 * 주문 정보를 저장하는 JPA Entity
 */
@Entity
@Table(name = "`order`")
@SQLRestriction("deleted_at IS NULL") // Soft Delete: 삭제된 행 제외
class OrderEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_coupon_id")
    var userCoupon: UserCouponEntity? = null,

    @Column(name = "ordered_at", nullable = false)
    val orderedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val orderItems: MutableList<OrderItemEntity> = mutableListOf()
) : BaseEntity() {
    
    /**
     * 주문 항목 추가
     */
    fun addOrderItem(orderItem: OrderItemEntity) {
        orderItems.add(orderItem)
        orderItem.setOrder(this)
    }
    
    /**
     * 주문 총 금액 계산 (쿠폰 할인 적용 전)
     */
    fun getTotalAmount(): Long {
        return orderItems.sumOf { it.getTotalPrice() }
    }
    
    /**
     * 쿠폰 할인 금액 계산
     */
    fun getDiscountAmount(): Long {
        return userCoupon?.coupon?.discount ?: 0L
    }
    
    /**
     * 최종 결제 금액 계산 (쿠폰 할인 적용 후)
     */
    fun getFinalAmount(): Long {
        val totalAmount = getTotalAmount()
        val discountAmount = getDiscountAmount()
        return maxOf(0L, totalAmount - discountAmount)
    }
    
    /**
     * 쿠폰 적용
     */
    fun applyCoupon(userCoupon: UserCouponEntity) {
        require(userCoupon.canUse()) { "사용할 수 없는 쿠폰입니다" }
        require(userCoupon.user.id == this.user.id) { "다른 사용자의 쿠폰은 사용할 수 없습니다" }
        this.userCoupon = userCoupon
    }

    fun toDomain(timeProvider: TimeProvider): Order {
        val orderItems: List<OrderItem> = this.orderItems.map { orderItemEntity ->
            OrderItem(
                id = orderItemEntity.id,
                productId = orderItemEntity.product.id,
                productName = requireNotNull(orderItemEntity.product.name) {
                    "ProductEntity.name is null (ID=${orderItemEntity.product.id}). 상품명 데이터 정합성 확인 필요"
                },
                unitPrice = orderItemEntity.unitPrice,
                quantity = orderItemEntity.quantity
            )
        }

        val totalProductsPrice: Long = orderItems.sumOf { it.totalPrice }
        val discountedPrice: Long = this.userCoupon?.coupon?.discount ?: 0L

        val orderedAt = requireNotNull(this.orderedAt) {
            "OrderEntity.orderedAt is null (ID=${this.id}). 주문일시 데이터 무결성 확인 필요"
        }

        return Order(
            id = this.id,
            userId = this.user.id,
            userCouponId = this.userCoupon?.id,
            orderItems = orderItems,
            totalProductsPrice = totalProductsPrice,
            discountedPrice = discountedPrice,
            orderedAt = timeProvider.toZonedDateTime(orderedAt)
        )
    }
}