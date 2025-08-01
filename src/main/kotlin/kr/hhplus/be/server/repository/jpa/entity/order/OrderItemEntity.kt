package kr.hhplus.be.server.repository.jpa.entity.order

import jakarta.persistence.*
import kr.hhplus.be.server.repository.jpa.entity.BaseEntity
import kr.hhplus.be.server.repository.jpa.entity.product.ProductEntity
import org.hibernate.annotations.SQLRestriction

/**
 * 주문 상품 정보를 저장하는 JPA Entity
 */
@Entity
@Table(name = "order_item")
@SQLRestriction("deleted_at IS NULL") // Soft Delete: 삭제된 행 제외
class OrderItemEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: ProductEntity,

    @Column(name = "unit_price", nullable = false)
    val unitPrice: Long,

    @Column(name = "quantity", nullable = false)
    val quantity: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private var order: OrderEntity? = null
) : BaseEntity() {
    
    /**
     * 주문 설정 (내부 사용)
     */
    internal fun setOrder(order: OrderEntity) {
        this.order = order
    }
    
    /**
     * 해당 주문 항목의 총 가격 계산
     */
    fun getTotalPrice(): Long {
        return unitPrice * quantity
    }
    
    /**
     * 수량 검증
     */
    fun validateQuantity() {
        require(quantity > 0) { "주문 수량은 0보다 커야 합니다" }
    }
    
    /**
     * 단가 검증 (상품 가격과 일치하는지)
     */
    fun validateUnitPrice() {
        require(unitPrice == product.price) { 
            "주문 단가(${unitPrice})가 상품 가격(${product.price})과 일치하지 않습니다" 
        }
    }
} 