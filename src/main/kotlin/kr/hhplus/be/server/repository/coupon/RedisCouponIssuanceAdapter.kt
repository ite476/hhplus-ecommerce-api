package kr.hhplus.be.server.repository.coupon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.hhplus.be.server.service.coupon.port.CouponIssuancePort
import kr.hhplus.be.server.service.coupon.port.CouponIssuanceResult
import kr.hhplus.be.server.service.lock.LockUtils.lockKeyWithReady
import org.redisson.api.RScript
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Redis 기반 쿠폰 발급 어댑터
 * - ready 플래그 워밍
 * - Lua 스크립트로 중복/재고 원자 검증 및 차감
 */
@Component
class RedisCouponIssuanceAdapter(
    private val redisson: RedissonClient
) : CouponIssuancePort {

    private fun stockKey(couponId: Long) = "coupon:$couponId:stock"
    private fun readyKey(couponId: Long) = lockKeyWithReady(prefix = "coupon:$couponId")
    private fun issuedKey(couponId: Long, userId: Long) = "coupon:$couponId:issued:$userId"
    private fun warmLockKey(couponId: Long) = "lock:coupon:$couponId:warm"

    override suspend fun ensureReady(couponId: Long) = withContext(Dispatchers.IO) {
        val ready = redisson.getBucket<String>(readyKey(couponId))
        if (ready.isExists) return@withContext

        val lock = redisson.getLock(warmLockKey(couponId))
        val acquired = lock.tryLock(/* waitTime = */ 1000, /* leaseTime = */ 5000, TimeUnit.MILLISECONDS)
        if (!acquired) return@withContext
        try {
            // 더블 체크
            if (ready.isExists) return@withContext
            // 간단화를 위해 초기화만 수행. 실제 수량 로딩은 DB 연동 필요(향후 확장)
            val stock = redisson.getBucket<Long>(stockKey(couponId))
            if (!stock.isExists) {
                // 기본 0으로 초기화 (운영에서는 DB remaining 반영 필요)
                stock.set(0L)
            }
            // ready 플래그 설정 (예: 만료까지 유지 필요 시 TTL 설정)
            ready.set("1")
        } finally {
            if (lock.isHeldByCurrentThread) lock.unlock()
        }
    }

    override suspend fun tryIssue(userId: Long, couponId: Long): CouponIssuanceResult = withContext(Dispatchers.IO) {
        ensureReady(couponId)

        val script = """
            local issuedKey = KEYS[1]
            local stockKey = KEYS[2]
            local readyKey = KEYS[3]

            -- ready 확인
            if redis.call('EXISTS', readyKey) == 0 then
                return 3 -- NOT_READY
            end

            -- 이미 발급 여부 확인
            if redis.call('EXISTS', issuedKey) == 1 then
                return 1 -- ALREADY_ISSUED
            end

            local stock = tonumber(redis.call('GET', stockKey) or '0')
            if stock <= 0 then
                return 2 -- OUT_OF_STOCK
            end

            -- 재고 차감 및 발급 마킹
            redis.call('DECR', stockKey)
            redis.call('SET', issuedKey, '1')

            return 0 -- OK
        """.trimIndent()

        val keys = listOf(
            issuedKey(couponId, userId),
            stockKey(couponId),
            readyKey(couponId)
        )

        val rScript: RScript = redisson.getScript((StringCodec.INSTANCE))

        val evalResult = rScript.eval(
            /* mode = */ RScript.Mode.READ_WRITE,
            /* luaScript = */ script,
            /* returnType = */ RScript.ReturnType.INTEGER,
            /* keys = */ keys
        ) as Number?

        when (evalResult?.toInt()) {
            0 -> CouponIssuanceResult.OK
            1 -> CouponIssuanceResult.ALREADY_ISSUED
            2 -> CouponIssuanceResult.OUT_OF_STOCK
            3 -> CouponIssuanceResult.NOT_READY
            else -> CouponIssuanceResult.NOT_READY
        }
    }
}


