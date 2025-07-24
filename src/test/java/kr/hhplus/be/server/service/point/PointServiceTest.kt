package kr.hhplus.be.server.service.point

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kr.hhplus.be.server.service.ServiceTestBase
import kr.hhplus.be.server.service.point.entity.PointChange
import kr.hhplus.be.server.service.point.entity.PointChangeType
import kr.hhplus.be.server.service.point.port.PointPort
import kr.hhplus.be.server.service.point.service.PointService
import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PointService 단위테스트")
class PointServiceTest : ServiceTestBase() {

    @MockK
    private lateinit var userService: UserService
    
    @MockK
    private lateinit var pointPort: PointPort
    
    private lateinit var pointService: PointService

    @BeforeEach
    fun setupPointService() {
        super.setUp()
        pointService = PointService(userService, timeProvider, pointPort)
    }

    @Nested
    @DisplayName("chargePoint 메서드는")
    inner class ChargePointTest {

        @Test
        @DisplayName("포인트를 성공적으로 충전한다")
        fun chargesPointSuccessfully() {
            // given
            val userId = 1L
            val chargeAmount = 10000L
            val user = User(userId, "김철수", 5000L)
            val expectedPointChange = PointChange(
                1L, userId, chargeAmount, PointChangeType.Charge, fixedTime
            )
            
            every { userService.readSingleUser(userId) } returns user
            every { pointPort.chargePoint(userId, chargeAmount, fixedTime) } returns expectedPointChange

            // when
            val result = pointService.chargePoint(userId, chargeAmount)

            // then
            result shouldBe expectedPointChange
            verify { userService.readSingleUser(userId) }
            verify { pointPort.chargePoint(userId, chargeAmount, fixedTime) }
        }

        @Test
        @DisplayName("사용자 조회 후 포인트 충전을 처리한다")
        fun verifiesUserExistenceBeforeCharging() {
            // given
            val userId = 1L
            val chargeAmount = 5000L
            val user = User(userId, "김철수", 10000L)
            val expectedPointChange = PointChange(
                2L, userId, chargeAmount, PointChangeType.Charge, fixedTime
            )
            
            every { userService.readSingleUser(userId) } returns user
            every { pointPort.chargePoint(user.id, chargeAmount, fixedTime) } returns expectedPointChange

            // when
            val result = pointService.chargePoint(userId, chargeAmount)

            // then
            result shouldBe expectedPointChange
            verify { userService.readSingleUser(userId) }
            verify { pointPort.chargePoint(user.id, chargeAmount, fixedTime) }
        }
    }

    @Nested
    @DisplayName("usePoint 메서드는")
    inner class UsePointTest {

        @Test
        @DisplayName("포인트를 성공적으로 사용한다")
        fun usesPointSuccessfully() {
            // given
            val userId = 1L
            val useAmount = 3000L
            val expectedPointChange = PointChange(
                3L, userId, useAmount, PointChangeType.Use, fixedTime
            )
            
            every { userService.requireUserExists(userId) } returns Unit
            every { pointPort.usePoint(userId, useAmount, fixedTime) } returns expectedPointChange

            // when
            val result = pointService.usePoint(userId, useAmount)

            // then
            result shouldBe expectedPointChange
            verify { userService.requireUserExists(userId) }
            verify { pointPort.usePoint(userId, useAmount, fixedTime) }
        }

        @Test
        @DisplayName("사용자 존재 확인 후 포인트 사용을 처리한다")
        fun verifiesUserExistenceBeforeUsing() {
            // given
            val userId = 1L
            val useAmount = 7000L
            val expectedPointChange = PointChange(
                4L, userId, useAmount, PointChangeType.Use, fixedTime
            )
            
            every { userService.requireUserExists(userId) } returns Unit
            every { pointPort.usePoint(userId, useAmount, fixedTime) } returns expectedPointChange

            // when
            val result = pointService.usePoint(userId, useAmount)

            // then
            result shouldBe expectedPointChange
            verify { userService.requireUserExists(userId) }
            verify { pointPort.usePoint(userId, useAmount, fixedTime) }
        }
    }

    @Nested
    @DisplayName("PointChange Entity 검증")
    inner class PointChangeEntityTest {

        @Test
        @DisplayName("충전 타입 PointChange가 올바르게 생성된다")
        fun createChargePointChange() {
            // given & when
            val pointChange = PointChange(
                1L, 1L, 10000L, PointChangeType.Charge, fixedTime
            )

            // then
            pointChange.id shouldBe 1L
            pointChange.userId shouldBe 1L
            pointChange.pointChange shouldBe 10000L
            pointChange.type shouldBe PointChangeType.Charge
            pointChange.happenedAt shouldBe fixedTime
        }

        @Test
        @DisplayName("사용 타입 PointChange가 올바르게 생성된다")
        fun createUsePointChange() {
            // given & when
            val pointChange = PointChange(
                2L, 1L, 5000L, PointChangeType.Use, fixedTime
            )

            // then
            pointChange.id shouldBe 2L
            pointChange.userId shouldBe 1L
            pointChange.pointChange shouldBe 5000L
            pointChange.type shouldBe PointChangeType.Use
            pointChange.happenedAt shouldBe fixedTime
        }

        @Test
        @DisplayName("PointChangeType이 올바른 값들을 가진다")
        fun verifyPointChangeTypeValues() {
            // given & when & then
            PointChangeType.Charge shouldBe PointChangeType.Charge
            PointChangeType.Use shouldBe PointChangeType.Use
        }
    }
} 