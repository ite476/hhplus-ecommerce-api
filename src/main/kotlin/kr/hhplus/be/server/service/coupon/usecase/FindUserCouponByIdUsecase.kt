package kr.hhplus.be.server.service.coupon.usecase

import kr.hhplus.be.server.service.coupon.entity.UserCoupon

interface FindUserCouponByIdUsecase {
    fun findUserCouponById(
        userId: Long,
        userCouponId: Long
    ) : UserCoupon
}
