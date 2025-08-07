package kr.hhplus.be.server.repository.jpa.repository.order

import kr.hhplus.be.server.repository.jpa.entity.order.OrderItemEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 주문 상품 Entity를 위한 JPA Repository
 */
interface OrderItemJpaRepository : JpaRepository<OrderItemEntity, Long> {
} 