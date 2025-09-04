package kr.hhplus.be.server.service.coupon.event

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class CouponEventKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val topicName: String = "coupon.events"

    fun publish(eventKey: String, event: Any) {
        kafkaTemplate.send(topicName, eventKey, event)
    }
}


