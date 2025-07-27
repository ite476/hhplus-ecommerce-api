package kr.hhplus.be.server.service.coupon.usecase

import kr.hhplus.be.server.service.coupon.entity.UserCoupon

interface FindAllUserCouponsUsecase {
    fun findAllUserCoupons(userId: Long) : List<UserCoupon>
}
