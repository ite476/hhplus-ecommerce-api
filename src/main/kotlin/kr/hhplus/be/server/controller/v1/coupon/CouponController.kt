package kr.hhplus.be.server.controller.v1.coupon

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.coupon.response.PostCouponIssueResponse
import kr.hhplus.be.server.controller.v1.coupon.response.GetMyCouponsResponse
import kr.hhplus.be.server.controller.v1.coupon.response.MyCouponInfo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Coupon API", description = "쿠폰 API")
class CouponController {

    @GetMapping("/mycoupons")
    @Operation(
        summary = "내 쿠폰 목록 조회",
        description = "회원이 보유한 쿠폰 목록을 조회합니다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                name = "내 쿠폰 목록 조회 성공 응답",
                                value = """{
                                    "coupons": [
                                        {
                                            "couponId": 1,
                                            "couponName": "신규가입 할인쿠폰",
                                            "discountAmount": 2000,
                                            "isUsable": true,
                                            "expirationDate": "2024-12-31",
                                            "issuedAt": "2024-01-15T10:30:00+09:00",
                                            "usedAt": null
                                        },
                                        {
                                            "couponId": 2,
                                            "couponName": "첫 주문 할인쿠폰",
                                            "discountAmount": 3000,
                                            "isUsable": false,
                                            "expirationDate": "2024-12-31",
                                            "issuedAt": "2024-01-10T09:00:00+09:00",
                                            "usedAt": "2024-01-20T14:15:00+09:00"
                                        }
                                    ]
                                }"""
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "회원이 존재하지 않습니다.",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                name = "회원 없음 오류",
                                value = """{
                                    "message": "회원이 존재하지 않습니다."
                                }"""
                            )
                        ]
                    )
                ]
            )
        ]
    )
    fun getMyCoupons(
        @RequestHeader userId: Long
    ): ResponseEntity<GetMyCouponsResponse> = ResponseEntity.ok(null)

    @PostMapping("/coupon/{couponId}")
    @Operation(
        summary = "쿠폰 발급",
        description = "지정된 쿠폰을 회원에게 발급합니다.",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "쿠폰 발급 성공",
                headers = [
                    Header(name = "Location", description = "발급된 쿠폰 목록 조회 URL", example = "/api/v1/mycoupons")
                ],
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                name = "쿠폰 발급 성공 응답",
                                value = """{
                                    "couponId": 1,
                                    "couponName": "신규가입 할인쿠폰",
                                    "discountAmount": 2000,
                                    "expirationDate": "2024-12-31",
                                    "issuedAt": "2024-01-15T10:30:00+09:00"
                                }"""
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "회원이 존재하지 않습니다.",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                name = "회원 없음 오류",
                                value = """{
                                    "message": "회원이 존재하지 않습니다."
                                }"""
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "409",
                description = "이미 발급받은 쿠폰입니다.",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                name = "중복 발급 오류",
                                value = """{
                                    "message": "이미 발급받은 쿠폰입니다."
                                }"""
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "422",
                description = "쿠폰 재고가 부족합니다.",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                name = "재고 부족 오류",
                                value = """{
                                    "message": "쿠폰 재고가 부족합니다."
                                }"""
                            )
                        ]
                    )
                ]
            )
        ]
    )
    fun issueCoupon(
        @RequestHeader userId: Long,
        @PathVariable couponId: Long
    ): ResponseEntity<PostCouponIssueResponse> = ResponseEntity.ok(null)
}