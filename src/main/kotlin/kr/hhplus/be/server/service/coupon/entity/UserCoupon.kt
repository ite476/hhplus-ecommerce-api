package kr.hhplus.be.server.service.coupon.entity

import kr.hhplus.be.server.service.coupon.exception.UserCouponCantBeUsedException
import kr.hhplus.be.server.service.coupon.exception.UserCouponIsNotUsedButTriedToBeUnusedException
import java.time.ZonedDateTime

class UserCoupon (
    val id: Long,
    val userId: Long,
    val couponId: Long,
    val couponName: String,
    val discount: Long,
    status: UserCouponStatus,

    val issuedAt: ZonedDateTime,
    usedAt: ZonedDateTime?,
    val validUntil: ZonedDateTime
) {
    var status: UserCouponStatus = status
        private set

    private fun updateStatus(now: ZonedDateTime) {
        status = when {
            usedAt != null -> UserCouponStatus.USED
            now > validUntil -> UserCouponStatus.EXPIRED
            else -> UserCouponStatus.ACTIVE
        }
    }

    var usedAt: ZonedDateTime? = usedAt
        private set

    fun use(now: ZonedDateTime){
        if (status != UserCouponStatus.ACTIVE) {
            throw UserCouponCantBeUsedException()
        }

        usedAt = now
        updateStatus(now)
    }

    fun undoUsage(now: ZonedDateTime){
        if (status != UserCouponStatus.USED){
            throw UserCouponIsNotUsedButTriedToBeUnusedException()
        }

        usedAt = null
        updateStatus(now)
    }
}

enum class UserCouponStatus {
    ACTIVE,
    USED,
    EXPIRED
}