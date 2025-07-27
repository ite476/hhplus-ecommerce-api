package kr.hhplus.be.server.service.product.usecase

import kr.hhplus.be.server.service.product.entity.ProductSaleSummary

interface FindAllPopularProductsUsecase {
    /**
     * 인기 상품 목록 조회
     */
    fun findAllPopularProducts() : List<ProductSaleSummary>
}
