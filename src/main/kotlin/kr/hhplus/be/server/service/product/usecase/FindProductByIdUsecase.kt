package kr.hhplus.be.server.service.product.usecase

import kr.hhplus.be.server.service.product.entity.Product

interface FindProductByIdUsecase {
    /**
     * 상품을 Id로 검색
     */
    fun findProductById(productId: Long) : Product

}
