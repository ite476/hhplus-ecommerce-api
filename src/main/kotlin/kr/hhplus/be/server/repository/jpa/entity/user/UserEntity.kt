package kr.hhplus.be.server.repository.jpa.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.hhplus.be.server.repository.jpa.entity.BaseEntity
import kr.hhplus.be.server.service.point.exception.LackOfPointException
import kr.hhplus.be.server.service.point.exception.PointChargeMustBeGreaterThanZeroException
import kr.hhplus.be.server.service.point.exception.PointUsageMustBeGreaterThanZeroException
import org.hibernate.annotations.SQLRestriction

/**
 * 사용자 정보를 저장하는 JPA Entity
 */
@Entity
@Table(name = "user")
@SQLRestriction("deleted_at IS NULL") // Soft Delete: 삭제된 행 제외
class UserEntity(
    @Column(name = "name", nullable = false, length = 100)
    var name: String,
    
    @Column(name = "point", nullable = false)
    var point: Long = 0L
) : BaseEntity() {
    
    /**
     * 포인트 충전
     */
    fun chargePoint(amount: Long) {
        require(amount > 0) {
            throw PointChargeMustBeGreaterThanZeroException()
        }

        this.point += amount
    }
    
    /**
     * 포인트 사용 (차감)
     */
    fun usePoint(amount: Long) {
        require(amount > 0) {
            throw PointUsageMustBeGreaterThanZeroException()
        }
        require(this.point >= amount) {
            throw LackOfPointException()
        }

        this.point -= amount
    }
    
    /**
     * 포인트 사용 가능 여부 확인
     */
    fun canUsePoint(amount: Long): Boolean {
        return this.point >= amount
    }
} 