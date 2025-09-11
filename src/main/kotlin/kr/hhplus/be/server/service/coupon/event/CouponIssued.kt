package kr.hhplus.be.server.service.coupon.event

import kr.hhplus.be.server.service.event.DomainEvent

/**
 * 쿠폰이 성공적으로 발급되었음을 나타내는 도메인 이벤트.
 */
data class CouponIssued(
    val couponId: Long,
    val userId: Long,
    val issuedUserCouponId: Long
) : DomainEvent()


