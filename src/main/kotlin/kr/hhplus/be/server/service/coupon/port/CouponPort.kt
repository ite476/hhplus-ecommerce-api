package kr.hhplus.be.server.service.coupon.port

import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions

interface CouponPort {
    fun findUserCouponById(userId: Long, userCouponId: Long): UserCoupon
    fun saveUserCoupon(userCoupon: UserCoupon)
    fun findAllUserCoupons(userId: Long) : List<UserCoupon>
    fun issueCoupon(couponId: Long) : UserCoupon
    fun revokeCoupon(issuedUserCoupon: kr.hhplus.be.server.service.coupon.entity.UserCoupon)
    fun existsUserCoupon(userCouponId: Long) : Boolean
    fun existsCoupon(couponId: Long): Boolean
    fun findPagedUserCoupons(userId: Long, pagingOptions: PagingOptions): PagedList<UserCoupon>
}
