package kr.hhplus.be.server.controller.v1.coupon

import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.coupon.response.GetMyCouponsResponse
import kr.hhplus.be.server.controller.v1.coupon.response.MyCouponInfo
import kr.hhplus.be.server.controller.v1.coupon.response.PostCouponIssueResponse
import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.usecase.FindAllUserCouponsUsecase
import kr.hhplus.be.server.service.coupon.usecase.IssueCouponUsecase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Coupon API", description = "쿠폰 API")
class CouponController(
    val findAllUserCouponsUsecase: FindAllUserCouponsUsecase,
    val issueCouponUsecase: IssueCouponUsecase
    ) : CouponApiSpec {

    @GetMapping("/mycoupons")
    override fun getMyCoupons(
        @RequestHeader userId: Long
    ): ResponseEntity<GetMyCouponsResponse> {
        val userCoupons: List<UserCoupon> = findAllUserCouponsUsecase.findAllUserCoupons(userId)

        val couponsRespone = GetMyCouponsResponse(
            coupons = userCoupons.map {
                MyCouponInfo(
                    userCouponId = it.id,
                    couponName = it.couponName,
                    discountAmount = it.discount,
                    isUsable = it.isUsable(),
                    issuedAt = it.issuedAt,
                    validUntil = it.validUntil,
                    usedAt = it.usedAt,
                )
            }
        )

        return ResponseEntity.ok(couponsRespone)
    }

    @PostMapping("/coupons/{couponId}")
    override suspend fun issueCoupon(
        @RequestHeader userId: Long,
        @PathVariable couponId: Long
    ): ResponseEntity<PostCouponIssueResponse> {
        val issuedCoupon: UserCoupon = issueCouponUsecase.issueCoupon(userId, couponId);

        val issuedCouponResponse = PostCouponIssueResponse(
            couponId = issuedCoupon.id,
            couponName = issuedCoupon.couponName,
            discountAmount = issuedCoupon.discount,
            validUntil = issuedCoupon.validUntil,
            issuedAt = issuedCoupon.issuedAt,
        )

        return ResponseEntity.ok(issuedCouponResponse)
    }
}