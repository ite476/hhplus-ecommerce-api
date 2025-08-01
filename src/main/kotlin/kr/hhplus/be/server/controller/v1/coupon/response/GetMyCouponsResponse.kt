package kr.hhplus.be.server.controller.v1.coupon.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.pagination.PagedList
import java.time.ZonedDateTime

@Schema(description = "내 쿠폰 목록 응답")
data class GetMyCouponsResponse(
    val page: Int,
    val size: Int,
    val totalCount: Long,

    @field:Schema(description = "보유 쿠폰 목록")
    val coupons: List<MyCouponInfo>
) {
    companion object {
        fun fromEntity(userCoupons: PagedList<UserCoupon>): GetMyCouponsResponse {
            return GetMyCouponsResponse(
                coupons = userCoupons.items.map {
                    MyCouponInfo(
                        userCouponId = it.id,
                        couponName = it.couponName,
                        discountAmount = it.discount,
                        isUsable = it.isUsable(),
                        issuedAt = it.issuedAt,
                        validUntil = it.validUntil,
                        usedAt = it.usedAt,
                    )
                },
                page = userCoupons.page,
                size = userCoupons.size,
                totalCount = userCoupons.totalCount
            )
        }
    }
}

@Schema(description = "내 쿠폰 정보")
data class MyCouponInfo(
    @field:Schema(description = "쿠폰 ID", example = "1")
    val userCouponId: Long,
    @field:Schema(description = "쿠폰명", example = "신규가입 할인쿠폰")
    val couponName: String,
    @field:Schema(description = "할인 금액 (원)", example = "2000")
    val discountAmount: Long,
    @field:Schema(description = "사용 가능 여부", example = "true")
    val isUsable: Boolean,
    @field:Schema(description = "발급일시", example = "2024-01-15T10:30:00+09:00")
    val issuedAt: ZonedDateTime,
    @field:Schema(description = "쿠폰 만료일시", example = "2024-12-31T00:00:00+09:00")
    val validUntil: ZonedDateTime,
    @field:Schema(description = "사용일시 (사용된 경우)", example = "2024-01-20T14:15:00+09:00", required = false)
    val usedAt: ZonedDateTime? = null
) 