package kr.hhplus.be.server.repository.coupon

import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.port.CouponPort
import org.springframework.stereotype.Component

@Component
class CouponTemporaryRepository : CouponPort {
    override fun findUserCouponById(
        userId: Long,
        userCouponId: Long
    ): UserCoupon {
        TODO("Not yet implemented")
    }

    override fun saveUserCoupon(userCoupon: UserCoupon) {
        TODO("Not yet implemented")
    }

    override fun findAllUserCoupons(userId: Long): List<UserCoupon> {
        TODO("Not yet implemented")
    }

    override fun issueCoupon(couponId: Long): UserCoupon {
        TODO("Not yet implemented")
    }

    override fun revokeCoupon(issuedUserCoupon: UserCoupon) {
        TODO("Not yet implemented")
    }

    override fun existsUserCoupon(userCouponId: Long) : Boolean {
        TODO("Not yet implemented")
    }

    override fun existsCoupon(couponId: Long): Boolean {
        TODO("Not yet implemented")
    }
}