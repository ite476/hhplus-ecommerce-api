package kr.hhplus.be.server.service.order.event

import kr.hhplus.be.server.service.event.DomainEvent
import kr.hhplus.be.server.service.order.entity.Order

/**
 * 주문 생성이 성공적으로 완료되었음을 나타내는 도메인 이벤트.
 */
data class OrderCreated(
    val order: Order
) : DomainEvent()


