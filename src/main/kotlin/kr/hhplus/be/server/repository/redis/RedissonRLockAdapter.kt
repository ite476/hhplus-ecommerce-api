package kr.hhplus.be.server.repository.redis

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kr.hhplus.be.server.service.lock.AsyncLockClient
import kr.hhplus.be.server.service.lock.LockHandle
import kr.hhplus.be.server.service.lock.LockOptions
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Redisson 락 어댑터
 * 
 * 특징:
 * - 단일 스레드 디스패처로 락 작업 직렬화
 * - UUID 기반 고유 토큰으로 소유자 검증
 * - 메모리 기반 토큰 저장소로 빠른 검증
 */
@Component
class RedissonRLockAdapter(
    private val redisson: RedissonClient,
    private val keyPrefix: String = "lock:"
) : AsyncLockClient, AutoCloseable {

    // 토큰 저장소: key -> (lock, token) 매핑
    private val tokenStore = ConcurrentHashMap<String, Pair<org.redisson.api.RLock, String>>()
    
    // 모든 lock/unlock을 이 단일 스레드에서 수행 (가장 단순하고 안전)
    private val dispatcher: ExecutorCoroutineDispatcher =
        Executors.newSingleThreadExecutor { r -> 
            Thread(r, "rlock-single").apply { isDaemon = true } 
        }.asCoroutineDispatcher()

    private fun name(key: String) = "$keyPrefix$key"

    override suspend fun tryAcquire(key: String, lockOptions: LockOptions): LockHandle? =
        withContext(dispatcher) {
            val lock = redisson.getLock(name(key))
            val ok = lock.tryLock(
                lockOptions.wait.inWholeMilliseconds,
                lockOptions.lease.inWholeMilliseconds,
                TimeUnit.MILLISECONDS
            )
            
            if (ok) {
                // 고유 토큰 생성 및 저장
                val token = UUID.randomUUID().toString()
                tokenStore[key] = lock to token
                LockHandle(key, token)
            } else {
                null
            }
        }

    override suspend fun release(handle: LockHandle) {
        withContext(dispatcher) {
            val stored = tokenStore[handle.key]
            if (stored != null) {
                val (lock, storedToken) = stored
                
                // 토큰 검증으로 소유자 확인
                if (handle.token == storedToken && lock.isHeldByCurrentThread) {
                    lock.unlock()
                    tokenStore.remove(handle.key)
                } else {
                    // 토큰 불일치 또는 소유자가 아닌 경우
                    // 로깅만 하고 해제하지 않음
                }
            }
        }
    }

    override fun close() {
        dispatcher.close()
        // 보유 중인 모든 락 정리
        tokenStore.values.forEach { (lock, _) ->
            if (lock.isHeldByCurrentThread) {
                lock.forceUnlock()
            }
        }
        tokenStore.clear()
    }

    @PreDestroy
    fun shutdown() = close()
}
