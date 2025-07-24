package kr.hhplus.be.server.controller.v1.order

import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.order.request.PostOrderRequestBody
import kr.hhplus.be.server.controller.v1.order.response.OrderItemResponse
import kr.hhplus.be.server.controller.v1.order.response.PostOrderResponse
import kr.hhplus.be.server.service.order.service.OrderService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order API", description = "주문 API")
class OrderController(
    val orderService: OrderService
    ) : OrderApiSpec {

    @PostMapping("")
    override suspend fun createOrder(
        @RequestHeader userId: Long,
        @RequestBody body: PostOrderRequestBody
    ): ResponseEntity<PostOrderResponse> {
        val order = orderService.createOrder(OrderService.CreateOrderInput(
            userId,
            body.orderItems.map {
                OrderService.CreateOrderInput.ProductWithQuantity(
                    it.productId,
                    it.quantity
                )
            },
            body.couponId
        ))

        // 엔티티 persist 보증
        val persistedOrderId = requireNotNull(order.id) { "주문이 정상적으로 생성되지 않았습니다." }

        val orderResponse = PostOrderResponse(
            persistedOrderId,
            order.totalProductsPrice,
            order.purchasedPrice,
            order.discountedPrice,
            order.orderItems.map {
                OrderItemResponse(
                    it.productId,
                    it.productName,
                    it.unitPrice,
                    it.quantity,
                    it.totalPrice,
                )
            },
        )

        return ResponseEntity.ok(orderResponse)
    }
}