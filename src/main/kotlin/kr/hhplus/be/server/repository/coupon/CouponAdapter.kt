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
import org.springframework.stereotype.Component
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

    override fun issueCoupon(userId: Long, couponId: Long, now: ZonedDateTime): UserCoupon {
        // 유저 조회
        val userEntity = userRepository.findById(userId)
            .unwrapOrThrow { UserNotFoundException() }

        // 쿠폰 조회 및 차감 처리
        val couponEntity = couponRepository.findById(couponId)
            .unwrapOrThrow { CouponNotFoundException() }
            .run {
                issuedQuantity += 1
                couponRepository.save(this)
            }

        // UserCouponEntity 생성
        val userCouponEntity = UserCouponEntity(
            user = userEntity,
            coupon = couponEntity,
            status = UserCouponStatus.ACTIVE,
            issuedAt = now,
            expiredAt = couponEntity.expiredAt // 쿠폰 엔티티가 가진 만료일시 그대로
        )

        // 저장
        val saved = userCouponRepository.save(userCouponEntity)

        // 도메인으로 변환
        return saved.toDomain()
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