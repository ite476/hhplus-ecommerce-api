package kr.hhplus.be.server.repository.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.redisson.Redisson
import org.redisson.config.Config
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@Testcontainers
class RedisAdapterTest {

    companion object {
        private const val REDIS_IMAGE = "redis:7-alpine"

        @Container
        @JvmStatic
        val redis: GenericContainer<*> = GenericContainer(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(6379)
    }

    @Test
    fun `set and get value`() {
        val address = "redis://" + redis.host + ":" + redis.getMappedPort(6379)
        val cfg = Config()
        cfg.useSingleServer().address = address
        val client = Redisson.create(cfg)

        val adapter = RedisAdapter(client)

        val key = "adapter:test:key"
        adapter.delete(key)
        adapter.setValue(key, "hello", Duration.ofSeconds(5))
        val value = adapter.getValue(key)
        assertThat(value).isEqualTo("hello")
    }
}

