package kr.hhplus.be.server.repository.order

import kr.hhplus.be.server.service.order.entity.Order
import kr.hhplus.be.server.service.order.port.DataPlatformPort
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 데이터 플랫폼 더미 구현체.
 * - 외부 전송 없이 로그만 남김
 */
@Component
class DummyDataPlatformAdapter : DataPlatformPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun sendOrderData(order: Order) {
        delay(100)
        log.info("[DummyDataPlatform] 주문 데이터 전송 요청 수신: orderId={}", order.id)
    }

    override suspend fun revertOrderData(order: Order) {
        delay(100)
        log.info("[DummyDataPlatform] 주문 데이터 전송 보상 요청 수신: orderId={}", order.id)
    }
}


