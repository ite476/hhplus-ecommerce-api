package kr.hhplus.be.server.repository.order

import kotlinx.coroutines.delay
import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.port.DataPlatformPort
import org.springframework.stereotype.Component

@Component
class DummyDataPlatformAdapter : DataPlatformPort {
    override suspend fun sendOrderData(order: Order) {
        // 외부 데이터 플랫폼 전송 시뮬레이션
        delay(100)  // 0.1초 지연
    }
}