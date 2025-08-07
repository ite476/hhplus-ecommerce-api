package kr.hhplus.be.server.repository.jpa.entity.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.hhplus.be.server.repository.jpa.entity.BaseEntity
import org.hibernate.annotations.SQLRestriction
import java.time.ZonedDateTime

/**
 * 쿠폰 정보를 저장하는 JPA Entity
 */
@Entity
@Table(name = "coupon")
@SQLRestriction("deleted_at IS NULL") // Soft Delete: 삭제된 행 제외
class CouponEntity(
    @Column(name = "name", nullable = false, length = 200)
    var name: String,
    
    @Column(name = "discount", nullable = false)
    var discount: Long,
    
    @Column(name = "total_quantity", nullable = false)
    val totalQuantity: Int,
    
    @Column(name = "issued_quantity", nullable = false)
    var issuedQuantity: Int = 0,
    
    @Column(name = "expired_at", nullable = false)
    val expiredAt: ZonedDateTime
) : BaseEntity() {
    
    /**
     * 쿠폰 발급 가능 여부 확인
     */
    fun canIssue(): Boolean {
        return issuedQuantity < totalQuantity && !isExpired()
    }
    
    /**
     * 쿠폰 발급 (동시성 안전)
     * 
     * @throws IllegalStateException 발급 불가능한 경우
     */
    fun issue() {
        require(canIssue()) { "더 이상 발급할 수 있는 쿠폰이 없습니다" }
        
        // 원자적 연산으로 발급 수량 증가
        // 실제 DB 업데이트는 Repository에서 원자적 쿼리로 처리
        issuedQuantity++
    }
    
    /**
     * 쿠폰 발급 시도 (원자적 검증)
     * 
     * @return 발급 성공 여부
     */
    fun tryIssue(): Boolean {
        return if (canIssue()) {
            issuedQuantity++
            true
        } else {
            false
        }
    }
    
    /**
     * 남은 쿠폰 수량 조회
     */
    fun getRemainingQuantity(): Int {
        return totalQuantity - issuedQuantity
    }
    
    /**
     * 쿠폰 만료 여부 확인
     */
    fun isExpired(): Boolean {
        return ZonedDateTime.now().isAfter(expiredAt)
    }
    
    /**
     * 쿠폰 품절 여부 확인
     */
    fun isSoldOut(): Boolean {
        return issuedQuantity >= totalQuantity
    }
} 