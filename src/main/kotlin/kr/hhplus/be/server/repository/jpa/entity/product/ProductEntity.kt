package kr.hhplus.be.server.repository.jpa.entity.product

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.hhplus.be.server.repository.jpa.entity.BaseEntity
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.util.TimeProvider
import org.hibernate.annotations.SQLRestriction

/**
 * 상품 정보를 저장하는 JPA Entity
 */
@Entity
@Table(name = "product")
@SQLRestriction("deleted_at IS NULL") // Soft Delete: 삭제된 행 제외
class ProductEntity(
    @Column(name = "name", nullable = false, length = 200)
    var name: String,
    
    @Column(name = "price", nullable = false)
    var price: Long,
    
    @Column(name = "stock", nullable = false)
    var stock: Long
) : BaseEntity() {
    
    /**
     * 재고 추가
     */
    fun addStock(quantity: Int) {
        require(quantity > 0) { "추가할 재고 수량은 0보다 커야 합니다" }
        this.stock += quantity
    }
    
    /**
     * 재고 차감
     */
    fun reduceStock(quantity: Int) {
        require(quantity > 0) { "차감할 재고 수량은 0보다 커야 합니다" }
        require(this.stock >= quantity) { "재고가 부족합니다" }
        this.stock -= quantity
    }
    
    /**
     * 재고 사용 가능 여부 확인
     */
    fun canReduceStock(quantity: Int): Boolean {
        return this.stock >= quantity
    }
    
    /**
     * 품절 상태 확인
     */
    fun isOutOfStock(): Boolean {
        return this.stock <= 0
    }

    fun toDomain(timeProvider: TimeProvider): Product{
        return Product(
            id = id,
            name = name,
            price = price,
            stock = stock,
            createdAt = requireNotNull(createdAt) {
                "ProductEntity.createdAt is null (ID=${id}). 데이터 정합성 확인 필요"
            }.let {
                timeProvider.toZonedDateTime(it)
            }
        )
    }

    fun updateAsDomain(product: Product) {
        this.name = product.name
        this.price = product.price
        this.stock = product.stock
    }
}

fun Product.toEntity(): ProductEntity {
    val entity = ProductEntity(
        name = this.name,
        price = this.price,
        stock = this.stock
    )

    // id는 BaseEntity 쪽에서 auto-generated라 보통 직접 세팅하지 않음.
    // 필요하다면 Reflection이나 EntityManager.persist() 시점에서 채워짐.
    // createdAt, updatedAt, deletedAt 도 마찬가지.

    return entity
}
