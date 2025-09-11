package kr.hhplus.be.server.service.coupon.event

import kr.hhplus.be.server.service.event.DomainEvent

/**
 * 쿠폰 재고가 부족함을 나타내는 도메인 이벤트.
 */
data class CouponOutOfStock(
    val couponId: Long
) : DomainEvent()


