package kr.hhplus.be.server.controller.v1.coupon

import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.runBlocking
import kr.hhplus.be.server.controller.dto.request.PagingOptionsRequestParam
import kr.hhplus.be.server.controller.v1.coupon.response.GetMyCouponsResponse
import kr.hhplus.be.server.controller.v1.coupon.response.PostCouponIssueResponse
import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.usecase.FindPagedUserCouponsUsecase
import kr.hhplus.be.server.service.coupon.usecase.IssueCouponUsecase
import kr.hhplus.be.server.service.pagination.PagedList
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Coupon API", description = "쿠폰 API")
class CouponController(
    val findAllUserCouponsUsecase: FindPagedUserCouponsUsecase,
    val issueCouponUsecase: IssueCouponUsecase
    ) : CouponApiSpec {

    @GetMapping("/mycoupons")
    override fun getMyCoupons(
        @RequestHeader userId: Long,
        @ParameterObject pagingOptions: PagingOptionsRequestParam
    ): ResponseEntity<GetMyCouponsResponse> {
        val userCoupons: PagedList<UserCoupon> = findAllUserCouponsUsecase.findPagedUserCoupons(userId, pagingOptions.toPagingOptions())

        val couponsRespone: GetMyCouponsResponse = GetMyCouponsResponse.fromEntity(userCoupons)

        return ResponseEntity.ok(couponsRespone)
    }

    @PostMapping("/coupons/{couponId}")
    override fun issueCoupon(
        @RequestHeader userId: Long,
        @PathVariable couponId: Long
    ): ResponseEntity<PostCouponIssueResponse> {
        val issuedCoupon: UserCoupon = runBlocking {
            issueCouponUsecase.issueCoupon(userId, couponId);
        }

        val issuedCouponResponse: PostCouponIssueResponse = PostCouponIssueResponse.fromEntity(issuedCoupon)

        return ResponseEntity
            .created(URI.create("/mycoupons"))
            .body(issuedCouponResponse)
    }
}