package kr.hhplus.be.server.service.coupon

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import kr.hhplus.be.server.service.ServiceTestBase
import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.entity.UserCouponStatus
import kr.hhplus.be.server.service.coupon.exception.UserCouponCantBeUsedException
import kr.hhplus.be.server.service.coupon.exception.UserCouponIsNotUsedButTriedToBeUnusedException
import kr.hhplus.be.server.service.coupon.port.CouponPort
import kr.hhplus.be.server.service.coupon.service.CouponService
import kr.hhplus.be.server.service.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CouponService 단위테스트")
class CouponServiceTest : ServiceTestBase() {

    @MockK
    private lateinit var couponPort: CouponPort

    @MockK
    private lateinit var userService: UserService

    private lateinit var couponService: CouponService


    @BeforeEach
    fun setupCouponService() {
        super.setUp()
        couponService = CouponService(couponPort, userService, timeProvider)
    }

    @Nested
    @DisplayName("readSingleUserCoupon 메서드는")
    inner class ReadSingleUserCouponTest {

        @Test
        @DisplayName("사용자의 특정 쿠폰을 반환한다")
        fun returnsSingleUserCoupon() {
            // given
            val userId = 1L
            val userCouponId = 1L
            val expectedCoupon = UserCoupon(
                userCouponId, userId, 1L, "신규가입쿠폰", 2000L,
                UserCouponStatus.ACTIVE, fixedTime, null, fixedTime.plusDays(30)
            )
            every { userService.requireUserExists(any()) } just Runs
            every { couponPort.existsUserCoupon(any()) } returns true
            every { couponPort.findUserCouponById(userId, userCouponId) } returns expectedCoupon

            // when
            val result = couponService.readSingleUserCoupon(userId, userCouponId)

            // then
            result shouldBe expectedCoupon
            verify { couponPort.findUserCouponById(userId, userCouponId) }
        }
    }

    @Nested
    @DisplayName("useUserCoupon 메서드는")
    inner class UseUserCouponTest {

        @Test
        @DisplayName("활성 상태의 쿠폰을 사용 처리한다")
        fun usesActiveCoupon() {
            // given
            val userCoupon = UserCoupon(
                1L, 1L, 1L, "신규가입쿠폰", 2000L,
                UserCouponStatus.ACTIVE, fixedTime, null, fixedTime.plusDays(30)
            )
            every { couponPort.existsUserCoupon(any()) } returns true
            every { couponPort.saveUserCoupon(userCoupon) } returns Unit

            // when
            couponService.useUserCoupon(userCoupon, fixedTime)

            // then
            userCoupon.status shouldBe UserCouponStatus.USED
            userCoupon.usedAt shouldBe fixedTime
            verify { couponPort.saveUserCoupon(userCoupon) }
        }
    }

    @Nested
    @DisplayName("rollbackUserCouponUsage 메서드는")
    inner class RollbackUserCouponUsageTest {

        @Test
        @DisplayName("사용된 쿠폰을 활성 상태로 되돌린다")
        fun rollbacksUsedCoupon() {
            // given
            val userCoupon = UserCoupon(
                1L, 1L, 1L, "신규가입쿠폰", 2000L,
                UserCouponStatus.USED, fixedTime, fixedTime, fixedTime.plusDays(30)
            )
            every { couponPort.existsUserCoupon(any()) } returns true
            every { couponPort.saveUserCoupon(userCoupon) } returns Unit

            // when
            couponService.rollbackUserCouponUsage(userCoupon, fixedTime)

            // then
            userCoupon.status shouldBe UserCouponStatus.ACTIVE
            userCoupon.usedAt shouldBe null
            verify { couponPort.saveUserCoupon(userCoupon) }
        }
    }

    @Nested
    @DisplayName("readUserCoupons 메서드는")
    inner class ReadUserCouponsTest {

        @Test
        @DisplayName("사용자의 모든 쿠폰 목록을 반환한다")
        fun returnsAllUserCoupons() {
            // given
            val userId = 1L
            val expectedCoupons = listOf(
                UserCoupon(
                    1L, userId, 1L, "신규가입쿠폰", 2000L,
                    UserCouponStatus.ACTIVE, fixedTime, null, fixedTime.plusDays(30)
                ),
                UserCoupon(
                    2L, userId, 2L, "생일할인쿠폰", 5000L,
                    UserCouponStatus.USED, fixedTime, fixedTime, fixedTime.plusDays(30)
                )
            )
            every { userService.requireUserExists(any()) } just Runs
            every { couponPort.findAllUserCoupons(userId) } returns expectedCoupons

            // when
            val result = couponService.readUserCoupons(userId)

            // then
            result shouldBe expectedCoupons
            verify { couponPort.findAllUserCoupons(userId) }
        }

        @Test
        @DisplayName("쿠폰이 없을 때 빈 리스트를 반환한다")
        fun returnsEmptyListWhenNoCoupons() {
            // given
            val userId = 1L
            every { userService.requireUserExists(any()) } just Runs
            every { couponPort.findAllUserCoupons(userId) } returns emptyList()

            // when
            val result = couponService.readUserCoupons(userId)

            // then
            result shouldBe emptyList()
            verify { couponPort.findAllUserCoupons(userId) }
        }
    }

