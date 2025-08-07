package kr.hhplus.be.server.repository.jpa.entity

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * 모든 JPA Entity의 공통 기능을 제공하는 추상 클래스
 * - 자동 Auditing (생성일시, 수정일시)
 * - Soft Delete 지원
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
@SQLRestriction("deleted_at IS NULL") // Soft Delete: 삭제된 행 제외
abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0L
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: Instant? = null
        protected set
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    open var updatedAt: Instant? = null
        protected set
    
    @Column(name = "deleted_at")
    open var deletedAt: Instant? = null
        protected set
    
    /**
     * Soft Delete 실행
     */
    fun delete(now: Instant) {
        this.deletedAt = now
    }
    
    /**
     * 삭제된 엔티티인지 확인
     */
    fun isDeleted(): Boolean = deletedAt != null
} 