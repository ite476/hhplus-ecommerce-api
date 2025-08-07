package kr.hhplus.be.server.repository.jpa.repository.coupon

import kr.hhplus.be.server.repository.jpa.entity.coupon.UserCouponEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 사용자 쿠폰 Entity를 위한 JPA Repository
 */
interface UserCouponJpaRepository : JpaRepository<UserCouponEntity, Long> {
    /**
     * 특정 사용자의 쿠폰 조회
     */
    fun findByUserId(userId: Long, pageable: Pageable): Page<UserCouponEntity>
} 