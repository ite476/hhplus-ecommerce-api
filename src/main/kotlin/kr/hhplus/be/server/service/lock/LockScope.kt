package kr.hhplus.be.server.service.lock

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * 교착 회피: keys를 정렬해서 순차 획득
 * 공정성(fairness) 보장은 아님. FIFO 공정이 필요하면 큐 기반 설계가 따로 필요함.
 * 
 * 특징:
 * - 순차적 락 획득으로 교착 상태 방지
 * - 자동 락 해제 보장 (finally 블록)
 * - NonCancellable 컨텍스트로 강제 해제
 */
class LockScope private constructor(
    private val client: AsyncLockClient
) {
    private val acquiredLocks = ArrayDeque<LockHandle>()

    companion object {
        /**
         * 단일 락으로 블록 실행
         */
        suspend fun <T> withLock(
            client: AsyncLockClient,
            key: String,
            options: LockOptions = LockOptions.DEFAULT,
            block: suspend LockScope.() -> T
        ): T = withLocks(client = client, keys = listOf(key), options = options, block = block)

        /**
         * 다중 락으로 블록 실행
         * 
         * @param client 락 클라이언트
         * @param keys 락을 획득할 키 목록
         * @param options 락 옵션
         * @param block 락 획득 후 실행할 블록
         * @return 블록 실행 결과
         * @throws LockAcquisitionTimeoutException 락 획득 실패 시
         */
        suspend fun <T> withLocks(
            client: AsyncLockClient,
            keys: List<String>,
            options: LockOptions = LockOptions.DEFAULT,
            block: suspend LockScope.() -> T
        ): T {
            require(keys.isNotEmpty()) { "keys must not be empty" }

            // 중복 제거 + 정렬(교착 회피 글로벌 오더)
            val distinctSorted = keys.distinct().sorted()

            val scope = LockScope(client)

            try {
                // 순차 획득 (타임아웃/취소 대응은 AsyncLockClient 구현에 위임)
                for (key in distinctSorted) {
                    val handle = client.tryAcquire(key = key, lockOptions = options)
                        ?: throw LockAcquisitionTimeoutException(failedKey = key)
                    scope.acquiredLocks.addLast(element = handle)
                }

                return scope.block()
            } catch (t: Throwable) {
                // 예외 발생 시 전파
                throw t
            } finally {
                // 어떤 상황에서도 해제되도록 NonCancellable
                withContext(context = NonCancellable) {
                    scope.releaseAll()
                }
            }
        }
    }

    /**
     * 획득한 모든 락을 안전하게 해제
     * 개별 락 해제 실패는 로깅만 하고 계속 진행
     */
    private suspend fun releaseAll() {
        while (acquiredLocks.isNotEmpty()) {
            val handle = acquiredLocks.removeLast()
            runCatching { 
                client.release(handle) 
            }.onFailure { exception ->
                // 락 해제 실패 시 로깅 (실제 운영에서는 로거 사용)
                // logger.warn("Failed to release lock: ${handle.key}", exception)
            }
        }
    }
}

