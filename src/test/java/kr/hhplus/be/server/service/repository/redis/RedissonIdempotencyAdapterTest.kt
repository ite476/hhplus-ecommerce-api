package kr.hhplus.be.server.service.repository.redis

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kr.hhplus.be.server.repository.redis.RedissonIdempotencyAdapter
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.api.RBucket
import org.redisson.api.RedissonClient
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@DisplayName("RedissonIdempotencyAdapter - 멱등 키 선점")
class RedissonIdempotencyAdapterTest {

    @Test
    @DisplayName("동일 키를 두 번 선점하면, 첫 번째는 성공이고 두 번째는 실패이다")
    fun 동일_키_두_번_선점_첫번째_성공_두번째_실패() = runBlocking {
        // Given
        val client = mockk<RedissonClient>()
        val bucket = mockk<RBucket<String>>()
        every { client.getBucket<String>(any<String>()) } returns bucket
        every { bucket.trySet(any(), any<Long>(), any<TimeUnit>()) } returnsMany listOf(true, false)

        val adapter = RedissonIdempotencyAdapter(client)

        // When
        val first = adapter.tryAcquire("k1", 1.seconds)
        val second = adapter.tryAcquire("k1", 1.seconds)

        // Then
        assertTrue(first)
        assertFalse(second)
    }

    @Test
    @DisplayName("서로 다른 키를 각각 선점하면, 두 호출 모두 성공이다")
    fun 서로_다른_키_각각_선점_모두_성공() = runBlocking {
        // Given
        val client = mockk<RedissonClient>()
        val bucket = mockk<RBucket<String>>()
        every { client.getBucket<String>(any<String>()) } returns bucket
        every { bucket.trySet(any(), any<Long>(), any<TimeUnit>()) } returns true

        val adapter = RedissonIdempotencyAdapter(client)

        // When
        val k1 = adapter.tryAcquire("k1", 1.seconds)
        val k2 = adapter.tryAcquire("k2", 1.seconds)

        // Then
        assertTrue(k1)
        assertTrue(k2)
    }
}


