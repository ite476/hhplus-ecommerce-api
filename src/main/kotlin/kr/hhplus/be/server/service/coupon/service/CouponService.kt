package kr.hhplus.be.server.service.coupon.service

import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.exception.CouponNotFoundException
import kr.hhplus.be.server.service.coupon.exception.UserCouponNotFoundException
import kr.hhplus.be.server.service.coupon.port.CouponPort
import kr.hhplus.be.server.service.coupon.usecase.FindAllUserCouponsUsecase
import kr.hhplus.be.server.service.coupon.usecase.FindUserCouponByIdUsecase
import kr.hhplus.be.server.service.coupon.usecase.IssueCouponUsecase
import kr.hhplus.be.server.service.coupon.usecase.UseUserCouponUsecase
import kr.hhplus.be.server.service.transaction.CompensationScope
import kr.hhplus.be.server.service.user.usecase.RequiresUserIdExistsUsecase
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class CouponService (
    val couponPort: CouponPort,
    val requireUserIdExistsUsecase: RequiresUserIdExistsUsecase,
    val timeProvider: KoreanTimeProvider
    ) : FindUserCouponByIdUsecase,
    UseUserCouponUsecase,
    FindAllUserCouponsUsecase,
    IssueCouponUsecase {
    override fun findUserCouponById(
        userId: Long,
        userCouponId: Long
    ) : UserCoupon {
        requireUserCouponExists(userCouponId)

        return couponPort.findUserCouponById(userId, userCouponId)
    }

    override fun useUserCoupon(userCoupon: UserCoupon, now: ZonedDateTime) {
        requireUserCouponExists(userCoupon.id)

        userCoupon.use(now)

        couponPort.saveUserCoupon(userCoupon)
    }

    override fun rollbackUserCouponUsage(userCoupon: UserCoupon, now: ZonedDateTime) {
        requireUserCouponExists(userCoupon.id)

        userCoupon.undoUsage(now)

        couponPort.saveUserCoupon(userCoupon)
    }

    override fun findAllUserCoupons(userId: Long) : List<UserCoupon> {
        requireUserIdExistsUsecase.requireUserIdExists(userId)

        val userCoupons: List<UserCoupon> = couponPort.findAllUserCoupons(userId)

        return userCoupons
    }

    override suspend fun issueCoupon(userId: Long, couponId: Long) : UserCoupon {
        requireUserIdExistsUsecase.requireUserIdExists(userId)
        requireCouponExists(couponId)

        val issuedUserCoupon: UserCoupon = CompensationScope.runTransaction {
            execute {
                couponPort.issueCoupon(couponId)
            }.compensateBy { coupon ->
                couponPort.revokeCoupon(issuedUserCoupon = coupon)
            }
        }

        return issuedUserCoupon
    }


    private fun existsUserCoupon(userCouponId: Long) : Boolean {
        return couponPort.existsUserCoupon(userCouponId)
    }

    private fun requireUserCouponExists(userCouponId: Long) {
        if (!existsUserCoupon(userCouponId)){
            throw UserCouponNotFoundException()
        }
    }

    private fun requireCouponExists(couponId: Long){
        if (!existsCoupon(couponId)){
            throw CouponNotFoundException()
        }
    }

    private fun existsCoupon(couponId: Long): Boolean {
        return couponPort.existsCoupon(couponId)
    }

}