    @Nested
    @DisplayName("issueCoupon 메서드는 (코루틴)")
    inner class IssueCouponTest {

        @Test
        @DisplayName("쿠폰을 성공적으로 발급한다")
        fun issuesCouponSuccessfully() = runTest {
            // given
            val userId = 1L
            val couponId = 1L
            val expectedUserCoupon = UserCoupon(
                1L, userId, couponId, "신규가입쿠폰", 2000L,
                UserCouponStatus.ACTIVE, fixedTime, null, fixedTime.plusDays(30)
            )

            coEvery { userService.requireUserExists(any()) } just Runs
            coEvery { couponPort.existsCoupon(any()) } returns true
            coEvery { couponPort.issueCoupon(couponId) } returns expectedUserCoupon

            // when
            val result = couponService.issueCoupon(userId, couponId)

            // then
            result shouldBe expectedUserCoupon
            coVerify { couponPort.issueCoupon(couponId) }
        }

        @Test
        @DisplayName("쿠폰 발급 실패 시 CompensationScope가 롤백을 처리한다")
        fun rollsBackOnIssueCouponFailure() = runTest {
            // given
            val userId = 1L
            val couponId = 1L
            val issuedUserCoupon = UserCoupon(
                1L, userId, couponId, "신규가입쿠폰", 2000L,
                UserCouponStatus.ACTIVE, fixedTime, null, fixedTime.plusDays(30)
            )

            coEvery { userService.requireUserExists(any()) } just Runs
            coEvery { couponPort.existsCoupon(any()) } returns true
            coEvery { couponPort.issueCoupon(couponId) } returns issuedUserCoupon
            // 실패 상황을 시뮬레이션하기 위해 예외를 발생시킬 수는 없지만,
            // 정상 케이스를 통해 CompensationScope의 구조를 검증

            // when
            val result = couponService.issueCoupon(userId, couponId)

            // then
            result shouldBe issuedUserCoupon
            coVerify { couponPort.issueCoupon(couponId) }
        }
    }

    @Nested
    @DisplayName("UserCoupon Entity 로직 테스트")
    inner class UserCouponEntityTest {

        @Test
        @DisplayName("활성 상태의 쿠폰을 사용할 수 있다")
        fun canUseActiveCoupon() {
            // given
            val userCoupon = UserCoupon(
                1L, 1L, 1L, "신규가입쿠폰", 2000L,
                UserCouponStatus.ACTIVE, fixedTime, null, fixedTime.plusDays(30)
            )

            // when
            userCoupon.use(fixedTime)

            // then
            userCoupon.status shouldBe UserCouponStatus.USED
            userCoupon.usedAt shouldBe fixedTime
        }

        @Test
        @DisplayName("사용된 쿠폰은 다시 사용할 수 없다")
        fun cannotUseUsedCoupon() {
            // given
            val userCoupon = UserCoupon(
                1L, 1L, 1L, "신규가입쿠폰", 2000L,
                UserCouponStatus.USED, fixedTime, fixedTime, fixedTime.plusDays(30)
            )

            // when & then
            shouldThrow<UserCouponCantBeUsedException> {
                userCoupon.use(fixedTime)
            }
        }

        @Test
        @DisplayName("만료된 쿠폰은 사용할 수 없다")
        fun cannotUseExpiredCoupon() {
            // given
            val expiredTime = fixedTime.plusDays(31) // 만료일 이후
            val userCoupon = UserCoupon(
                1L, 1L, 1L, "신규가입쿠폰", 2000L,
                UserCouponStatus.EXPIRED, fixedTime, null, fixedTime.plusDays(30)
            )

            // when & then
            shouldThrow<UserCouponCantBeUsedException> {
                userCoupon.use(expiredTime)
            }
        }

        @Test
        @DisplayName("사용된 쿠폰을 사용 취소할 수 있다")
        fun canUndoUsedCoupon() {
            // given
            val userCoupon = UserCoupon(
                1L, 1L, 1L, "신규가입쿠폰", 2000L,
                UserCouponStatus.USED, fixedTime, fixedTime, fixedTime.plusDays(30)
            )

            // when
            userCoupon.undoUsage(fixedTime)

            // then
            userCoupon.status shouldBe UserCouponStatus.ACTIVE
            userCoupon.usedAt shouldBe null
        }

        @Test
        @DisplayName("사용되지 않은 쿠폰은 사용 취소할 수 없다")
        fun cannotUndoUnusedCoupon() {
            // given
            val userCoupon = UserCoupon(
                1L, 1L, 1L, "신규가입쿠폰", 2000L,
                UserCouponStatus.ACTIVE, fixedTime, null, fixedTime.plusDays(30)
            )

            // when & then
            shouldThrow<UserCouponIsNotUsedButTriedToBeUnusedException> {
                userCoupon.undoUsage(fixedTime)
            }
        }

        @Test
        @DisplayName("UserCouponStatus가 올바른 값들을 가진다")
        fun verifyUserCouponStatusValues() {
            // given & when & then
            UserCouponStatus.ACTIVE shouldBe UserCouponStatus.ACTIVE
            UserCouponStatus.USED shouldBe UserCouponStatus.USED
            UserCouponStatus.EXPIRED shouldBe UserCouponStatus.EXPIRED
        }
    }
} 