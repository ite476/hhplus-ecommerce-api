package kr.hhplus.be.server.service.transaction

import java.util.*

class CompensationScope {
    companion object {
        suspend fun <T> runTransaction(block: suspend CompensationScope.() -> T): T {
            val scope = CompensationScope()
            return scope.executeWithCompensation(block)
        }
    }


    private val stack = ArrayDeque<suspend () -> Unit>() // Stack 대신 비동기 안전한 deque

    suspend fun <T> execute(block: suspend () -> T): CompensatedResult<T> {
        val result = block()
        return CompensatedResult(result, this::registerRollback)
    }

    fun registerRollback(block: suspend () -> Unit) {
        stack.push(block)
    }


    suspend fun rollbackAll() {
        while (stack.isNotEmpty()) {
            val rollback = stack.removeLast()
            try {
                rollback() // 확실히 실행
            } catch (ex: Exception) {
                // TODO: 로그 남기고 무시
            }
        }
    }

    suspend fun <T> executeWithCompensation(block: suspend CompensationScope.() -> T): T {
        try{
            val result = block()
            return result
        }
        catch(ex: Exception){
            this.rollbackAll()
            throw ex
        }
    }


    inner class CompensatedResult<T>(
        val result: T,
        private val registerRollbackBlock: (suspend () -> Unit) -> Unit
    ) {
        suspend fun compensateBy(rollback: suspend (T) -> Unit): T {
            registerRollbackBlock { rollback(result) }
            return result
        }

        suspend fun compensate(rollback: suspend () -> Unit): T {
            registerRollbackBlock { rollback() }
            return result
        }
    }
}

