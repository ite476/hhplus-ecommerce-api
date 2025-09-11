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
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
class KafkaConsumerConfig(
    private val kafkaProperties: KafkaProperties
) {
    @Bean
    fun orderCreatedConsumerFactory(): ConsumerFactory<String, OrderCreated> {
        val props = HashMap<String, Any>(kafkaProperties.buildConsumerProperties())
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        val valueDeserializer = JsonDeserializer(OrderCreated::class.java).apply {
            addTrustedPackages("kr.hhplus.be.server.*")
            setUseTypeHeaders(false)
        }
        return DefaultKafkaConsumerFactory(props, StringDeserializer(), valueDeserializer)
    }

    @Bean
    fun orderCreatedKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, OrderCreated> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, OrderCreated>()
        factory.consumerFactory = orderCreatedConsumerFactory()
        factory.setAutoStartup(true)
        return factory
    }
}


