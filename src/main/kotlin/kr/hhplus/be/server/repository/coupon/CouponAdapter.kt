package kr.hhplus.be.server.repository.coupon

import kr.hhplus.be.server.repository.jpa.entity.coupon.CouponEntity
import kr.hhplus.be.server.repository.jpa.entity.coupon.UserCouponEntity
import kr.hhplus.be.server.repository.jpa.entity.user.UserEntity
import kr.hhplus.be.server.repository.jpa.repository.coupon.CouponJpaRepository
import kr.hhplus.be.server.repository.jpa.repository.coupon.UserCouponJpaRepository
import kr.hhplus.be.server.repository.jpa.repository.user.UserJpaRepository
import kr.hhplus.be.server.repository.pagination.toPageable
import kr.hhplus.be.server.repository.pagination.toPagedList
import kr.hhplus.be.server.service.coupon.entity.UserCoupon
import kr.hhplus.be.server.service.coupon.entity.UserCouponStatus
import kr.hhplus.be.server.service.coupon.exception.CouponNotFoundException
import kr.hhplus.be.server.service.coupon.exception.UserCouponNotFoundException
import kr.hhplus.be.server.service.coupon.exception.UserCouponNotOwnedButTriedToBeUsedException
import kr.hhplus.be.server.service.coupon.port.CouponPort
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions
import kr.hhplus.be.server.service.user.exception.UserNotFoundException
import kr.hhplus.be.server.util.unwrapOrThrow
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class CouponAdapter(
    private val couponRepository: CouponJpaRepository,
    private val userCouponRepository: UserCouponJpaRepository,
    private val userRepository: UserJpaRepository
) : CouponPort {

    override fun findUserCouponById(userId: Long, userCouponId: Long): UserCoupon {
        val userCouponEntity = userCouponRepository.findById(userCouponId)
            .unwrapOrThrow { UserCouponNotFoundException() }

        require(userCouponEntity.user.id == userId) {
            throw UserCouponNotOwnedButTriedToBeUsedException()
        }

        return userCouponEntity.toDomain()
    }

    override fun saveUserCoupon(userCoupon: UserCoupon) {
        val userEntity: UserEntity = userRepository.findById(userCoupon.userId)
            .unwrapOrThrow { UserNotFoundException() }

        val couponEntity: CouponEntity = couponRepository.findById(userCoupon.couponId)
            .unwrapOrThrow { CouponNotFoundException() }

        val userCouponEntity: UserCouponEntity = userCouponRepository.findById(userCoupon.id)
            .unwrapOrThrow { UserCouponNotFoundException() }
            .run {
                updateFromDomain(userCoupon)
            }

        userCouponRepository.save(userCouponEntity)
    }

    override fun findPagedUserCoupons(userId: Long, pagingOptions: PagingOptions): PagedList<UserCoupon> {
        // 사용자 존재 확인
        userRepository.findById(userId)
            .unwrapOrThrow { UserNotFoundException() }

        val userCouponEntities: PagedList<UserCoupon> = userCouponRepository.findByUserId(userId, pagingOptions.toPageable())
            .toPagedList()
            .let { page ->
                PagedList(
                    items = page.items.map { it.toDomain() },
                    page = page.page,
                    size = page.size,
                    totalCount = page.totalCount
                )
            }

        return userCouponEntities
    }

    @Transactional
    override fun issueCoupon(userId: Long, couponId: Long, now: ZonedDateTime): UserCoupon {
        // 1. 유저 조회
        val userEntity = userRepository.findById(userId)
            .unwrapOrThrow { UserNotFoundException() }

        // 2. 쿠폰 원자적 증가 (동시성 안전)
        val updateCount = couponRepository.incrementIssuedQuantityIfAvailable(couponId)
        
        if (updateCount == 0) {
            // 쿠폰 발급 실패 - 수량 부족이거나 만료됨
            val couponEntity = couponRepository.findById(couponId)
                .unwrapOrThrow { CouponNotFoundException() }
            
            when {
                couponEntity.isSoldOut() -> throw IllegalStateException("쿠폰이 모두 발급되었습니다")
                couponEntity.isExpired() -> throw IllegalStateException("만료된 쿠폰입니다")
                else -> throw IllegalStateException("쿠폰 발급에 실패했습니다")
            }
        }

        // 3. 업데이트된 쿠폰 정보 조회
        val couponEntity = couponRepository.findById(couponId)
            .unwrapOrThrow { CouponNotFoundException() }

        // 4. UserCouponEntity 생성 (중복 발급 방지 제약조건 적용)
        val userCouponEntity = UserCouponEntity(
            user = userEntity,
            coupon = couponEntity,
            status = UserCouponStatus.ACTIVE,
            issuedAt = now,
            expiredAt = couponEntity.expiredAt
        )

        try {
            // 5. 저장 (Unique 제약조건으로 중복 발급 방지)
            val saved = userCouponRepository.save(userCouponEntity)
            return saved.toDomain()
            
        } catch (e: DataIntegrityViolationException) {
            // 중복 발급 시도 - 이미 발급된 쿠폰을 다시 발급하려고 시도
            throw IllegalStateException("이미 발급받은 쿠폰입니다", e)
        }
    }

    override fun revokeCoupon(issuedUserCoupon: UserCoupon, now: ZonedDateTime) {
        val userCouponEntity: UserCouponEntity = userCouponRepository.findById(issuedUserCoupon.id)
            .unwrapOrThrow { UserCouponNotFoundException() }

        // Soft Delete 적용
        userCouponEntity.delete(now.toInstant())

        // 쿠폰 조회 및 발행 취소 처리
        val couponEntity = couponRepository.findById(userCouponEntity.coupon.id)
            .unwrapOrThrow { CouponNotFoundException() }
            .run {
                issuedQuantity -= 1
                couponRepository.save(this)
            }

        userCouponRepository.save(userCouponEntity)
    }

    override fun existsUserCoupon(userCouponId: Long): Boolean {
        return userCouponRepository.existsById(userCouponId)
    }

    override fun existsCoupon(couponId: Long): Boolean {
        return couponRepository.existsById(couponId)
    }
}