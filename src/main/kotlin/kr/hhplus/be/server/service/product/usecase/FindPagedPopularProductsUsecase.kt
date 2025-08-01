package kr.hhplus.be.server.service.product.usecase

interface FindPagedPopularProductsUsecase {
    /**
     * 인기 상품 목록 조회
     */
    fun findPagedPopularProducts(pagingOptions: kr.hhplus.be.server.service.pagination.PagingOptions): kr.hhplus.be.server.service.pagination.PagedList<kr.hhplus.be.server.service.product.entity.ProductSaleSummary>
}
