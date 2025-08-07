package kr.hhplus.be.server.service.coupon.usecase

import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import java.time.ZonedDateTime

interface UseUserCouponUsecase {
    fun useUserCoupon(userCoupon: UserCoupon, now: ZonedDateTime)

    fun rollbackUserCouponUsage(userCoupon: UserCoupon, now: ZonedDateTime)
}
