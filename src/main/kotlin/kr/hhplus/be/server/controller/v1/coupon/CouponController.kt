package kr.hhplus.be.server.controller.v1.coupon

import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.coupon.response.GetMyCouponsResponse
import kr.hhplus.be.server.controller.v1.coupon.response.MyCouponInfo
import kr.hhplus.be.server.controller.v1.coupon.response.PostCouponIssueResponse
import kr.hhplus.be.server.service.coupon.entity.UserCouponStatus
import kr.hhplus.be.server.service.coupon.service.CouponService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Coupon API", description = "쿠폰 API")
class CouponController(
    val couponService: CouponService
    ) : CouponApiSpec {

    @GetMapping("/mycoupons")
    override fun getMyCoupons(
        @RequestHeader userId: Long
    ): ResponseEntity<GetMyCouponsResponse> {
        val userCoupons = couponService.readUserCoupons(userId)

        val couponsRespone =
            ResponseEntity.ok(GetMyCouponsResponse(
                coupons = userCoupons.map {
                    MyCouponInfo(
                        it.id,
                        it.couponName,
                        it.discount,
                        it.status == UserCouponStatus.ACTIVE,
                        it.issuedAt,
                        it.validUntil,
                        it.usedAt,
                    )
                }
            )
        )

        return couponsRespone
    }

    @PostMapping("/coupon/{couponId}")
    override suspend fun issueCoupon(
        @RequestHeader userId: Long,
        @PathVariable couponId: Long
    ): ResponseEntity<PostCouponIssueResponse> {
        val issuedCoupon = couponService.issueCoupon(userId, couponId);

        val issuedCouponResponse = PostCouponIssueResponse(
            issuedCoupon.id,
            issuedCoupon.couponName,
            issuedCoupon.discount,
            issuedCoupon.validUntil,
            issuedCoupon.issuedAt,
        )

        return ResponseEntity.ok(issuedCouponResponse)
    }
}