package kr.hhplus.be.server.service.transaction

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CompensationScope 단위테스트")
class CompensationScopeTest {

    @Test
    @DisplayName("정상 실행 시 롤백이 수행되지 않는다")
    fun shouldNotRollbackOnSuccess() = runTest {
        // given
        var mainActionExecuted = false
        var rollbackExecuted = false

        // when
        val result = CompensationScope.runTransaction {
            execute {
                mainActionExecuted = true
                "success"
            }.compensate {
                rollbackExecuted = true
            }
        }

        // then
        result shouldBe "success"
        mainActionExecuted shouldBe true
        rollbackExecuted shouldBe false
    }

    @Test
    @DisplayName("예외 발생 시 롤백이 수행된다")
    fun shouldRollbackOnException() = runTest {
        // given
        var mainActionExecuted = false
        var rollbackExecuted = false

        // when & then
        shouldThrow<RuntimeException> {
            CompensationScope.runTransaction {
                execute {
                    mainActionExecuted = true
                    "success"
                }.compensate {
                    rollbackExecuted = true
                }
                
                // 예외 발생
                throw RuntimeException("Test exception")
            }
        }

        // then
        mainActionExecuted shouldBe true
        rollbackExecuted shouldBe true
    }

    @Test
    @DisplayName("여러 작업이 역순으로 롤백된다")
    fun shouldRollbackInReverseOrder() = runTest {
        // given
        val executionOrder = mutableListOf<String>()
        val rollbackOrder = mutableListOf<String>()

        // when & then
        shouldThrow<RuntimeException> {
            CompensationScope.runTransaction {
                execute {
                    executionOrder.add("action1")
                    "result1"
                }.compensate {
                    rollbackOrder.add("rollback1")
                }

                execute {
                    executionOrder.add("action2")
                    "result2"
                }.compensate {
                    rollbackOrder.add("rollback2")
                }

                execute {
                    executionOrder.add("action3")
                    "result3"
                }.compensate {
                    rollbackOrder.add("rollback3")
                }

                // 예외 발생
                throw RuntimeException("Test exception")
            }
        }

        // then
        executionOrder shouldBe listOf("action1", "action2", "action3")
        rollbackOrder shouldBe listOf("rollback3", "rollback2", "rollback1") // 역순
    }
}