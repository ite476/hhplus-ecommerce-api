package kr.hhplus.be.server.service.coupon.usecase

import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions

interface FindPagedUserCouponsUsecase {
    fun findPagedUserCoupons(userId: Long, pagingOptions: PagingOptions): PagedList<UserCoupon>
}
