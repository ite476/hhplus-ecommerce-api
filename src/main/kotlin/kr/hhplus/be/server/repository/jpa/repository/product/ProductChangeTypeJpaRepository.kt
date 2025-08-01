package kr.hhplus.be.server.repository.jpa.repository.product

import kr.hhplus.be.server.repository.jpa.entity.product.ProductChangeTypeEntity
import kr.hhplus.be.server.repository.jpa.enums.ProductChangeType
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 상품 변경 유형 Entity를 위한 JPA Repository
 * 주로 코드성 데이터 관리용
 */
interface ProductChangeTypeJpaRepository : JpaRepository<ProductChangeTypeEntity, ProductChangeType> {
} 