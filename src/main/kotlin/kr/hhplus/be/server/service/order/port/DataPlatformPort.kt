package kr.hhplus.be.server.service.order.port

import kr.hhplus.be.server.service.order.entity.Order

interface DataPlatformPort {
    suspend fun sendOrderData(order: Order)
}
