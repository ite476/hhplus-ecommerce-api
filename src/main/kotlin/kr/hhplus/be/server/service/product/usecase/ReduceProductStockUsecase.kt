package kr.hhplus.be.server.service.product.usecase

import java.time.ZonedDateTime

interface ReduceProductStockUsecase {
    /**
     * 상품 재고 차감 처리
     */
    fun reduceProductStock(productId: Long, quantity: Int, now: ZonedDateTime)
}
