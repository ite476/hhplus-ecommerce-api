package kr.hhplus.be.server.service.coupon.service

import kr.hhplus.be.server.service.common.transaction.CompensationScope
import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.exception.CouponNotFoundException
import kr.hhplus.be.server.service.coupon.exception.UserCouponNotFoundException
import kr.hhplus.be.server.service.coupon.port.CouponPort
import kr.hhplus.be.server.service.user.service.UserService
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class CouponService (
    val couponPort: CouponPort,
    val userService: UserService,
    val timeProvider: KoreanTimeProvider
    ) {
    fun readSingleUserCoupon(
        userId: Long,
        userCouponId: Long
    ) : UserCoupon {
        requireUserCouponExists(userCouponId)

        return couponPort.findUserCouponById(userId, userCouponId)
    }

    fun useUserCoupon(userCoupon: UserCoupon, now: ZonedDateTime) {
        requireUserCouponExists(userCoupon.id)

        userCoupon.use(now)

        couponPort.saveUserCoupon(userCoupon)
    }

    fun rollbackUserCouponUsage(userCoupon: UserCoupon, now: ZonedDateTime) {
        requireUserCouponExists(userCoupon.id)

        userCoupon.undoUsage(now)

        couponPort.saveUserCoupon(userCoupon)
    }


    fun readUserCoupons(userId: Long) : List<UserCoupon> {
        userService.requireUserExists(userId)

        val userCoupons = couponPort.findAllUserCoupons(userId)

        return userCoupons
    }

    suspend fun issueCoupon(userId: Long, couponId: Long) : UserCoupon {
        userService.requireUserExists(userId)
        requireCouponExists(couponId)

        val now = timeProvider.now()
        val scope = CompensationScope()

        try{
            val couponIssueResult = scope.execute {
                couponPort.issueCoupon(couponId)
            }

            val issuedUserCoupon = couponIssueResult.result

            couponIssueResult.compensate {
                couponPort.revokeCoupon(issuedUserCoupon)
            }

            return issuedUserCoupon
        }
        catch (ex: Exception){
            scope.rollbackAll()

            throw ex
        }
    }

    fun existsUserCoupon(userCouponId: Long) : Boolean {
        return couponPort.existsUserCoupon(userCouponId)
    }

    fun requireUserCouponExists(userCouponId: Long) {
        if (!existsUserCoupon(userCouponId)){
            throw UserCouponNotFoundException()
        }
    }

    fun requireCouponExists(couponId: Long){
        if (!existsCoupon(couponId)){
            throw CouponNotFoundException()
        }
    }

    private fun existsCoupon(couponId: Long): Boolean {
        return couponPort.existsCoupon(couponId)
    }

}
