package kr.hhplus.be.server.service.order.event

import kr.hhplus.be.server.repository.product.RedisPopularProductSummaryCacheDto
import kr.hhplus.be.server.service.order.port.DataPlatformPort
import kr.hhplus.be.server.service.product.port.PopularProductPort
import kr.hhplus.be.server.service.transaction.CompensationScope
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OrderEventListener(
    private val popularProductPort: PopularProductPort,
    private val dataPlatformPort: DataPlatformPort,
    private val timeProvider: KoreanTimeProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 인기 상품 집계: 주문 생성 정보를 Redis 집계 시스템에 전달
     */
    @Async
    @EventListener
    suspend fun aggregatePopularProductSalesOnOrderCreated(event: OrderCreated) {
        runCatching {
            CompensationScope.runTransaction {
                val now: ZonedDateTime = timeProvider.now()
                val maps: List<RedisPopularProductSummaryCacheDto> = event.order.orderItems.map {
                    RedisPopularProductSummaryCacheDto(
                        productId = it.productId.toInt(),
                        soldCount = it.quantity.toInt()
                    )
                }

                execute {
                    popularProductPort.increaseProductSoldCount(maps, now)
                }.compensate { 
                    popularProductPort.decreaseProductSoldCount(maps, now)
                }
            }
        }.onFailure { ex ->
            log.warn("인기상품 집계 처리 실패", ex)
        }
    }

    /**
     * 데이터 플랫폼 전송: 주문 생성 정보를 외부 데이터 플랫폼에 전달
     */
    @Async
    @EventListener
    suspend fun sendOrderToDataPlatformOnOrderCreated(event: OrderCreated) {
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


