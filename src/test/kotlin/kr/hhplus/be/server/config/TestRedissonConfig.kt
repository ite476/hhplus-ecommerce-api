package kr.hhplus.be.server.config

import org.redisson.api.RedissonClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
@Profile("test")
class TestRedissonConfig {
    
    companion object {
        val redisContainer: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--appendonly", "yes", "--requirepass", "redis_pwd")
            .apply {
                start()
            }
    }

    @Bean
    @Primary
    fun redissonClient(): RedissonClient {
        val config = org.redisson.config.Config()
        config.useSingleServer()
            .setAddress("redis://${redisContainer.host}:${redisContainer.getMappedPort(6379)}")
            .setPassword("redis_pwd")
            .setConnectionMinimumIdleSize(1)
            .setConnectionPoolSize(2)
        
        return org.redisson.Redisson.create(config)
    }
}
