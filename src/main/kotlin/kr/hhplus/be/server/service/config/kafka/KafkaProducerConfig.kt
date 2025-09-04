package kr.hhplus.be.server.service.config.kafka

import kr.hhplus.be.server.service.order.event.OrderCreated
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaProducerConfig(
    private val kafkaProperties: KafkaProperties
) {
    @Bean
    fun orderCreatedProducerFactory(): ProducerFactory<String, OrderCreated> {
        val props = HashMap<String, Any>(kafkaProperties.buildProducerProperties())
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        props[JsonSerializer.ADD_TYPE_INFO_HEADERS] = false
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun orderCreatedKafkaTemplate(): KafkaTemplate<String, OrderCreated> =
        KafkaTemplate(orderCreatedProducerFactory())

    @Bean
    fun genericProducerFactory(): ProducerFactory<String, Any> {
        val props = HashMap<String, Any>(kafkaProperties.buildProducerProperties())
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        props[JsonSerializer.ADD_TYPE_INFO_HEADERS] = false
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun genericKafkaTemplate(): KafkaTemplate<String, Any> = KafkaTemplate(genericProducerFactory())
}


