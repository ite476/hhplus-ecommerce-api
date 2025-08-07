package kr.hhplus.be.server.repository.jpa.entity.product

import jakarta.persistence.*
import kr.hhplus.be.server.repository.jpa.entity.BaseEntity
import kr.hhplus.be.server.repository.jpa.enums.ProductChangeType
import org.hibernate.annotations.SQLRestriction
import java.time.ZonedDateTime

/**
 * 상품 변경 이력을 저장하는 JPA Entity
 */
@Entity
@Table(name = "product_history")
@SQLRestriction("deleted_at IS NULL") // Soft Delete: 삭제된 행 제외
class ProductHistoryEntity(
    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    @Column(name = "price", nullable = false)
    val price: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 50)
    val changeType: ProductChangeType,

    @Column(name = "changed_at", nullable = false)
    val changedAt: ZonedDateTime = ZonedDateTime.now()
) : BaseEntity() {
    
    // 읽기 전용으로 설계된 이력 데이터이므로 비즈니스 로직 없음
} 