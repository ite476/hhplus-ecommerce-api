package kr.hhplus.be.server.repository.jpa.repository.coupon

import jakarta.persistence.LockModeType
import kr.hhplus.be.server.repository.jpa.entity.coupon.CouponEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

/**
 * 쿠폰 Entity를 위한 JPA Repository
 */
interface CouponJpaRepository : JpaRepository<CouponEntity, Long> {
    
    /**
     * 쿠폰 조회 (Pessimistic Write Lock)
     * 동시성 제어를 위해 배타적 락을 건다
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CouponEntity c WHERE c.id = :couponId AND c.deletedAt IS NULL")
    fun findByIdWithLock(@Param("couponId") couponId: Long): Optional<CouponEntity>
    
    /**
     * 쿠폰 발급 수량 원자적 증가
     * 조건부 업데이트로 동시성 문제 해결
     * 
     * @return 업데이트된 행의 수 (성공: 1, 실패: 0)
     */
    @Modifying
    @Query("""
        UPDATE CouponEntity c 
        SET c.issuedQuantity = c.issuedQuantity + 1, c.version = c.version + 1
        WHERE c.id = :couponId 
          AND c.issuedQuantity < c.totalQuantity 
          AND c.expiredAt > CURRENT_TIMESTAMP
          AND c.deletedAt IS NULL
    """)
    fun incrementIssuedQuantityIfAvailable(@Param("couponId") couponId: Long): Int
} 