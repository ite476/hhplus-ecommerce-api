package kr.hhplus.be.server.repository.redis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.hhplus.be.server.service.idempotency.IdempotencyPort
import org.redisson.api.RedissonClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

@Component
@ConditionalOnBean(RedissonClient::class)
class RedissonIdempotencyAdapter(
    private val redisson: RedissonClient
) : IdempotencyPort {
    override suspend fun tryAcquire(key: String, ttl: Duration): Boolean = withContext(Dispatchers.IO) {
        val bucket = redisson.getBucket<String>(key)
        bucket.trySet("1", ttl.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }
}


