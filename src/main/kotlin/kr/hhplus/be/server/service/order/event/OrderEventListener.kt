package kr.hhplus.be.server.service.order.event

import kotlinx.coroutines.runBlocking
import kr.hhplus.be.server.service.order.port.DataPlatformPort
import kr.hhplus.be.server.service.product.port.PopularProductPort
import kr.hhplus.be.server.service.transaction.CompensationScope
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class OrderEventListener(
    private val popularProductPort: PopularProductPort,
    private val dataPlatformPort: DataPlatformPort,
    private val timeProvider: KoreanTimeProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 데이터 플랫폼 전송: 주문 생성 정보를 외부 데이터 플랫폼에 전달
     */
    @Async
    @EventListener
    fun sendOrderToDataPlatformOnOrderCreated(event: OrderCreated) = runBlocking {
        runCatching {
            CompensationScope.runTransaction {
                execute {
                    dataPlatformPort.sendOrderData(event.order)
                }.compensate { 
                    dataPlatformPort.revertOrderData(event.order)
                }
            }
        }.onFailure { ex ->
            log.warn("데이터 플랫폼 전송 실패", ex)
        }
    }
}


