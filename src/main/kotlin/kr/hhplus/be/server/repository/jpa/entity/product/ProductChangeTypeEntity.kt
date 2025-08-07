package kr.hhplus.be.server.repository.jpa.entity.product

import jakarta.persistence.*
import kr.hhplus.be.server.repository.jpa.enums.ProductChangeType
import org.hibernate.annotations.SQLRestriction

/**
 * 상품 변경 유형의 상세 정보를 저장하는 JPA Entity
 * 주로 코드성 데이터로 사용
 */
@Entity
@Table(name = "product_change_type")
@SQLRestriction("deleted_at IS NULL") // Soft Delete: 삭제된 행 제외
class ProductChangeTypeEntity(
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 50)
    val changeType: ProductChangeType,

    @Column(name = "description", nullable = false, length = 200)
    val description: String
) {
    
    companion object {
        /**
         * ProductChangeType enum으로부터 Entity 생성
         */
        fun from(changeType: ProductChangeType): ProductChangeTypeEntity {
            return ProductChangeTypeEntity(
                changeType = changeType,
                description = changeType.description
            )
        }
    }
} 