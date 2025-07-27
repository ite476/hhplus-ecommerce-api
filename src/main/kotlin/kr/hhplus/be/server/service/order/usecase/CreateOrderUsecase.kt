package kr.hhplus.be.server.service.order.usecase

import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.service.OrderService.CreateOrderInput

interface CreateOrderUsecase {
    /**
     * 주문을 신규 생성합니다.
     */
    suspend fun createOrder(input: CreateOrderInput) : Order
}
