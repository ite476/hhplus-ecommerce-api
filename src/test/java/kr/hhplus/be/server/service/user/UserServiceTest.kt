package kr.hhplus.be.server.service.user

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kr.hhplus.be.server.service.ServiceTestBase
import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.exception.UserNotFoundException
import kr.hhplus.be.server.service.user.port.UserPort
import kr.hhplus.be.server.service.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UserService 단위테스트")
class UserServiceTest : ServiceTestBase() {

    @MockK
    private lateinit var userPort: UserPort
    
    private lateinit var userService: UserService

    @BeforeEach
    fun setupUserService() {
        super.setUp()
        userService = UserService(userPort)
    }

    @Nested
    @DisplayName("findUserById 메서드는")
    inner class ReadSingleUserTest {

        @Test
        @DisplayName("회원이 존재할 때 회원 정보를 반환한다")
        fun returnsUserWhenUserExists() {
            // given
            val userId = 1L
            val expectedUser = User(userId, "김철수", 10000L)
            
            every { userPort.findUserById(userId) } returns expectedUser

            // when
            val result = userService.findUserById(userId)

            // then
            result shouldBe expectedUser
            verify { userPort.findUserById(userId) }
        }

        @Test
        @DisplayName("회원이 존재하지 않을 때 UserNotFoundException을 던진다")
        fun throwsUserNotFoundExceptionWhenUserDoesNotExist() {
            // given
            val userId = 999L
            every { userPort.findUserById(userId) } throws UserNotFoundException()

            // when & then
            shouldThrow<UserNotFoundException> {
                userService.findUserById(userId)
            }
            
            verify { userPort.findUserById(any()) }
        }
    }

    @Nested
    @DisplayName("requireUserIdExists 메서드는")
    inner class RequireUserExistsTest {

        @Test
        @DisplayName("회원이 존재할 때 예외를 던지지 않는다")
        fun doesNotThrowWhenUserExists() {
            // given
            val userId = 1L
            every { userPort.existsUser(userId) } returns true

            // when & then
            userService.requireUserIdExists(userId)
            
            verify { userPort.existsUser(userId) }
        }

        @Test
        @DisplayName("회원이 존재하지 않을 때 UserNotFoundException을 던진다")
        fun throwsUserNotFoundExceptionWhenUserDoesNotExist() {
            // given
            val userId = 999L
            every { userPort.existsUser(userId) } returns false

            // when & then
            shouldThrow<UserNotFoundException> {
                userService.requireUserIdExists(userId)
            }
            
            verify { userPort.existsUser(userId) }
        }
    }
} 