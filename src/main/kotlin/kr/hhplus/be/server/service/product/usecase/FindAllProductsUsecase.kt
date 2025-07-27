package kr.hhplus.be.server.service.product.usecase

import kr.hhplus.be.server.service.product.entity.Product

interface FindAllProductsUsecase {
    /**
     * 전체 상품 목록 조회
     */
    fun findAllProducts() : List<Product>
}
