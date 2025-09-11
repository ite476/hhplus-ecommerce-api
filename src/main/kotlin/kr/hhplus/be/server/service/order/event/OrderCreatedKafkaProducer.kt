package kr.hhplus.be.server.service.order.event

import org.springframework.context.event.EventListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * OrderCreated 도메인 이벤트를 Kafka 토픽으로 포워딩하는 최소 구현.
 * - 토픽: order.events
 * - 키: orderId
 * - 값: OrderCreated (도메인 이벤트 그대로)
 */
@Component
class OrderCreatedKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, OrderCreated>
) {
    private val topicName: String = "order.events"

    @EventListener
    fun onOrderCreated(event: OrderCreated) {
        val orderId: Long = checkNotNull(event.order.id) { "order.id must not be null when publishing." }
        //
        kafkaTemplate.send(topicName, orderId.toString(), event)
    }
}