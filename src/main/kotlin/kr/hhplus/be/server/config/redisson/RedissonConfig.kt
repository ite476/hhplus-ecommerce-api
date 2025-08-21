package kr.hhplus.be.server.config.redisson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/*
 * RedissonClient Configuration
 */
@Configuration
class RedissonConfig {
    @Value("\${spring.redis.host}")
    private lateinit var redisHost: String

    @Value("\${spring.redis.port}")
    private var redisPort: Int = 6379

    @Value("\${spring.redis.password}")
    private var redisPassword: String? = null

    @Bean
    fun redissonClient(): RedissonClient {
        val config: Config = Config()
        config.useSingleServer()
            .setAddress("$REDISSON_HOST_PREFIX$redisHost:$redisPort")
            .apply {
                redisPassword?.let { setPassword(it) }
            }
        
        return Redisson.create(config)
    }

    /**
     * ZonedDateTime 지원을 강화한 전역 ObjectMapper
     * Spring Boot의 기본 ObjectMapper를 오버라이드
     */
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            // JSR310 모듈 등록 (ZonedDateTime 등 지원)
            registerModule(JavaTimeModule())
            
            // ZonedDateTime을 ISO-8601 문자열로 직렬화
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            
            // Kotlin 모듈 등록
            registerModule(KotlinModule.Builder().build())
            
            // 추가 모듈 자동 등록
            findAndRegisterModules()
        }
    }

    companion object {
        private const val REDISSON_HOST_PREFIX = "redis://"
    }
}