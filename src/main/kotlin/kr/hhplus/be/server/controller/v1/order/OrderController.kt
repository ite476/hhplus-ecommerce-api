package kr.hhplus.be.server.controller.v1.order

import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.order.request.PostOrderRequestBody
import kr.hhplus.be.server.controller.v1.order.response.OrderItemResponse
import kr.hhplus.be.server.controller.v1.order.response.PostOrderResponse
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.service.OrderService
import kr.hhplus.be.server.service.order.usecase.CreateOrderUsecase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order API", description = "주문 API")
class OrderController(
    val createOrderUsecase: CreateOrderUsecase
    ) : OrderApiSpec {

    @PostMapping("")
    override suspend fun createOrder(
        @RequestHeader userId: Long,
        @RequestBody body: PostOrderRequestBody
    ): ResponseEntity<PostOrderResponse> {
        val order: Order = createOrderUsecase.createOrder(OrderService.CreateOrderInput(
            userId = userId,
            products = body.orderItems.map {
                OrderService.CreateOrderInput.ProductWithQuantity(
                    productId = it.productId,
                    quantity = it.quantity
                )
            },
            userCouponId = body.couponId
        ))

        // 엔티티 persist 보증
        val persistedOrderId: Long = requireNotNull(value = order.id) { "주문이 정상적으로 생성되지 않았습니다." }

        val orderResponse = PostOrderResponse(
            orderId = persistedOrderId,
            totalAmount = order.totalProductsPrice,
            usedPoint = order.purchasedPrice,
            couponDiscountAmount = order.discountedPrice,
            orderItems = order.orderItems.map {
                OrderItemResponse(
                    productId = it.productId,
                    productName = it.productName,
                    unitPrice = it.unitPrice,
                    quantity = it.quantity,
                    totalPrice = it.totalPrice,
                )
            }
        )

        return ResponseEntity.ok(/* body = */ orderResponse)
    }
}