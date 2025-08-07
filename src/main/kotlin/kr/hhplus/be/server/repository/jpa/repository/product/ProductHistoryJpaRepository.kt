package kr.hhplus.be.server.repository.jpa.repository.product

import kr.hhplus.be.server.repository.jpa.entity.product.ProductHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 상품 변경 이력 Entity를 위한 JPA Repository
 */
interface ProductHistoryJpaRepository : JpaRepository<ProductHistoryEntity, Long> {
} 