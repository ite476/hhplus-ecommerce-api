package kr.hhplus.be.server.controller.v1.order

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kr.hhplus.be.server.controller.v1.order.request.PostOrderRequestBody
import kr.hhplus.be.server.controller.v1.order.response.PostOrderResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

interface OrderApiSpec {
    @Operation(
        summary = "주문 생성",
        description = "상품을 주문하고 결제를 처리합니다.",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "주문 생성 성공",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                name = "주문 생성 성공 응답",
                                value = """{
                                    "orderId": 12345,
                                    "totalAmount": 15000,
                                    "usedPoint": 15000,
                                    "couponDiscountAmount": 2000,
                                    "orderItems": [
                                        {
                                            "productId": 1,
                                            "productName": "아메리카노",
                                            "unitPrice": 4500,
                                            "quantity": 2,
                                            "totalPrice": 9000
                                        },
                                        {
                                            "productId": 2,
                                            "productName": "라떼",
                                            "unitPrice": 5000,
                                            "quantity": 1,
                                            "totalPrice": 5000
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
            ),
            ApiResponse(
                responseCode = "422",
                description = "주문 처리 불가 (재고 부족, 포인트 부족, 쿠폰 사용 불가)",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                name = "재고 부족 오류",
                                value = """{
                                    "message": "상품 재고가 부족합니다."
                                }"""
                            ),
                            ExampleObject(
                                name = "포인트 부족 오류",
                                value = """{
                                    "message": "포인트 잔액이 부족합니다."
                                }"""
                            ),
                            ExampleObject(
                                name = "쿠폰 사용 불가 오류",
                                value = """{
                                    "message": "쿠폰을 사용할 수 없습니다."
                                }"""
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "주문 처리 실패",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                name = "시스템 오류",
                                value = """{
                                    "message": "주문 처리 중 오류가 발생했습니다."
                                }"""
                            )
                        ]
                    )
                ]
            )
        ]
    )
    fun createOrder(
        @RequestHeader userId: Long,
        @RequestBody body: PostOrderRequestBody
    ): ResponseEntity<PostOrderResponse>
}