package kr.hhplus.be.server.service.product.usecase

import java.time.ZonedDateTime

interface AddProductStockUsecase {
    /**
     * 상품 재고 증가 처리
     */
    fun addProductStock(productId: Long, quantity: Int, now: ZonedDateTime)
}
