package kr.hhplus.be.server.service.point

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import kr.hhplus.be.server.service.ServiceTestBase
import kr.hhplus.be.server.service.point.entity.PointChange
import kr.hhplus.be.server.service.point.entity.PointChangeType
import kr.hhplus.be.server.service.point.port.PointPort
import kr.hhplus.be.server.service.point.service.PointService
import kr.hhplus.be.server.service.user.entity.User
import kr.hhplus.be.server.service.user.service.UserService
import kr.hhplus.be.server.service.user.usecase.RequiresUserIdExistsUsecase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PointService 단위테스트")
class PointServiceTest : ServiceTestBase() {

    @MockK
    private lateinit var userService: UserService

    @MockK
    private lateinit var requireUserIdExistsUsecase: RequiresUserIdExistsUsecase

    @MockK
    private lateinit var pointPort: PointPort
    
    private lateinit var pointService: PointService

    @BeforeEach
    fun setupPointService() {
        super.setUp()
        pointService = PointService(
            requireUserIdExistsUsecase = requireUserIdExistsUsecase,
            timeProvider = timeProvider,
            pointPort = pointPort
        )
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
            val user = User(id = userId, name = "김철수", point = 5000L)
            val expectedPointChange = PointChange(
                id = 1L, userId = userId, pointChange = chargeAmount, type = PointChangeType.Charge, happenedAt = fixedTime
            )
            
            every { requireUserIdExistsUsecase.requireUserIdExists(userId) } just Runs
            every { pointPort.chargePoint(userId = userId, pointChange = chargeAmount, `when` = fixedTime) } returns expectedPointChange

            // when
            val result: PointChange = pointService.chargePoint(userId = userId, point = chargeAmount)

            // then
            result shouldBe expectedPointChange
            verify { requireUserIdExistsUsecase.requireUserIdExists(userId) }
            verify { pointPort.chargePoint(userId = userId, pointChange = chargeAmount, `when` = fixedTime) }
        }

        @Test
        @DisplayName("사용자 조회 후 포인트 충전을 처리한다")
        fun verifiesUserExistenceBeforeCharging() {
            // given
            val userId = 1L
            val chargeAmount = 5000L
            val user = User(id = userId, name = "김철수", point = 10000L)
            val expectedPointChange = PointChange(
                id = 2L, userId = userId, pointChange = chargeAmount, type = PointChangeType.Charge, happenedAt = fixedTime
            )
            
            every { requireUserIdExistsUsecase.requireUserIdExists(userId) } just Runs
            every { pointPort.chargePoint(userId = userId, pointChange = chargeAmount, `when` = fixedTime) } returns expectedPointChange

            // when
            val result: PointChange = pointService.chargePoint(userId = userId, point = chargeAmount)

            // then
            result shouldBe expectedPointChange
            verify { requireUserIdExistsUsecase.requireUserIdExists(userId) }
            verify { pointPort.chargePoint(userId = userId, pointChange = chargeAmount, `when` = fixedTime) }
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
                id = 3L, userId = userId, pointChange = useAmount, type = PointChangeType.Use, happenedAt = fixedTime
            )
            
            every { requireUserIdExistsUsecase.requireUserIdExists(userId) } returns Unit
            every { pointPort.usePoint(userId = userId, pointChange = useAmount, `when` = fixedTime) } returns expectedPointChange

            // when
            val result: PointChange = pointService.usePoint(userId = userId, point = useAmount)

            // then
            result shouldBe expectedPointChange
            verify { requireUserIdExistsUsecase.requireUserIdExists(userId) }
            verify { pointPort.usePoint(userId = userId, pointChange = useAmount, `when` = fixedTime) }
        }

        @Test
        @DisplayName("사용자 존재 확인 후 포인트 사용을 처리한다")
        fun verifiesUserExistenceBeforeUsing() {
            // given
            val userId = 1L
            val useAmount = 7000L
            val expectedPointChange = PointChange(
                id = 4L, userId = userId, pointChange = useAmount, type = PointChangeType.Use, happenedAt = fixedTime
            )
            
            every { requireUserIdExistsUsecase.requireUserIdExists(userId) } returns Unit
            every { pointPort.usePoint(userId = userId, pointChange = useAmount, `when` = fixedTime) } returns expectedPointChange

            // when
            val result: PointChange = pointService.usePoint(userId = userId, point = useAmount)

            // then
            result shouldBe expectedPointChange
            verify { requireUserIdExistsUsecase.requireUserIdExists(userId) }
            verify { pointPort.usePoint(userId = userId, pointChange = useAmount, `when` = fixedTime) }
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
                id = 1L, userId = 1L, pointChange = 10000L, type = PointChangeType.Charge, happenedAt = fixedTime
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
                id = 2L, userId = 1L, pointChange = 5000L, type = PointChangeType.Use, happenedAt = fixedTime
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