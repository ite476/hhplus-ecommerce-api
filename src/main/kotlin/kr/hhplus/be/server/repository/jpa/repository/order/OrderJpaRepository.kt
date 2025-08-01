package kr.hhplus.be.server.repository.jpa.repository.order

import kr.hhplus.be.server.repository.jpa.entity.order.OrderEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 주문 Entity를 위한 JPA Repository
 */
interface OrderJpaRepository : JpaRepository<OrderEntity, Long> {
} 