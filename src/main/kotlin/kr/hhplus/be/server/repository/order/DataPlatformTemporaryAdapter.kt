package kr.hhplus.be.server.repository.order

import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.port.DataPlatformPort
import org.springframework.stereotype.Component

@Component
class DataPlatformTemporaryAdapter : DataPlatformPort {
    override suspend fun sendOrderData(order: Order) {
        TODO("Not yet implemented")
    }
}