package kr.hhplus.be.server.repository.jpa.repository.coupon

import kr.hhplus.be.server.repository.jpa.entity.coupon.CouponEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 쿠폰 Entity를 위한 JPA Repository
 */
interface CouponJpaRepository : JpaRepository<CouponEntity, Long> {
} 