package kr.hhplus.be.server.service.transaction

import java.util.*

class CompensationScope {
    private val stack = Stack<suspend () -> Unit>()

    suspend fun <T> execute(block: suspend () -> T): CompensatedResult<T> {
        val result = block()
        return CompensatedResult(result, this::registerRollback)
    }

    fun registerRollback(block: suspend () -> Unit) {
        stack.push(block)
    }

    suspend fun rollbackAll() {
        while (stack.isNotEmpty()) {
            try {
                stack.pop().invoke()
            } catch (_: Exception) {
                // 필요 시 로그 처리 등 후처리를 위한 공간
                // 예외가 터져도 롤백은 해야한다
                // 살려야한다..!!! 마인드
            }
        }
    }

    class CompensatedResult<T>(
        val result: T,
        private val registerRollback: suspend (suspend () -> Unit) -> Unit
    ) {
        suspend fun compensate(rollback: suspend () -> Unit): T {
            registerRollback(rollback)
            return result
        }
    }
}

