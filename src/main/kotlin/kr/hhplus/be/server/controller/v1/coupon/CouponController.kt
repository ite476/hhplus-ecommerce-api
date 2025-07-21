package kr.hhplus.be.server.controller.v1.coupon

import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.coupon.response.GetMyCouponsResponse
import kr.hhplus.be.server.controller.v1.coupon.response.MyCouponInfo
import kr.hhplus.be.server.controller.v1.coupon.response.PostCouponIssueResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Coupon API", description = "쿠폰 API")
class CouponController
    : CouponApiSpec {

    @GetMapping("/mycoupons")
    override fun getMyCoupons(
        @RequestHeader userId: Long
    ): ResponseEntity<GetMyCouponsResponse> = ResponseEntity.ok(
        GetMyCouponsResponse(
            coupons = listOf(
                MyCouponInfo(
                    couponId = 1,
                    couponName = "아무튼할인쿠폰",
                    discountAmount = 5000,
                    isUsable = true,
                    expirationDate = Date.from(
                        LocalDate.parse("2025-01-23")
                            .atStartOfDay(ZoneId.of("Asia/Seoul"))
                            .toInstant()
                    ),
                    issuedAt = ZonedDateTime.of(2025, 1, 23, 0, 0, 0, 0, ZoneOffset.of("+09:00")),
                    usedAt = ZonedDateTime.of(2025, 1, 23, 0, 0, 0, 0, ZoneOffset.of("+09:00")),
                ),
            ),
        )
    )

    @PostMapping("/coupon/{couponId}")
    override fun issueCoupon(
        @RequestHeader userId: Long,
        @PathVariable couponId: Long
    ): ResponseEntity<PostCouponIssueResponse> = ResponseEntity.ok(
        PostCouponIssueResponse(
            couponId = 1,
            couponName = "할인할인쿠폰",
            discountAmount = 3000,
            expirationDate = Date.from(
                LocalDate.parse("2025-01-23")
                    .atStartOfDay(ZoneId.of("Asia/Seoul"))
                    .toInstant()
            ),
            issuedAt = ZonedDateTime.of(2025, 1, 23, 0, 0, 0, 0, ZoneOffset.of("+09:00")),
        )
    )
}