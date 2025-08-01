package kr.hhplus.be.server.repository.jpa.repository.product

import kr.hhplus.be.server.repository.jpa.entity.product.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 상품 Entity를 위한 JPA Repository
 */
interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
}