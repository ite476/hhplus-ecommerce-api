package kr.hhplus.be.server.service.config.kafka

import kr.hhplus.be.server.service.order.event.OrderCreated
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@Configuration
class KafkaConsumerConfig(
    private val kafkaProperties: KafkaProperties
) {
    @Bean
    fun orderCreatedConsumerFactory(): ConsumerFactory<String, OrderCreated> {
        val props = HashMap<String, Any>(kafkaProperties.buildConsumerProperties())
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        // Rely on application.yml for JsonDeserializer configuration to avoid double configuration
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun orderCreatedKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, OrderCreated> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, OrderCreated>()
        factory.consumerFactory = orderCreatedConsumerFactory()
        factory.setAutoStartup(true)
        return factory
    }
}


