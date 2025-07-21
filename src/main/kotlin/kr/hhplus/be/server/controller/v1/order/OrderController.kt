package kr.hhplus.be.server.controller.v1.order

import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.order.request.PostOrderRequestBody
import kr.hhplus.be.server.controller.v1.order.response.OrderItemResponse
import kr.hhplus.be.server.controller.v1.order.response.PostOrderResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order API", description = "주문 API")
class OrderController
    : OrderApiSpec {

    @PostMapping("")
    override fun createOrder(
        @RequestHeader userId: Long,
        @RequestBody request: PostOrderRequestBody
    ): ResponseEntity<PostOrderResponse> = ResponseEntity.ok(PostOrderResponse(
        orderId = 1,
        totalAmount = 40000,
        usedPoint = 37000,
        couponDiscountAmount = 3000,
        orderItems = listOf(
            OrderItemResponse(
                productId = 1,
                productName = "사탕",
                unitPrice = 8000,
                quantity = 5,
                totalPrice = 40000,
            )
        )
    ))
}