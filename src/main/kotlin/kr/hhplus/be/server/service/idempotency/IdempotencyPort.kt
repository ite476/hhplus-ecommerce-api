package kr.hhplus.be.server.service.idempotency

import kotlin.time.Duration

interface IdempotencyPort {
    /**
     * 멱등성 키를 TTL과 함께 선점합니다. 이미 존재하면 false를 반환합니다.
     */
    suspend fun tryAcquire(key: String, ttl: Duration): Boolean
}