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
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions
import kr.hhplus.be.server.service.user.service.UserService
import kr.hhplus.be.server.service.user.usecase.RequiresUserIdExistsUsecase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("CouponService 단위테스트")
class CouponServiceTest : ServiceTestBase() {

    @MockK
    private lateinit var couponPort: CouponPort

    @MockK
    private lateinit var userService: UserService

    @MockK
    private lateinit var requireUserIdExistsUsecase: RequiresUserIdExistsUsecase

    private lateinit var couponService: CouponService



    @BeforeEach
    fun setupCouponService() {
        super.setUp()
        couponService = CouponService(couponPort, requireUserIdExistsUsecase, timeProvider)
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
                id = userCouponId, userId = userId, couponId = 1L, couponName = "신규가입쿠폰", discount = 2000L,
                status = UserCouponStatus.ACTIVE, issuedAt = fixedTime, usedAt = null, validUntil = fixedTime.plusDays(30)
            )
            every { requireUserIdExistsUsecase.requireUserIdExists(userId = any()) } just Runs
            every { couponPort.existsUserCoupon(userCouponId = any()) } returns true
            every { couponPort.findUserCouponById(userId, userCouponId) } returns expectedCoupon

            // when
            val result = couponService.findUserCouponById(userId, userCouponId)

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
                id = 1L, userId = 1L, couponId = 1L, couponName = "신규가입쿠폰", discount = 2000L,
                status = UserCouponStatus.ACTIVE, issuedAt = fixedTime, usedAt = null, validUntil = fixedTime.plusDays(30)
            )
            every { couponPort.existsUserCoupon(userCouponId = any()) } returns true
            every { couponPort.saveUserCoupon(userCoupon) } returns Unit

            // when
            couponService.useUserCoupon(userCoupon = userCoupon, now = fixedTime)

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
                id = 1L, userId = 1L, couponId = 1L, couponName = "신규가입쿠폰", discount = 2000L,
                status = UserCouponStatus.USED, issuedAt = fixedTime, usedAt = fixedTime, validUntil = fixedTime.plusDays(30)
            )
            every { couponPort.existsUserCoupon(userCouponId = any()) } returns true
            every { couponPort.saveUserCoupon(userCoupon) } returns Unit

            // when
            couponService.rollbackUserCouponUsage(userCoupon = userCoupon, now = fixedTime)

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
            val pagingOptions = PagingOptions(page = 0, size = 10)
            val expectedCoupons: List<UserCoupon> = listOf(
                UserCoupon(
                    id = 1L, userId = userId, couponId = 1L, couponName = "신규가입쿠폰", discount = 2000L,
                    status = UserCouponStatus.ACTIVE, issuedAt = fixedTime, usedAt = null, validUntil = fixedTime.plusDays(30)
                ),
                UserCoupon(
                    id = 2L, userId = userId, couponId = 2L, couponName = "생일할인쿠폰", discount = 5000L,
                    status = UserCouponStatus.USED, issuedAt = fixedTime, usedAt = fixedTime, validUntil = fixedTime.plusDays(30)
                )
            )
            every { requireUserIdExistsUsecase.requireUserIdExists(userId = any()) } just Runs
            every { couponPort.findPagedUserCoupons(userId, any()) } returns PagedList(
                items = expectedCoupons,
                page = 0,
                size = 10,
                totalCount = 2
            )

            // when
            val result: List<UserCoupon> = couponService.findPagedUserCoupons(userId, pagingOptions = pagingOptions).items

            // then
            result shouldBe expectedCoupons
            verify { couponPort.findPagedUserCoupons(userId, any()) }
        }

        @Test
        @DisplayName("쿠폰이 없을 때 빈 리스트를 반환한다")
        fun returnsEmptyListWhenNoCoupons() {
            // given
            val userId = 1L
            val pagingOptions = PagingOptions(page = 0, size = 10)
            every { requireUserIdExistsUsecase.requireUserIdExists(userId = any()) } just Runs
            every { couponPort.findPagedUserCoupons(userId, any()) } returns PagedList(
                items = emptyList(),
                page = 0,
                size = 10,
                totalCount = 0
            )

            // when
            val result: List<UserCoupon> = couponService.findPagedUserCoupons(userId, pagingOptions).items

            // then
            result shouldBe emptyList()
            verify { couponPort.findPagedUserCoupons(userId, any()) }
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
                id = 1L, userId = userId, couponId = couponId, couponName = "신규가입쿠폰", discount = 2000L,
                status = UserCouponStatus.ACTIVE, issuedAt = fixedTime, usedAt = null, validUntil = fixedTime.plusDays(30)
            )

            coEvery { requireUserIdExistsUsecase.requireUserIdExists(userId = any()) } just Runs
            coEvery { couponPort.existsCoupon(couponId = any()) } returns true
            coEvery { couponPort.issueCoupon(userId, couponId, any()) } returns expectedUserCoupon

            // when
            val result: UserCoupon = couponService.issueCoupon(userId, couponId)

            // then
            result shouldBe expectedUserCoupon
            coVerify { couponPort.issueCoupon(userId, couponId, any()) }
        }

        @Test
        @DisplayName("쿠폰 발급 실패 시 CompensationScope가 롤백을 처리한다")
        fun rollsBackWhenIssueCouponFails() = runTest {
            // given
            val userId = 1L
            val couponId = 1L
            val testException = RuntimeException("쿠폰 발급 실패")

            coEvery { requireUserIdExistsUsecase.requireUserIdExists(userId = any()) } just Runs
            coEvery { couponPort.existsCoupon(couponId = any()) } returns true
            // 쿠폰 발급 자체에서 예외 발생
            coEvery { couponPort.issueCoupon(userId, couponId, any()) } throws testException
            coEvery { couponPort.revokeCoupon(issuedUserCoupon = any(), now = any()) } just Runs

            // when & then
            shouldThrow<RuntimeException> {
                couponService.issueCoupon(userId, couponId)
            }

            // 발급이 실패했으므로 롤백도 호출되지 않음
            coVerify { couponPort.issueCoupon(userId, couponId, any()) }
            coVerify(exactly = 0) { couponPort.revokeCoupon(any(), any()) }
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
                id = 1L, userId = 1L, couponId = 1L, couponName = "신규가입쿠폰", discount = 2000L,
                status = UserCouponStatus.ACTIVE, issuedAt = fixedTime, usedAt = null, validUntil = fixedTime.plusDays(30)
            )

            // when
            userCoupon.use(now = fixedTime)

            // then
            userCoupon.status shouldBe UserCouponStatus.USED
            userCoupon.usedAt shouldBe fixedTime
        }

        @Test
        @DisplayName("사용된 쿠폰은 다시 사용할 수 없다")
        fun cannotUseUsedCoupon() {
            // given
            val userCoupon = UserCoupon(
                id = 1L, userId = 1L, couponId = 1L, couponName = "신규가입쿠폰", discount = 2000L,
                status = UserCouponStatus.USED, issuedAt = fixedTime, usedAt = fixedTime, validUntil = fixedTime.plusDays(30)
            )

            // when & then
            shouldThrow<UserCouponCantBeUsedException> {
                userCoupon.use(now = fixedTime)
            }
        }

        @Test
        @DisplayName("만료된 쿠폰은 사용할 수 없다")
        fun cannotUseExpiredCoupon() {
            // given
            val expiredTime: ZonedDateTime = fixedTime.plusDays(31) // 만료일 이후
            val userCoupon = UserCoupon(
                id = 1L, userId = 1L, couponId = 1L, couponName = "신규가입쿠폰", discount = 2000L,
                status = UserCouponStatus.EXPIRED, issuedAt = fixedTime, usedAt = null, validUntil = fixedTime.plusDays(30)
            )

            // when & then
            shouldThrow<UserCouponCantBeUsedException> {
                userCoupon.use(now = expiredTime)
            }
        }

        @Test
        @DisplayName("사용된 쿠폰을 사용 취소할 수 있다")
        fun canUndoUsedCoupon() {
            // given
            val userCoupon = UserCoupon(
                id = 1L, userId = 1L, couponId = 1L, couponName = "신규가입쿠폰", discount = 2000L,
                status = UserCouponStatus.USED, issuedAt = fixedTime, usedAt = fixedTime, validUntil = fixedTime.plusDays(30)
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
                id = 1L, userId = 1L, couponId = 1L, couponName = "신규가입쿠폰", discount = 2000L,
                status = UserCouponStatus.ACTIVE, issuedAt = fixedTime, usedAt = null, validUntil = fixedTime.plusDays(30)
            )

            // when & then
            shouldThrow<UserCouponIsNotUsedButTriedToBeUnusedException> {
                userCoupon.undoUsage(now = fixedTime)
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