package kr.hhplus.be.server.controller.v1.order

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import kr.hhplus.be.server.controller.v1.order.request.PostOrderRequestBody
import kr.hhplus.be.server.controller.v1.order.response.PostOrderResponse
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.service.OrderService
import kr.hhplus.be.server.service.order.usecase.CreateOrderUsecase
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order API", description = "주문 API")
@Validated
class OrderController(
    val createOrderUsecase: CreateOrderUsecase
    ) : OrderApiSpec {

    @PostMapping("")
    override fun createOrder(
        @RequestHeader userId: Long,
        @RequestBody @Valid body: PostOrderRequestBody
    ): ResponseEntity<PostOrderResponse> {
        val order: Order = runBlocking {
            createOrderUsecase.createOrder(OrderService.CreateOrderInput(
                userId = userId,
                products = body.orderItems.map {
                    OrderService.CreateOrderInput.ProductWithQuantity(
                        productId = it.productId,
                        quantity = it.quantity
                    )
                },
                userCouponId = body.userCouponId
            ))
        }

        val orderResponse: PostOrderResponse = PostOrderResponse.fromEntity(order)

        return ResponseEntity
            .created(URI.create("/orders"))
            .body(orderResponse)
    }
}