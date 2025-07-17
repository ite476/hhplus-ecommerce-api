package kr.hhplus.be.server.controller.v1.coupon.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "쿠폰 발급 요청")
data class PostCouponIssueRequestBody(
    @field:Schema(description = "발급받을 쿠폰 ID", example = "1")
    val couponId: Long
) 