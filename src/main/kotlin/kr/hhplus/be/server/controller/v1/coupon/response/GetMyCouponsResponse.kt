package kr.hhplus.be.server.controller.v1.coupon.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.Date

@Schema(description = "내 쿠폰 목록 응답")
data class GetMyCouponsResponse(
    @field:Schema(description = "보유 쿠폰 목록")
    val coupons: List<MyCouponInfo>
)

@Schema(description = "내 쿠폰 정보")
data class MyCouponInfo(
    @field:Schema(description = "쿠폰 ID", example = "1")
    val couponId: Long,
    @field:Schema(description = "쿠폰명", example = "신규가입 할인쿠폰")
    val couponName: String,
    @field:Schema(description = "할인 금액 (원)", example = "2000")
    val discountAmount: Long,
    @field:Schema(description = "사용 가능 여부", example = "true")
    val isUsable: Boolean,
    @field:Schema(description = "쿠폰 만료일", example = "2024-12-31")
    val expirationDate: Date,
    @field:Schema(description = "발급일시", example = "2024-01-15T10:30:00")
    val issuedAt: ZonedDateTime,
    @field:Schema(description = "사용일시 (사용된 경우)", example = "2024-01-20T14:15:00", required = false)
    val usedAt: ZonedDateTime? = null
) 