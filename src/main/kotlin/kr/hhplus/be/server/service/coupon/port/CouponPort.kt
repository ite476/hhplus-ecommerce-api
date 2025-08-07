package kr.hhplus.be.server.service.coupon.port

import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions
import java.time.ZonedDateTime

interface CouponPort {
    fun findUserCouponById(userId: Long, userCouponId: Long): UserCoupon
    fun saveUserCoupon(userCoupon: UserCoupon)
    fun issueCoupon(userId: Long, couponId: Long, now: ZonedDateTime) : UserCoupon
    fun revokeCoupon(issuedUserCoupon: UserCoupon, now: ZonedDateTime)
    fun existsUserCoupon(userCouponId: Long) : Boolean
    fun existsCoupon(couponId: Long): Boolean
    fun findPagedUserCoupons(userId: Long, pagingOptions: PagingOptions): PagedList<UserCoupon>
}
