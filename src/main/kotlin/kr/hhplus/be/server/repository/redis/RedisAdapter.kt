package kr.hhplus.be.server.repository.redis

import kr.hhplus.be.server.service.redis.port.RedisPort
import org.redisson.api.RedissonClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConditionalOnBean(RedissonClient::class)
class RedisAdapter(
    private val redissonClient: RedissonClient
) : RedisPort {

    override fun setValue(key: String, value: String, ttl: Duration?) {
        val bucket = redissonClient.getBucket<String>(key)
        if (ttl == null) {
            bucket.set(value)
        } else {
            bucket.set(value, ttl)
        }
    }

    override fun getValue(key: String): String? {
        val bucket = redissonClient.getBucket<String>(key)
        return bucket.get()
    }

    override fun delete(key: String) {
        val bucket = redissonClient.getBucket<String>(key)
        bucket.delete()
    }
}