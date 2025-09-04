package kr.hhplus.be.server.service.order.event

import kr.hhplus.be.server.repository.product.RedisPopularProductSummaryCacheDto
import kr.hhplus.be.server.service.idempotency.IdempotencyPort
import kr.hhplus.be.server.service.product.port.PopularProductPort
import kr.hhplus.be.server.service.transaction.CompensationScope
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 주문 생성 이벤트 소비자
 */
@Component
class OrderCreatedKafkaConsumer(
    private val popularProductPort: PopularProductPort,
    private val timeProvider: KoreanTimeProvider,
    private val idempotencyPort: IdempotencyPort,
    @Value("\${app.idempotency.event-ttl-seconds:86400}") private val idempotencyTtlSeconds: Long
) {
    private val processedKeyPrefix = "evt:processed:"

    /**
     * 인기 상품 카운트 업데이트 (멱등 가드 적용)
     */
    @KafkaListener(
        topics = ["order.events"],
        containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    suspend fun onMessage(event: OrderCreated) {
        val key = processedKeyPrefix + event.eventId.toString()
        val ttl: Duration = idempotencyTtlSeconds.seconds
        if (!idempotencyPort.tryAcquire(key, ttl)) return

        val now = timeProvider.now()
        val maps: List<RedisPopularProductSummaryCacheDto> = event.order.orderItems.map { item ->
            RedisPopularProductSummaryCacheDto(
                productId = item.productId.toInt(),
                soldCount = item.quantity.toInt()
            )
        }

        CompensationScope.runTransaction {
            execute {
                popularProductPort.increaseProductSoldCount(maps, now)
            }.compensate {
                popularProductPort.decreaseProductSoldCount(maps, now)
            }
        }
    }
}


