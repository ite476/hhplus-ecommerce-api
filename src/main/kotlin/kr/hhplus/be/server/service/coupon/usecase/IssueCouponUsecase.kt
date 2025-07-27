package kr.hhplus.be.server.service.coupon.usecase

import kr.hhplus.be.server.service.coupon.entity.UserCoupon

interface IssueCouponUsecase {
    suspend fun issueCoupon(userId: Long, couponId: Long) : UserCoupon
}
